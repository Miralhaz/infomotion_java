package org.example.classesGiulia;

import org.example.AwsConnection;
import org.example.Logs;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class TratamentoHistoricoServidor {
    // Atributos:
    private AwsConnection awsCon;
    private final JdbcTemplate con;
    private static final String PASTA_CLIENT = "tratamentos_giulia";

    // Construtor:
    public TratamentoHistoricoServidor(AwsConnection awsCon, JdbcTemplate con) {
        this.con = con;
        this.awsCon = awsCon;
    }

    // Métodos:
    private List<LogsGiuliaCriticidade> transformarLogs(List<Logs> logs){
        List<LogsGiuliaCriticidade> listaLogs = new ArrayList<>();

        for (Logs log : logs){
            LocalDateTime timestamp = log.getDataHora();
            LogsGiuliaCriticidade logCriticidade = new LogsGiuliaCriticidade(log.getFk_servidor(), log.getNomeMaquina(), timestamp, 0, log.getCpu(), log.getRam(), log.getDisco(), 0, 0, 0, 0);
            listaLogs.add(logCriticidade);
        }
        return listaLogs;
    }

    private static LocalDateTime validarDias(LocalDateTime t, Integer dias) {
        if (t == null) return null;
        if (dias == 1) {
            return t.withMinute(0).withSecond(0).withNano(0);
        }
        return t.toLocalDate().atStartOfDay();
    }

    public Map<LocalDateTime, Integer> calcularAlertas(List<LogsGiuliaCriticidade> logsServidor, Double limite, Integer duracao, String componente, Integer dias){

        Map<LocalDateTime, Integer> contadorAlertas = new TreeMap<>();
        Integer contadorDuracao = 0;
        LocalDateTime dataInicio = null;

        for (int i = 0; i < logsServidor.size(); i++) {
            LogsGiuliaCriticidade logAtual = logsServidor.get(i);
            Double uso = 0.0;

            if (componente.equalsIgnoreCase("CPU")){
                uso = logAtual.getUsoCpu();
            }

            if (componente.equalsIgnoreCase("RAM")){
                uso = logAtual.getUsoRam();
            }

            if(componente.equalsIgnoreCase("DISCO")){
                uso = logAtual.getUsoDisco();
            }

            Boolean acimaLimite = uso > limite;

            if (acimaLimite){
                contadorDuracao++;

                if (contadorDuracao == 1){
                    dataInicio = logAtual.getTimestamp();
                }
            }

            else{
                if (contadorDuracao >= duracao && dataInicio != null){
                    LocalDateTime dataFinal = logsServidor.get(i - 1).getTimestamp();
                    LocalDateTime dia = validarDias(dataFinal, dias);
                    contadorAlertas.merge(dia, 1, Integer::sum);
                }
                contadorDuracao = 0;
                dataInicio = null;
            }
        }

        if (contadorDuracao >= duracao && dataInicio != null){
            LocalDateTime dataFinal = logsServidor.get(logsServidor.size() - 1).getTimestamp();
            LocalDateTime dia = validarDias(dataFinal, dias);
            contadorAlertas.merge(dia, 1, Integer::sum);
        }
        return contadorAlertas;
    }

    public void classificarAlertas(List<Logs> logsConsolidados, Integer dias) {
        System.out.println("\n◎ Classificando alertas...");

        if (logsConsolidados == null || logsConsolidados.isEmpty()) {
            System.out.println("Nenhum log recebido para calcular alertas");
            return;
        }

        List<LogsGiuliaCriticidade> listaLogs = transformarLogs(logsConsolidados);
        LocalDateTime fim = LocalDateTime.now();
        LocalDateTime inicio = fim.minusDays(dias);
        Set<Integer> idsServidores = new LinkedHashSet<>();

        for (LogsGiuliaCriticidade log : listaLogs) {
            idsServidores.add(log.getFk_servidor());
        }

        List<Map<String, Object>> lista = new ArrayList<>();
        for (Integer id : idsServidores){
            List<LogsGiuliaCriticidade> logsServidor = new ArrayList<>();

            for (LogsGiuliaCriticidade log : listaLogs){
                if (log.getFk_servidor().equals(id)){
                    logsServidor.add(log);
                }
            }

            logsServidor.sort(Comparator.comparing(LogsGiuliaCriticidade::getTimestamp));

            List<LogsGiuliaCriticidade> logsPeriodo = new ArrayList<>();
            for (LogsGiuliaCriticidade log : logsServidor) {
                LocalDateTime periodo = log.getTimestamp();
                if (periodo != null && !periodo.isBefore(inicio) && !periodo.isAfter(fim)) {
                    logsPeriodo.add(log);
                };
            }
            if (logsPeriodo.isEmpty()) {
                continue;
            }

            String selectTipo = ("""
                select cast(pa.max as decimal(10,2)) as limite
                from parametro_alerta pa
                inner join componentes c on c.id = pa.fk_componente
                where c.tipo = (?) and pa.fk_servidor = (?) and pa.unidade_medida = '%';
            """);

            String selectDuracao = ("""
                select cast(pa.duracao_min as unsigned) as duracao
                from parametro_alerta pa
                inner join componentes c on c.id = pa.fk_componente
                where c.tipo = (?) and pa.fk_servidor = (?) and pa.unidade_medida = '%';
            """);

            List<Double> listaCpu = con.queryForList(selectTipo, Double.class, "CPU", id);
            List<Double> listaRam = con.queryForList(selectTipo, Double.class, "RAM", id);
            List<Double> listaDisco = con.queryForList(selectTipo, Double.class, "DISCO", id);

            if (listaCpu.isEmpty() || listaRam.isEmpty() || listaDisco.isEmpty()) {
                System.out.printf("Servidor %d sem parametro_alerta completo (CPU/RAM/DISCO em '%%'). Pulando.%n", id);
                continue;
            }

            Double limiteCpu = listaCpu.get(0);
            Double limiteRam = listaRam.get(0);
            Double limiteDisco = listaDisco.get(0);

            List<Integer> listaDuracaoCpu = con.queryForList(selectDuracao, Integer.class, "CPU", id);
            List<Integer> listaDuracaoRam = con.queryForList(selectDuracao, Integer.class, "RAM", id);
            List<Integer> listaDuracaoDisco = con.queryForList(selectDuracao, Integer.class, "DISCO", id);

            if (listaDuracaoCpu.isEmpty() || listaDuracaoRam.isEmpty() || listaDuracaoDisco.isEmpty()) {
                System.out.printf("Servidor %d sem duracao_min completo. Pulando.%n", id);
                continue;
            }

            Integer duracaoCpu = listaDuracaoCpu.get(0);
            Integer duracaoRam = listaDuracaoRam.get(0);
            Integer duracaoDisco = listaDuracaoDisco.get(0);

            Map<LocalDateTime, Integer> alertasCpu = calcularAlertas(logsPeriodo, limiteCpu, duracaoCpu, "CPU", dias);;
            Map<LocalDateTime, Integer> alertasRam = calcularAlertas(logsPeriodo, limiteRam, duracaoRam, "RAM", dias);
            Map<LocalDateTime, Integer> alertasDisco = calcularAlertas(logsPeriodo, limiteDisco, duracaoDisco, "DISCO", dias);

            Set<LocalDateTime> alertas = new TreeSet<>();
            alertas.addAll(alertasCpu.keySet());
            alertas.addAll(alertasRam.keySet());
            alertas.addAll(alertasDisco.keySet());

            String apelido = logsServidor.isEmpty() ? "" : logsServidor.get(0).getApelido();

            DateTimeFormatter fmt = (dias == 1) ? DateTimeFormatter.ofPattern("dd/MM/yyyy HH:00") : DateTimeFormatter.ofPattern("dd/MM/yyyy");

            for (LocalDateTime k : alertas) {
                int aCpu = alertasCpu.getOrDefault(k, 0);
                int aRam = alertasRam.getOrDefault(k, 0);
                int aDisco = alertasDisco.getOrDefault(k, 0);

                Map<String, Object> row = new LinkedHashMap<>();
                row.put("fk_servidor", id);
                row.put("apelido", apelido);
                row.put("timestamp", k.format(fmt));
                row.put("alertasCpu", aCpu);
                row.put("alertasRam", aRam);
                row.put("alertasDisco", aDisco);
                row.put("total", aCpu + aRam + aDisco);

                lista.add(row);
            }
        }
        String nome = "historicoAlertasLinhas_" + dias;
        gravaArquivoJson(lista, nome);
        awsCon.uploadBucketClient(PASTA_CLIENT, nome + ".json");
    }

    public static void gravaArquivoJson(List<Map<String, Object>> listaLogs, String nomeArq) {

        String nome = nomeArq.endsWith(".json") ? nomeArq : nomeArq + ".json";
        OutputStreamWriter saida = null;
        Boolean deuRuim = false;

        try {
            saida = new OutputStreamWriter(new FileOutputStream(nome), StandardCharsets.UTF_8);
            saida.append("[");
            Integer contador = 0;

            for (int i = 0; i < listaLogs.size(); i++) {
                Map<String, Object> obj = listaLogs.get(i);
                if (contador > 0) {
                    saida.append(",");
                }

                saida.write(String.format(Locale.US,""" 
                           {
                           "fk_servidor": %d,
                           "apelido": "%s",
                           "timestamp": "%s",
                           "alertasCpu": %d,
                           "alertasRam": %d,
                           "alertasDisco": %d,
                           "totalAlertas": %d
                           }""",
                        (Integer) obj.get("fk_servidor"),
                        ((String) obj.get("apelido")).replace("\"","\\\""),
                        ((String) obj.get("timestamp")).replace("\"","\\\""),
                        (Integer) obj.get("alertasCpu"),
                        (Integer) obj.get("alertasRam"),
                        (Integer) obj.get("alertasDisco"),
                        (Integer) obj.get("total")
                ));
                contador ++;
            }
            saida.append("]");
            System.out.println("Arquivo Json de histórico de um servidor gerado com sucesso!");

        }

        catch (IOException erro) {
            System.out.println("Erro ao gravar o arquivo Json de histórico de um servidor!");
            erro.printStackTrace();
            deuRuim = true;
        }

        finally {

            try {
                if (saida != null){
                    saida.close();
                }
            }

            catch (IOException erro) {
                System.out.println("Erro ao fechar o arquivo Json de donut");
                deuRuim = true;
            }

            if (deuRuim) {
                System.exit(1);
            }
        }
    }
}
