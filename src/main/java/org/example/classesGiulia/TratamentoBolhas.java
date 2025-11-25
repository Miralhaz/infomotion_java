package org.example.classesGiulia;

import org.example.AwsConnection;
import org.example.Connection;
import org.example.Logs;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class TratamentoBolhas {

    private final AwsConnection awsCon;
    private final JdbcTemplate con;
    private static final String PASTA_CLIENT = "tratamentos_giulia";

    public TratamentoBolhas(AwsConnection awsCon, JdbcTemplate con) {
        this.awsCon = awsCon;
        this.con = con;
    }

    private List<LogsGiuliaCriticidade> transformarLogs(List<Logs> logs){
        List<LogsGiuliaCriticidade> listaLogs = new ArrayList<>();
        for (Logs log : logs) {
            LocalDateTime timestamp = log.getDataHora();
            LogsGiuliaCriticidade logCriticidade = new LogsGiuliaCriticidade(
                    log.getFk_servidor(),
                    log.getNomeMaquina(),
                    0,
                    timestamp,
                    log.getCpu(),
                    log.getRam(),
                    log.getDisco(),
                    ""
            );
            listaLogs.add(logCriticidade);
        }
        return listaLogs;
    }

    public void gerarBolhasCpu(List<Logs> logsConsolidados) {

        List<LogsGiuliaCriticidade> listaLogs = transformarLogs(logsConsolidados);
        List<Integer> idsServidores = new ArrayList<>();

        for (LogsGiuliaCriticidade log : listaLogs) {
            idsServidores.add(log.getFk_servidor());
        }

        List<LogsGiuliaCriticidade> listaBolhasCpu = new ArrayList<>();
        String selectTipo = ("""
                select pa.max
                from parametro_alerta pa
                inner join componentes c on c.id = pa.fk_componente
                where c.tipo = (?) and pa.fk_servidor = (?) and pa.unidade_medida = '%';
            """);

        for (Integer id : idsServidores) {
            List<LogsGiuliaCriticidade> logsServidor = new ArrayList<>();

            for (LogsGiuliaCriticidade log : listaLogs) {
                if (log.getFk_servidor().equals(id)) {
                    logsServidor.add(log);
                }
            }

            logsServidor.sort(Comparator.comparing(LogsGiuliaCriticidade::getTimestamp));

            List<Double> listaCpu = con.queryForList(selectTipo, Double.class, "CPU", id);
            Double limiteCpu = listaCpu.get(0);

            Integer minutosAcima = 0;
            Double maxPercentual = 0.0;


            for (int i = 0; i < logsServidor.size() - 1; i++) {
                LogsGiuliaCriticidade primeiroLog = logsServidor.get(0);
                LogsGiuliaCriticidade ultimoLog = logsServidor.get(logsServidor.size() - 1);
                LogsGiuliaCriticidade logAtual = logsServidor.get(i);

                Duration minutos = Duration.between(primeiroLog.getTimestamp(), ultimoLog.getTimestamp());
                long minutosEntreLogs = minutos.toMinutes();

                Double uso = logAtual.getUsoCpu();

                if (uso > limiteCpu) {
                    minutosAcima += (int) minutosEntreLogs;
                }

                if (uso > maxPercentual) {
                    maxPercentual = uso;
                }
            }

            if (minutosAcima >= 0){
                String apelidoServidor = logsServidor.get(0).getApelido();
                String classificacao = "";
                LogsGiuliaCriticidade bolha = new LogsGiuliaCriticidade(id, apelidoServidor, maxPercentual, minutosAcima, classificacao);

                if (minutosAcima >= 30) {
                    classificacao = "CRITICO";
                }

                else if (minutosAcima >= 5) {
                    classificacao = "ATENCAO";
                }

                else {
                    classificacao = "OK";
                }

                bolha.setClassificacao(classificacao);
                listaBolhasCpu.add(bolha);
            }
        }
        gravaArquivoJson(listaBolhasCpu, "criticidadeCpu");
        awsCon.uploadBucketClient(PASTA_CLIENT, "criticidadeCpu.json");
    }


    public void gerarBolhasRam(List<Logs> logsConsolidados) {

        List<LogsGiuliaCriticidade> listaLogs = transformarLogs(logsConsolidados);
        List<Integer> idsServidores = new ArrayList<>();

        for (LogsGiuliaCriticidade log : listaLogs) {
            idsServidores.add(log.getFk_servidor());
        }

        List<LogsGiuliaCriticidade> listaBolhasRam = new ArrayList<>();

        String selectTipo = ("""
                select pa.max
                from parametro_alerta pa
                inner join componentes c on c.id = pa.fk_componente
                where c.tipo = (?) and pa.fk_servidor = (?) and pa.unidade_medida = '%';
            """);

        for (Integer id : idsServidores) {
            List<LogsGiuliaCriticidade> logsServidor = new ArrayList<>();

            for (LogsGiuliaCriticidade log : listaLogs) {
                if (log.getFk_servidor().equals(id)) {
                    logsServidor.add(log);
                }
            }

            logsServidor.sort(Comparator.comparing(LogsGiuliaCriticidade::getTimestamp));

            List<Double> listaRam = con.queryForList(selectTipo, Double.class, "CPU", id);
            Double limiteRam = listaRam.get(0);

            Integer minutosAcima = 0;
            Double maxPercentual = 0.0;

            for (int i = 0; i < logsServidor.size() - 1; i++) {
                LogsGiuliaCriticidade logAtual = logsServidor.get(i);
                LogsGiuliaCriticidade logProximo = logsServidor.get(i + 1);

                Duration minutos = Duration.between(logAtual.getTimestamp(), logProximo.getTimestamp());
                long minutosEntreLogs = minutos.toMinutes();

                if (minutosEntreLogs <= 0) {
                    minutosEntreLogs = 1;
                }

                Double uso = logAtual.getUsoRam();

                if (uso > limiteRam) {
                    minutosAcima += (int) minutosEntreLogs;
                }

                if (uso > maxPercentual) {
                    maxPercentual = uso;
                }
            }

            if (minutosAcima >= 0){
                String apelidoServidor = logsServidor.get(0).getApelido();
                String classificacao = "";
                LogsGiuliaCriticidade bolha = new LogsGiuliaCriticidade(id, apelidoServidor, maxPercentual, minutosAcima, classificacao);

                if (minutosAcima >= 30) {
                    classificacao = "CRITICO";
                }

                else if (minutosAcima >= 5) {
                    classificacao = "ATENCAO";
                }

                else {
                    classificacao = "OK";
                }

                bolha.setClassificacao(classificacao);
                listaBolhasRam.add(bolha);
            }
        }

        gravaArquivoJson(listaBolhasRam, "criticidadeRam");
        awsCon.uploadBucketClient(PASTA_CLIENT, "criticidadeRam.json");
    }


    public void gerarBolhasDisco(List<Logs> logsConsolidados) {

        List<LogsGiuliaCriticidade> listaLogs = transformarLogs(logsConsolidados);
        List<Integer> idsServidores = new ArrayList<>();

        for (LogsGiuliaCriticidade log : listaLogs) {
            idsServidores.add(log.getFk_servidor());
        }

        List<LogsGiuliaCriticidade> listaBolhasDisco = new ArrayList<>();

        String selectTipo = ("""
                select pa.max
                from parametro_alerta pa
                inner join componentes c on c.id = pa.fk_componente
                where c.tipo = (?) and pa.fk_servidor = (?) and pa.unidade_medida = '%';
            """);

        for (Integer id : idsServidores) {
            List<LogsGiuliaCriticidade> logsServidor = new ArrayList<>();

            for (LogsGiuliaCriticidade log : listaLogs) {
                if (log.getFk_servidor().equals(id)) {
                    logsServidor.add(log);
                }
            }

            logsServidor.sort(Comparator.comparing(LogsGiuliaCriticidade::getTimestamp));

            List<Double> listaDisco = con.queryForList(selectTipo, Double.class, "DISCO", id);
            Double limiteDisco = listaDisco.get(0);

            Integer minutosAcima = 0;
            Double maxPercentual = 0.0;

            for (int i = 0; i < logsServidor.size() - 1; i++) {
                LogsGiuliaCriticidade logAtual = logsServidor.get(i);
                LogsGiuliaCriticidade logProximo = logsServidor.get(i + 1);

                Duration minutos = Duration.between(logAtual.getTimestamp(), logProximo.getTimestamp());
                long minutosEntreLogs = minutos.toMinutes();

                Double uso = logAtual.getUsoDisco();

                if (uso > limiteDisco) {
                    minutosAcima += (int) minutosEntreLogs;
                }

                if (uso > maxPercentual) {
                    maxPercentual = uso;
                }
            }

            if (minutosAcima > 0){
                String apelidoServidor = logsServidor.get(0).getApelido();
                String classificacao = "";
                LogsGiuliaCriticidade bolha = new LogsGiuliaCriticidade(id, apelidoServidor, maxPercentual, minutosAcima, classificacao);

                if (minutosAcima >= 30) {
                    classificacao = "CRITICO";
                }

                else if (minutosAcima >= 5) {
                    classificacao = "ATENCAO";
                }

                else {
                    classificacao = "OK";
                }

                bolha.setClassificacao(classificacao);
                listaBolhasDisco.add(bolha);
            }

        }

        gravaArquivoJson(listaBolhasDisco, "criticidadeDisco");
        awsCon.uploadBucketClient(PASTA_CLIENT, "bolhasCpuGiulia.json");
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
                           "percentual": %.2f,
                           "minutos": %d,
                           "classificacao": "%s"
                           }""",
                        log.getFk_servidor(), log.getApelido(), log.getPercentual(), log.getMinutos(), log.getClassificacao() == null ? "" : log.getClassificacao()));
                contador ++;
            }
            saida.append("]");
            System.out.println("Arquivo Json de bolhas gerado com sucesso!");
        }

        catch (IOException erro) {
            System.out.println("Erro ao gravar o arquivo de bolhas");
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
                System.out.println("Erro ao fechar o arquivo");
                deuRuim = true;
            }

            if (deuRuim) {
                System.exit(1);
            }
        }
    }


}
