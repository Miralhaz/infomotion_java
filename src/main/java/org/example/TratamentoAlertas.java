package org.example;

import org.example.classesRede.TratamentoRede;
import org.example.classesWillian.TratamentoWillian;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static org.example.Main.gravaArquivoCsv;

public class TratamentoAlertas {

    public static void analisarAlertasNoArquivoRaw(
            JdbcTemplate con,
            Integer fk_servidor,
            List<Logs> logsDoArquivo,
            AwsConnection aws) {

        System.out.println("\n🔍 Analisando alertas para servidor " + fk_servidor);
        System.out.println("   Registros neste arquivo: " + logsDoArquivo.size());

        // Busca os parâmetros deste servidor
        String selectParametros = "SELECT * FROM parametro_alerta WHERE fk_servidor = ?";
        List<Parametro_alerta> parametros = con.query(selectParametros,
                new BeanPropertyRowMapper<>(Parametro_alerta.class),
                fk_servidor);

        if (parametros.isEmpty()) {
            System.out.println("   ℹ️ Sem parâmetros configurados para este servidor");
            return;
        }

        List<Logs> todosAlertasDetectados = new ArrayList<>();

        for (Parametro_alerta parametro : parametros) {
            Integer fk_componente = parametro.getFk_componente();
            Double limiteMax = parametro.getMax();
            Integer duracaoMin = parametro.getDuracao_min();
            Integer fk_parametroAlerta = parametro.getId();

            String tipo = con.queryForObject(
                    "SELECT tipo FROM componentes WHERE id = ?",
                    String.class,
                    fk_componente
            );

            List<String> unidadesMedida = con.queryForList(
                    "SELECT unidade_medida FROM parametro_alerta WHERE fk_componente = ?",
                    String.class,
                    fk_componente
            );

            for (String unidadeMedida : unidadesMedida) {
                System.out.println("   📊 " + tipo + " (" + unidadeMedida + ") | Limite: " + limiteMax + " | Duração: " + duracaoMin + " min");

                List<Logs> alertasDesteComponente = detectarAlerta(
                        logsDoArquivo,
                        tipo,
                        unidadeMedida,
                        limiteMax,
                        duracaoMin
                );

                if (!alertasDesteComponente.isEmpty()) {
                    Double valorMax = alertasDesteComponente.stream()
                            .mapToDouble(log -> obterValorDoLog(log, tipo, unidadeMedida))
                            .max()
                            .orElse(0.0);

                    Double valorMin = alertasDesteComponente.stream()
                            .mapToDouble(log -> obterValorDoLog(log, tipo, unidadeMedida))
                            .min()
                            .orElse(0.0);

                    String insertAlerta = "INSERT INTO alertas (fk_parametro, max, min) VALUES (?,?,?)";
                    con.update(insertAlerta, fk_parametroAlerta, valorMax, valorMin);

                    Logs ultimoLog = alertasDesteComponente.get(alertasDesteComponente.size() - 1);

                    enviarNotificacoes(fk_servidor, tipo, unidadeMedida, valorMax, valorMin, duracaoMin, ultimoLog);

                    todosAlertasDetectados.addAll(alertasDesteComponente);

                    System.out.println("🚨 ALERTA! Pico: " + valorMax + unidadeMedida);
                }
            }

            if (!todosAlertasDetectados.isEmpty()) {
                System.out.println("Total de alertas neste arquivo: " + todosAlertasDetectados.size());
                gravaArquivoCsv(todosAlertasDetectados, "alertas_" + fk_servidor);
                aws.uploadBucketTrusted("alertas_" + fk_servidor + ".csv");
            } else {
                System.out.println("Nenhum alerta detectado");
            }
        }
    }

    private static List<Logs> detectarAlerta(
            List<Logs> logs,
            String tipo,
            String unidadeMedida,
            Double limiteMax,
            Integer duracaoMin) {

        int contadorConsecutivos = 0;
        List<Logs> logsDoAlerta = new ArrayList<>();

        for (Logs log : logs) {
            Double valorAtual = obterValorDoLog(log, tipo, unidadeMedida);

            if (valorAtual > limiteMax) {
                contadorConsecutivos++;
                logsDoAlerta.add(log);

                // Se atingiu a duração mínima, alerta
                if (contadorConsecutivos >= duracaoMin) {
                    return logsDoAlerta; // Retorna os logs do alerta
                }
            } else {
                // Voltou ao normal, reseta
                contadorConsecutivos = 0;
                logsDoAlerta.clear();
            }
        }

        // Não atingiu a duração mínima
        return new ArrayList<>();
    }

    private static void enviarNotificacoes(
            Integer fk_servidor,
            String tipo,
            String unidadeMedida,
            Double valorMax,
            Double valorMin,
            Integer duracaoMin,
            Logs ultimoLog) {

        String tituloJira = String.format(
                "Alerta Crítico: %s em %.2f%s no Servidor %d",
                tipo, valorMax, unidadeMedida, fk_servidor
        );

        String descricaoJira = String.format(
                "Alerta de %s.\n" +
                        "Pico: %.2f%s\n" +
                        "Mínimo: %.2f%s\n" +
                        "Duração: %d minutos\n" +
                        "Data/Hora: %s",
                tipo, valorMax, unidadeMedida, valorMin, unidadeMedida, duracaoMin,
                ultimoLog.getDataHora().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
        );

        try {
            JiraService.createAlertTicket(tituloJira, descricaoJira);
        } catch (Exception e) {
            System.err.println("Falha ao criar ticket Jira: " + e.getMessage());
        }

        String mensagemSlack = String.format(
                "⚠️ Alerta de %s no servidor %d\n" +
                        "Pico: %.2f%s\n" +
                        "Mínimo: %.2f%s\n" +
                        "Duração: %d minutos",
                tipo, fk_servidor, valorMax, unidadeMedida, valorMin, unidadeMedida, duracaoMin
        );

        try {
            SlackNotifier.sendSlackMessage(mensagemSlack);
        } catch (Exception e) {
            System.err.println("Falha ao enviar Slack: " + e.getMessage());
        }
    }

    private static Double obterValorDoLog(Logs log, String tipo, String unidadeMedida) {
        switch (tipo.toUpperCase()) {
            case "CPU":
                return unidadeMedida.equals("%") ? log.getCpu() : log.getTmp_cpu();
            case "RAM":
                return log.getRam();
            case "DISCO":
                return unidadeMedida.equals("%") ? log.getDisco() : log.getTmp_disco();
            case "SWAP":
                return log.getMemoria_swap();
            default:
                return 0.0;
        }
    }
}
