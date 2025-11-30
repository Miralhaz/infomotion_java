package org.example.classesGiulia;

import org.example.AwsConnection;
import org.example.Logs;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

public class TratamentoHistorico {

    // Atributos:
    private AwsConnection awsCon;
    private final JdbcTemplate con;
    private static final String PASTA_CLIENT = "tratamentos_giulia";

    // Construtor:
    public TratamentoHistorico(AwsConnection awsCon, JdbcTemplate con) {
        this.con = con;
        this.awsCon = awsCon;
    }

    // Métodos:
    private List<LogsGiuliaCriticidade> transformarLogs(List<Logs> logs){
        List<LogsGiuliaCriticidade> listaLogs = new ArrayList<>();

        for (Logs log : logs){
            LocalDateTime timestamp = log.getDataHora();
            LogsGiuliaCriticidade logCriticidade = new LogsGiuliaCriticidade(log.getFk_servidor(), log.getNomeMaquina(), timestamp, 0, log.getCpu(), log.getRam(), log.getDisco(), log.getTmp_cpu(), log.getTmp_disco(), 0, 0, 0, 0);
            listaLogs.add(logCriticidade);
        }
        return listaLogs;
    }

    public Integer calcularAlertas(List<LogsGiuliaCriticidade> logsServidor, Double limite, Integer duracao, String componente, Character medida){

        Integer contadorAlertas = 0;
        LocalDateTime inicioPico = null;
        LocalDateTime ultimoAcima = null;
        Boolean emPico = false;

        for (int i = 0; i < logsServidor.size(); i++) {
            LogsGiuliaCriticidade logAtual = logsServidor.get(i);
            LocalDateTime t = logAtual.getTimestamp();
            if (t == null){
                continue;
            }
            Double uso = 0.0;

            if (componente.equalsIgnoreCase("CPU") && medida.equals('%')){
                uso = logAtual.getUsoCpu();
            }

            if (componente.equalsIgnoreCase("RAM")){
                uso = logAtual.getUsoRam();
            }

            if(componente.equalsIgnoreCase("DISCO")){
                uso = logAtual.getUsoDisco();
            }

            if (componente.equalsIgnoreCase("CPU") && medida.equals('C')){
                uso = logAtual.getTempCpu();
            }

            if (componente.equalsIgnoreCase("DISCO") && medida.equals('C')){
                uso = logAtual.getTempDisco();
            }

            Boolean acimaLimite = uso > limite;

            if (acimaLimite){
                if (!emPico) {
                    emPico = true;
                    inicioPico = t;
                }
                ultimoAcima = t;
            }

            else{
                if (emPico && inicioPico != null && ultimoAcima != null) {
                    long seg = Duration.between(inicioPico, ultimoAcima).toSeconds();
                    Integer minutos = (int) Math.max(1, (seg + 59) / 60);

                    if (medida.equals('%')) {
                        if (minutos >= duracao) {
                            contadorAlertas++;
                        }
                    } else{
                        if (minutos >= duracao) {
                            contadorAlertas++;
                        }
                    }
                }
                emPico = false;
                inicioPico = null;
                ultimoAcima = null;
            }
        }

        if (emPico && inicioPico != null && ultimoAcima != null) {
            long seg = Duration.between(inicioPico, ultimoAcima).toSeconds();
            Integer minutos = (int) Math.max(1, (seg + 59) / 60);

            if (medida.equals('%')) {
                if (minutos >= duracao) {
                    contadorAlertas++;
                }
            } else{
                if (minutos >= duracao) {
                    contadorAlertas++;
                }
            }
        }
        return contadorAlertas;
    }

    public void classificarAlertas(List<Logs> logsConsolidados, Integer dias) {
        System.out.println("\n⚠\uFE0F Classificando alertas...");

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
                where c.tipo = (?) and pa.fk_servidor = (?) and pa.unidade_medida = (?);
            """);

            String selectDuracao = ("""
                select cast(pa.duracao_min as unsigned) as duracao
                from parametro_alerta pa
                inner join componentes c on c.id = pa.fk_componente
                where c.tipo = (?) and pa.fk_servidor = (?) and pa.unidade_medida = (?);
            """);

            List<Double> listaCpu = con.queryForList(selectTipo, Double.class, "CPU", id, "%");
            List<Double> listaRam = con.queryForList(selectTipo, Double.class, "RAM", id, "%");
            List<Double> listaDisco = con.queryForList(selectTipo, Double.class, "DISCO", id, "%");
            List<Double> listaTempCpu = con.queryForList(selectTipo, Double.class, "CPU", id, "C");
            List<Double> listaTempDisco = con.queryForList(selectTipo, Double.class, "DISCO", id, "C");

            if (listaCpu.isEmpty() || listaRam.isEmpty() || listaDisco.isEmpty() || listaTempCpu.isEmpty() || listaTempDisco.isEmpty()) {
                System.out.printf("Servidor %d sem parametro_alerta completo (CPU/RAM/DISCO/TempCPU/TempDISCO). Pulando.%n", id);
                continue;
            }

            Double limiteCpu = listaCpu.get(0);
            Double limiteRam = listaRam.get(0);
            Double limiteDisco = listaDisco.get(0);
            Double limiteTempCpu = listaTempCpu.get(0);
            Double limiteTempDisco = listaTempDisco.get(0);

            List<Integer> listaDuracaoCpu = con.queryForList(selectDuracao, Integer.class, "CPU", id, "%");
            List<Integer> listaDuracaoRam = con.queryForList(selectDuracao, Integer.class, "RAM", id, "%");
            List<Integer> listaDuracaoDisco = con.queryForList(selectDuracao, Integer.class, "DISCO", id, "%");
            List<Integer> listaDuracaoTempCpu = con.queryForList(selectDuracao, Integer.class, "CPU", id, "C");
            List<Integer> listaDuracaoTempDisco = con.queryForList(selectDuracao, Integer.class, "DISCO", id, "C");


            if (listaDuracaoCpu.isEmpty() || listaDuracaoRam.isEmpty() || listaDuracaoDisco.isEmpty() || listaDuracaoTempCpu.isEmpty() || listaDuracaoTempDisco.isEmpty()) {
                System.out.printf("Servidor %d sem duracao_min completo. Pulando.%n", id);
                continue;
            }

            Integer duracaoCpu = listaDuracaoCpu.get(0);
            Integer duracaoRam = listaDuracaoRam.get(0);
            Integer duracaoDisco = listaDuracaoDisco.get(0);
            Integer duracaoTempCpu = listaDuracaoTempCpu.get(0);
            Integer duracaoTempDisco = listaDuracaoTempDisco.get(0);

            Integer alertasCpu = 0;
            Integer alertasRam = 0;
            Integer alertasDisco = 0;
            Integer alertasTempCpu = 0;
            Integer alertasTempDisco = 0;

            if (limiteCpu != null && duracaoCpu != null) {
                alertasCpu = calcularAlertas(logsPeriodo, limiteCpu, duracaoCpu, "CPU", '%');
            }
            if (limiteRam != null && duracaoRam != null) {
                alertasRam = calcularAlertas(logsPeriodo, limiteRam, duracaoRam, "RAM", '%');
            }
            if (limiteDisco != null && duracaoDisco != null) {
                alertasDisco = calcularAlertas(logsPeriodo, limiteDisco, duracaoDisco, "DISCO", '%');
            }
            if (limiteTempCpu != null && duracaoTempCpu != null) {
                alertasTempCpu = calcularAlertas(logsPeriodo, limiteTempCpu, duracaoTempCpu, "CPU", 'C');
            }
            if (limiteTempDisco != null && duracaoTempDisco != null) {
                alertasTempDisco = calcularAlertas(logsPeriodo, limiteTempDisco, duracaoTempDisco, "DISCO", 'C');
            }

            alertasCpu += alertasTempCpu;
            alertasDisco += alertasTempDisco;
            Integer totalAlertas = alertasCpu + alertasRam + alertasDisco;

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("fk_servidor", id);
            row.put("apelido", logsServidor.isEmpty() ? "" : logsServidor.get(0).getApelido());
            row.put("dias", dias);
            row.put("alertasCpu", alertasCpu);
            row.put("alertasRam", alertasRam);
            row.put("alertasDisco", alertasDisco);
            row.put("totalAlertas", totalAlertas);

            lista.add(row);
        }
        String nome = "historicoAlertas_" + dias;
        gravaArquivoJson(lista, nome);
        awsCon.uploadBucketClient(PASTA_CLIENT, nome + ".json");
    }

    public static void gravaArquivoJson(List<Map<String, Object>> lista, String nomeArq) {

        String nome = nomeArq.endsWith(".json") ? nomeArq : nomeArq + ".json";
        OutputStreamWriter saida = null;
        Boolean deuRuim = false;

        try {
            saida = new OutputStreamWriter(new FileOutputStream(nome), StandardCharsets.UTF_8);
            saida.append("[");

            for (int i = 0; i < lista.size(); i++) {
                Map<String, Object> map = lista.get(i);

                if (i > 0) {
                    saida.append(",");
                }

                saida.write(String.format(Locale.US,""" 
                           {
                           "fk_servidor": %d,
                           "apelido": "%s",
                           "dias": %d,
                           "alertasCpu": %d,
                           "alertasRam": %d,
                           "alertasDisco": %d,
                           "totalAlertas": %d
                           }""",
                        (Integer) map.get("fk_servidor"),
                        (String) map.get("apelido"),
                        (Integer) map.get("dias"),
                        (Integer) map.get("alertasCpu"),
                        (Integer) map.get("alertasRam"),
                        (Integer) map.get("alertasDisco"),
                        (Integer) map.get("totalAlertas")
                ));
            }
            saida.append("]");
            System.out.println("Arquivo Json de hisórico de alertas gerado com sucesso!");

        }

        catch (IOException erro) {
            System.out.println("Erro ao gravar o arquivo Json de hisórico de alertas!");
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
                System.out.println("Erro ao fechar o arquivo Json de hisórico de alertas");
                deuRuim = true;
            }

            if (deuRuim) {
                System.exit(1);
            }
        }
    }
}
