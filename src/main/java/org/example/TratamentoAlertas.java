package org.example;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static org.example.Main.gravaArquivoCsv;

public class TratamentoAlertas {

    public static void gerarAlerta(JdbcTemplate con, Integer fk_parametroAlerta, Integer fk_servidor,
                                   List<Logs> miniLista, String componente, String unidade,
                                   double maxValor, double minValor, int duracao_min){

        Logs ultimoLog = miniLista.get(miniLista.size() - 1);

        LocalDateTime dtCorte = ultimoLog.getDataHora().minusMinutes(5);

        String checarAlertasDuplicados = "SELECT COUNT(id) FROM alertas WHERE fk_parametro = ? AND dt_alerta >= ?";
        Integer contador = con.queryForObject(checarAlertasDuplicados, Integer.class, fk_parametroAlerta, dtCorte);

        if (contador != null && contador > 0) {
            System.out.println("Alerta duplicado com o parâmetro " + fk_parametroAlerta + "; Pulando alerta.");
            return;
        }

        con.update("INSERT INTO alertas (fk_parametro, max, min, dt_alerta) VALUES ((?),(?),(?),(?));",
                fk_parametroAlerta, maxValor, minValor, ultimoLog.getDataHora());

        String tituloJira = String.format("Alerta Crítico: %s %s em %.2f no Servidor %d",
                componente, unidade, maxValor, fk_servidor);
        String descricaoCorpo = String.format(
                "Alerta de %s %s.\nPico: %.2f\nDuração: %d minutos\nUsuário: %s\nData e hora: %s",
                componente, unidade, maxValor, duracao_min,
                ultimoLog.getNomeMaquina(),
                ultimoLog.getDataHora().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
        );

        System.out.println("Alerta de " + componente + " " + unidade + "!");

        try {
            JiraService.createAlertTicket(tituloJira, descricaoCorpo);
        } catch (Exception e) {
            System.err.println("Falha ao criar ticket Jira para " + componente + " " + unidade + ": " + e.getMessage());
        }

        String mensagemSlack = "\n ⚠️ Alerta de " + componente + " " + unidade + " no servidor: " + fk_servidor +
                "\nPico do alerta: " + maxValor +
                "\nMínimo do alerta: " + minValor +
                "\nDuração do alerta: " + duracao_min + " minutos";
        try {
            SlackNotifier.sendSlackMessage(mensagemSlack);
        } catch (Exception e) {
            System.err.println("Falha ao enviar mensagem Slack: " + e.getMessage());
        }
    }

    public static void TratamentoAlertas(JdbcTemplate con, Integer fk_servidor, List<Logs> listaLogsTotal, AwsConnection aws) throws Exception {

        final String insert = "INSERT INTO alertas (fk_parametro, max, min) VALUES ((?),(?),(?));";
        long valorMax = Long.MAX_VALUE;
        Long zero = 0L;

        List<Logs> listaAlertas = new ArrayList<>();

        String selectParametroPorServidor = "SELECT * FROM parametro_alerta WHERE fk_servidor = (?);";
        List<Parametro_alerta> metrica = con.query(selectParametroPorServidor,
                new BeanPropertyRowMapper<>(Parametro_alerta.class),
                fk_servidor);

        boolean existeAlertaDisco = false;
        boolean jaFoiAlertaDisco = false;

        for (int i = 0; i < metrica.size(); i++) {

            Integer fk_componente = metrica.get(i).getFk_componente();
            Double max = metrica.get(i).getMax();
            Integer duracao_min = metrica.get(i).getDuracao_min();
            Integer fk_parametroAlerta = metrica.get(i).getId();

            String selectTipo = "SELECT tipo FROM parametro_alerta pa INNER JOIN componentes c ON c.id = pa.fk_componente WHERE fk_componente = (?) LIMIT 1;";
            String tipo = con.queryForObject(selectTipo, String.class, fk_componente);

            String selectUnidadeMedida = "SELECT unidade_medida FROM parametro_alerta pa INNER JOIN componentes c ON c.id = pa.fk_componente WHERE fk_componente = (?);";
            List<String> unidadesMedida = con.queryForList(selectUnidadeMedida, String.class, fk_componente);

            int contadorCPUPorcentagem = 0, contadorCPUTemperatura = 0, contadorRamPorcentagem = 0,
                    contadorDiscoTemperatura = 0, contadorSwap = 0, contadorDownload = 0,
                    contadorUpload = 0, contadorPacketSent = 0, contadorPacketReceived = 0;

           Double maxCPUPorcentagem = 0.0, minCPUPorcentagem = 100.0;
           Double maxCPUTemperatura = 0.0, minCPUTemperatura = 100.0;
           Double maxRamPorcentagem = 0.0, minRamPorcentagem = 100.0;
           Double maxDiscoPorcentagem = 0.0, minDiscoPorcentagem = 100.0;
           Double maxDiscoTemperatura = 0.0, minDiscoTemperatura = 100.0;
           Double maxSwap = 0.0, minSwap = 99999999.9;

           Long maxDown = zero, minDown = valorMax;
           Long maxUp = zero, minUp = valorMax;
           Long maxpacketSent = zero, minpacketSent = valorMax;
           Long maxpacketReceived = zero, minpacketReceived = valorMax;

            List<Logs> miniLista = new ArrayList<>();
            for (int k = 0; k < duracao_min && k < listaLogsTotal.size(); k++) {
                miniLista.add(listaLogsTotal.get(k));
            }

            for (Logs logAtual : miniLista) {
                for (String unidadeMedida : unidadesMedida) {
                    if (tipo.equalsIgnoreCase("CPU")) {
                        if (unidadeMedida.equalsIgnoreCase("%")) {
                            if (logAtual.getCpu() > max) {
                                contadorCPUPorcentagem++;
                                maxCPUPorcentagem = Math.max(maxCPUPorcentagem, logAtual.getCpu());
                                minCPUPorcentagem = Math.min(minCPUPorcentagem, logAtual.getCpu());
                            }
                        } else {
                            if (logAtual.getTmp_cpu() > max) {
                                contadorCPUTemperatura++;
                                maxCPUTemperatura = Math.max(maxCPUTemperatura, logAtual.getTmp_cpu());
                                minCPUTemperatura = Math.min(minCPUTemperatura, logAtual.getTmp_cpu());
                            }
                        }
                    } else if (tipo.equalsIgnoreCase("RAM")) {
                        if (logAtual.getRam() > max) {
                            contadorRamPorcentagem++;
                            maxRamPorcentagem = Math.max(maxRamPorcentagem, logAtual.getRam());
                            minRamPorcentagem = Math.min(minRamPorcentagem, logAtual.getRam());
                        }
                    } else if (tipo.equalsIgnoreCase("DISCO")) {
                        if (unidadeMedida.equalsIgnoreCase("%")) {
                            if (!existeAlertaDisco && logAtual.getDisco() > max) {
                                existeAlertaDisco = true;
                            }
                            maxDiscoPorcentagem = logAtual.getDisco();
                            minDiscoPorcentagem = logAtual.getDisco();
                        } else {
                            if (logAtual.getTmp_disco() > max) {
                                contadorDiscoTemperatura++;
                                maxDiscoTemperatura = Math.max(maxDiscoTemperatura, logAtual.getTmp_disco());
                                minDiscoTemperatura = Math.min(minDiscoTemperatura, logAtual.getTmp_disco());
                            }
                        }
                    } else if (tipo.equalsIgnoreCase("SWAP")) {
                        if (logAtual.getMemoria_swap() > max) {
                            contadorSwap++;
                            maxSwap = Math.max(maxSwap, logAtual.getMemoria_swap());
                            minSwap = Math.min(minSwap, logAtual.getMemoria_swap());
                        }
                    } else if (tipo.equalsIgnoreCase("REDE")) {
                        if (unidadeMedida.equalsIgnoreCase("DOWNLOAD")) {
                            if (logAtual.getDownload_bytes() < max) {
                                contadorDownload++;
                                maxDown = Math.max(maxDown, logAtual.getDownload_bytes());
                                minDown = Math.min(minDown, logAtual.getDownload_bytes());
                            }
                        } else if (unidadeMedida.equalsIgnoreCase("UPLOAD")) {
                            if (logAtual.getUpload_bytes() < max) {
                                contadorUpload++;
                                maxUp = Math.max(maxUp, logAtual.getUpload_bytes());
                                minUp = Math.min(minUp, logAtual.getUpload_bytes());
                            }
                        } else if (unidadeMedida.equalsIgnoreCase("PCKT_RCVD")) {
                            if (logAtual.getPacotes_recebidos() < max) {
                                contadorPacketReceived++;
                                maxpacketReceived = Math.max(maxpacketReceived, logAtual.getPacotes_recebidos());
                                minpacketReceived = Math.min(minpacketReceived, logAtual.getPacotes_recebidos());
                            }
                        } else if (unidadeMedida.equalsIgnoreCase("PCKT_SNT")) {
                            if (logAtual.getPacotes_enviados() < max) {
                                contadorPacketSent++;
                                maxpacketSent = Math.max(maxpacketSent, logAtual.getPacotes_enviados());
                                minpacketSent = Math.min(minpacketSent, logAtual.getPacotes_enviados());
                            }
                        }
                    }
                }
            }

            if (contadorCPUPorcentagem == duracao_min) {
                gerarAlerta(con, fk_parametroAlerta, fk_servidor, miniLista,
                        "CPU", "%", maxCPUPorcentagem, minCPUPorcentagem, duracao_min);
            } else if (contadorCPUTemperatura == duracao_min) {
                gerarAlerta(con, fk_parametroAlerta, fk_servidor, miniLista,
                        "CPU", "°C", maxCPUTemperatura, minCPUTemperatura, duracao_min);
            } else if (contadorDiscoTemperatura == duracao_min) {
                gerarAlerta(con, fk_parametroAlerta, fk_servidor, miniLista,
                        "Disco", "°C", maxDiscoTemperatura, minDiscoTemperatura, duracao_min);
            } else if (existeAlertaDisco && !jaFoiAlertaDisco) {
                jaFoiAlertaDisco = true;
                gerarAlerta(con, fk_parametroAlerta, fk_servidor, miniLista,
                        "Disco", "%", maxDiscoPorcentagem, minDiscoPorcentagem, duracao_min);
            } else if (contadorRamPorcentagem == duracao_min) {
                gerarAlerta(con, fk_parametroAlerta, fk_servidor, miniLista,
                        "RAM", "%", maxRamPorcentagem, minRamPorcentagem, duracao_min);
            } else if (contadorSwap == duracao_min) {
                gerarAlerta(con, fk_parametroAlerta, fk_servidor, miniLista,
                        "SWAP", "%", maxSwap, minSwap, duracao_min);
            } else if (contadorDownload == duracao_min) {
                gerarAlerta(con, fk_parametroAlerta, fk_servidor, miniLista,
                        "Rede", "Download", maxDown, minDown, duracao_min);
            } else if (contadorUpload == duracao_min) {
                gerarAlerta(con, fk_parametroAlerta, fk_servidor, miniLista,
                        "Rede", "Upload", maxUp, minUp, duracao_min);
            } else if (contadorPacketSent == duracao_min) {
                gerarAlerta(con, fk_parametroAlerta, fk_servidor, miniLista,
                        "Rede", "Pacotes enviados", maxpacketSent, minpacketSent, duracao_min);
            } else if (contadorPacketReceived == duracao_min) {
                gerarAlerta(con, fk_parametroAlerta, fk_servidor, miniLista,
                        "Rede", "Pacotes recebidos", maxpacketReceived, minpacketReceived, duracao_min);
            }
        }
    }
}