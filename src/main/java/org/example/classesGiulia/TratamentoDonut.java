package org.example.classesGiulia;

import org.example.*;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

public class TratamentoDonut {

    private AwsConnection awsCon;
    private final JdbcTemplate con;
    private static final String PASTA_CLIENT = "tratamentos_giulia";

    public TratamentoDonut(JdbcTemplate con, AwsConnection awsCon) {
        this.con = con;
        this.awsCon = awsCon;
    }

    private List<LogsGiuliaCriticidade> transformarLogs(List<Logs> logs){
        List<LogsGiuliaCriticidade> listaLogs = new ArrayList<>();

        for (Logs log : logs){
            LocalDateTime timestamp = log.getDataHora();
            LogsGiuliaCriticidade logCriticidade = new LogsGiuliaCriticidade(log.getFk_servidor(), log.getNomeMaquina(), 0, timestamp, log.getCpu(), log.getRam(), log.getDisco(), "");
            listaLogs.add(logCriticidade);
        }
        return listaLogs;
    }


    public Integer calcularMinutosAcimaComponente(List<LogsGiuliaCriticidade> logsServidor, Double limite, String componente){
        Integer contadorMinutos = 0;

        for (int i = 0; i < logsServidor.size() - 1; i++) {
            LogsGiuliaCriticidade logAtual = logsServidor.get(i);
            LogsGiuliaCriticidade logProximo = logsServidor.get(i + 1);

            long minutosEntreLogs = Duration.between(logAtual.getTimestamp(), logProximo.getTimestamp()).toMinutes();

            if (minutosEntreLogs <= 0){
                minutosEntreLogs = 1;
            }

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

            if (uso > limite){
                contadorMinutos += (int) minutosEntreLogs;
            }
        }
        return contadorMinutos;
    }


    public void classificarCriticidade(List<Logs> logsConsolidados) {

        List<LogsGiuliaCriticidade> listaLogs = transformarLogs(logsConsolidados);

        Set<Integer> idsServidores = new LinkedHashSet<>();
        for (LogsGiuliaCriticidade log : listaLogs) {
            idsServidores.add(log.getFk_servidor());
        }

        Integer contadorOk = 0;
        Integer contadorAtencao = 0;
        Integer contadorCritico = 0;

        String selectTipo = ("""
                select pa.max
                from parametro_alerta pa
                inner join componentes c on c.id = pa.fk_componente
                where c.tipo = (?) and pa.fk_servidor = (?);
            """);

        for (Integer id : idsServidores){
            List<LogsGiuliaCriticidade> logsServidor = new ArrayList<>();

            for (LogsGiuliaCriticidade log : listaLogs){
                if (log.getFk_servidor().equals(id)){
                    logsServidor.add(log);
                }
            }

            Double limiteCpu = con.queryForObject(selectTipo, Double.class, "CPU", id);
            Double limiteRam = con.queryForObject(selectTipo, Double.class, "RAM", id);
            Double limiteDisco = con.queryForObject(selectTipo, Double.class, "DISCO", id);

            Integer minCpu = calcularMinutosAcimaComponente(logsServidor, limiteCpu, "CPU");
            Integer minRam = calcularMinutosAcimaComponente(logsServidor, limiteRam, "RAM");
            Integer minDisco = calcularMinutosAcimaComponente(logsServidor, limiteDisco, "DISCO");

            String classificacao;
            Integer maiorQtdMinutos = Math.max(minCpu, Math.max(minRam, minDisco));

            if (maiorQtdMinutos >= 30){
                classificacao = "CRITICO";
                contadorCritico++;
            }

            else if(maiorQtdMinutos >= 5){
                classificacao = "ATENCAO";
                contadorAtencao++;
            }

            else {
                classificacao = "OK";
                contadorOk++;
            }

            for (LogsGiuliaCriticidade log : logsServidor){
                log.setClassificacao(classificacao);
            }
        }
        gravaArquivoJson(listaLogs, "nivelCriticidadeGiulia");
        awsCon.uploadBucketClient(PASTA_CLIENT, "nivelCriticidadeGiulia.json");

        System.out.printf("Donut - OK: %d | ATENCAO: %d | CRITICO: %d%n", contadorOk, contadorAtencao, contadorCritico);
    }

    public static void gravaArquivoJson(List<LogsGiuliaCriticidade> lista, String nomeArq) {

        String nome = nomeArq.endsWith(".json") ? nomeArq : nomeArq + ".json";
        OutputStreamWriter saida = null;
        Boolean deuRuim = false;

        try {
            saida = new OutputStreamWriter(new FileOutputStream(nome), StandardCharsets.UTF_8);
            saida.append("[");
            Integer contador = 0;

            for (LogsGiuliaCriticidade log : lista) {

                if (contador > 0) {
                    saida.append(",");
                }

                saida.write(String.format(Locale.US,""" 
                           {
                           "fk_servidor": %d,
                           "apelido": "%s" ,
                           "timestamp": "%s",
                           "minutos": %d,
                           "cpu": %.2f,
                           "ram": %.2f,
                           "disco": %.2f,
                           "classificacao": "%s"
                           }""",
                        log.getFk_servidor(), log.getApelido(), log.getTimestamp(), log.getMinutos() == null ? 0 : log.getMinutos(), log.getUsoCpu(), log.getUsoRam(), log.getUsoDisco(), log.getClassificacao() == null ? "" : log.getClassificacao()));

                contador ++;
            }
            saida.append("]");
            System.out.println("Arquivo Json de Criticidade (donut) gerado com sucesso!");

        }

        catch (IOException erro) {
            System.out.println("Erro ao gravar o arquivo Json de Criticidade (donut)!");
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
