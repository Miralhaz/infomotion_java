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
        System.out.println("\n\uD83E\uDEE7 Gerando bolhas de CPU...");

        List<LogsGiuliaCriticidade> listaLogs = transformarLogs(logsConsolidados);
        Set<Integer> idsServidores = new LinkedHashSet<>();

        for (LogsGiuliaCriticidade log : listaLogs) {
            idsServidores.add(log.getFk_servidor());
        }

        List<LogsGiuliaCriticidade> listaBolhasCpu = new ArrayList<>();
        String selectTipo = ("""
                select cast(pa.max as decimal(10,2)) as limite
                from parametro_alerta pa
                inner join componentes c on c.id = pa.fk_componente
                where c.tipo = 'CPU' and pa.fk_servidor = (?) and pa.unidade_medida = '%';
            """);

        String selectDuracao = ("""
                select cast(pa.duracao_min as unsigned) as duracao
                from parametro_alerta pa
                inner join componentes c on c.id = pa.fk_componente
                where c.tipo = 'CPU' and pa.fk_servidor = (?) and pa.unidade_medida = '%';
            """);

        for (Integer id : idsServidores) {
            List<LogsGiuliaCriticidade> logsServidor = new ArrayList<>();

            Integer contadorMinutos = 0;
            Integer contadorDuracao = 0;
            LocalDateTime dataInicioCaptura = null;
            Double maxPercentual = 0.0;

            for (LogsGiuliaCriticidade log : listaLogs) {
                if (log.getFk_servidor().equals(id)) {
                    logsServidor.add(log);
                }
            }

            logsServidor.sort(Comparator.comparing(LogsGiuliaCriticidade::getTimestamp));

            List<Double> listaCpu = con.queryForList(selectTipo, Double.class, id);
            List<Integer> listaDuracaoCpu = con.queryForList(selectDuracao, Integer.class, id);

            if (listaCpu.isEmpty() || listaDuracaoCpu.isEmpty()) {
                System.out.printf("Servidor %d sem parametro CPU em %% - pulando.%n", id);
                continue;
            }

            Double limiteCpu = listaCpu.get(0);
            Integer duracaoCpu = listaDuracaoCpu.get(0);

            for (int i = 0; i < logsServidor.size() - 1; i++) {
                LogsGiuliaCriticidade logAtual = logsServidor.get(i);
                Double uso = logAtual.getUsoCpu();
                maxPercentual = Math.max(maxPercentual, uso);
                Boolean acimaLimite = uso > limiteCpu;

                if (acimaLimite){
                    contadorDuracao++;

                    if (contadorDuracao == 1){
                        dataInicioCaptura = logAtual.getTimestamp();
                    }
                }

                else{
                    if (contadorDuracao >= duracaoCpu && dataInicioCaptura != null){
                        LocalDateTime dataFinalCaptura = logsServidor.get(i - 1).getTimestamp();
                        long seg = Duration.between(dataInicioCaptura, dataFinalCaptura).toSeconds();
                        Integer minutos = (int) Math.max(1, (seg + 59)/60);
                        contadorMinutos += minutos;
                    }
                    contadorDuracao = 0;
                    dataInicioCaptura = null;
                }
            }

            if (contadorDuracao >= duracaoCpu && dataInicioCaptura != null){
                LocalDateTime dataFinalCaptura = logsServidor.get(logsServidor.size() - 1).getTimestamp();
                long seg = Duration.between(dataInicioCaptura, dataFinalCaptura).toSeconds();
                Integer minutos = (int) Math.max(1, (seg + 59)/60);
                contadorMinutos += minutos;
            }

            if (contadorMinutos > 0){
                String apelidoServidor = logsServidor.get(0).getApelido();
                String classificacao = "";
                LogsGiuliaCriticidade bolha = new LogsGiuliaCriticidade(id, apelidoServidor, maxPercentual, contadorMinutos, classificacao);

                if (contadorMinutos >= 30) {
                    classificacao = "CRITICO";
                }

                else if (contadorMinutos >= 5) {
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
        System.out.println("\n\uD83E\uDEE7 Gerando bolhas de RAM...");

        List<LogsGiuliaCriticidade> listaLogs = transformarLogs(logsConsolidados);
        Set<Integer> idsServidores = new LinkedHashSet<>();

        for (LogsGiuliaCriticidade log : listaLogs) {
            idsServidores.add(log.getFk_servidor());
        }

        List<LogsGiuliaCriticidade> listaBolhasRam = new ArrayList<>();
        String selectTipo = ("""
                select cast(pa.max as decimal(10,2)) as limite
                from parametro_alerta pa
                inner join componentes c on c.id = pa.fk_componente
                where c.tipo = 'RAM' and pa.fk_servidor = (?) and pa.unidade_medida = '%';
            """);

        String selectDuracao = ("""
                select cast(pa.duracao_min as unsigned) as duracao
                from parametro_alerta pa
                inner join componentes c on c.id = pa.fk_componente
                where c.tipo = 'RAM' and pa.fk_servidor = (?) and pa.unidade_medida = '%';
            """);

        for (Integer id : idsServidores) {
            List<LogsGiuliaCriticidade> logsServidor = new ArrayList<>();

            Integer contadorMinutos = 0;
            Integer contadorDuracao = 0;
            LocalDateTime dataInicioCaptura = null;
            Double maxPercentual = 0.0;

            for (LogsGiuliaCriticidade log : listaLogs) {
                if (log.getFk_servidor().equals(id)) {
                    logsServidor.add(log);
                }
            }

            logsServidor.sort(Comparator.comparing(LogsGiuliaCriticidade::getTimestamp));

            List<Double> listaRam = con.queryForList(selectTipo, Double.class, id);
            List<Integer> listaDuracaoRam = con.queryForList(selectDuracao, Integer.class, id);

            if (listaRam.isEmpty() || listaDuracaoRam.isEmpty()) {
                System.out.printf("Servidor %d sem parametro RAM em %% - pulando.%n", id);
                continue;
            }

            Double limiteRam = listaRam.get(0);
            Integer duracaoRam = listaDuracaoRam.get(0);

            for (int i = 0; i < logsServidor.size() - 1; i++) {
                LogsGiuliaCriticidade logAtual = logsServidor.get(i);
                Double uso = logAtual.getUsoRam();
                maxPercentual = Math.max(maxPercentual, uso);
                Boolean acimaLimite = uso > limiteRam;

                if (acimaLimite){
                    contadorDuracao++;

                    if (contadorDuracao == 1){
                        dataInicioCaptura = logAtual.getTimestamp();
                    }
                }

                else{
                    if (contadorDuracao >= duracaoRam && dataInicioCaptura != null){
                        LocalDateTime dataFinalCaptura = logsServidor.get(i - 1).getTimestamp();
                        long seg = Duration.between(dataInicioCaptura, dataFinalCaptura).toSeconds();
                        Integer minutos = (int) Math.max(1, (seg + 59)/60);
                        contadorMinutos += minutos;
                    }
                    contadorDuracao = 0;
                    dataInicioCaptura = null;
                }
            }

            if (contadorDuracao >= duracaoRam && dataInicioCaptura != null){
                LocalDateTime dataFinalCaptura = logsServidor.get(logsServidor.size() - 1).getTimestamp();
                long seg = Duration.between(dataInicioCaptura, dataFinalCaptura).toSeconds();
                Integer minutos = (int) Math.max(1, (seg + 59)/60);
                contadorMinutos += minutos;
            }

            if (contadorMinutos > 0){
                String apelidoServidor = logsServidor.get(0).getApelido();
                String classificacao = "";
                LogsGiuliaCriticidade bolha = new LogsGiuliaCriticidade(id, apelidoServidor, maxPercentual, contadorMinutos, classificacao);

                if (contadorMinutos >= 30) {
                    classificacao = "CRITICO";
                }

                else if (contadorMinutos >= 5) {
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
        System.out.println("\n\uD83E\uDEE7 Gerando bolhas de DISCO...");

        List<LogsGiuliaCriticidade> listaLogs = transformarLogs(logsConsolidados);
        Set<Integer> idsServidores = new LinkedHashSet<>();

        for (LogsGiuliaCriticidade log : listaLogs) {
            idsServidores.add(log.getFk_servidor());
        }

        List<LogsGiuliaCriticidade> listaBolhasDisco = new ArrayList<>();
        String selectTipo = ("""
                select cast(pa.max as decimal(10,2)) as limite
                from parametro_alerta pa
                inner join componentes c on c.id = pa.fk_componente
                where c.tipo = 'DISCO' and pa.fk_servidor = (?) and pa.unidade_medida = '%';
            """);

        String selectDuracao = ("""
                select cast(pa.duracao_min as unsigned) as duracao
                from parametro_alerta pa
                inner join componentes c on c.id = pa.fk_componente
                where c.tipo = 'DISCO' and pa.fk_servidor = (?) and pa.unidade_medida = '%';
            """);

        for (Integer id : idsServidores) {
            List<LogsGiuliaCriticidade> logsServidor = new ArrayList<>();

            Integer contadorMinutos = 0;
            Integer contadorDuracao = 0;
            LocalDateTime dataInicioCaptura = null;
            Double maxPercentual = 0.0;

            for (LogsGiuliaCriticidade log : listaLogs) {
                if (log.getFk_servidor().equals(id)) {
                    logsServidor.add(log);
                }
            }

            logsServidor.sort(Comparator.comparing(LogsGiuliaCriticidade::getTimestamp));

            List<Double> listaDisco = con.queryForList(selectTipo, Double.class, id);
            List<Integer> listaDuracaoDisco = con.queryForList(selectDuracao, Integer.class, id);

            if (listaDisco.isEmpty() || listaDuracaoDisco.isEmpty()) {
                System.out.printf("Servidor %d sem parametro DISCO em %% - pulando.%n", id);
                continue;
            }

            Double limiteDisco = listaDisco.get(0);
            Integer duracaoDisco = listaDuracaoDisco.get(0);

            for (int i = 0; i < logsServidor.size() - 1; i++) {
                LogsGiuliaCriticidade logAtual = logsServidor.get(i);
                Double uso = logAtual.getUsoDisco();
                maxPercentual = Math.max(maxPercentual, uso);
                Boolean acimaLimite = uso > limiteDisco;

                if (acimaLimite){
                    contadorDuracao++;

                    if (contadorDuracao == 1){
                        dataInicioCaptura = logAtual.getTimestamp();
                    }
                }

                else{
                    if (contadorDuracao >= duracaoDisco && dataInicioCaptura != null){
                        LocalDateTime dataFinalCaptura = logsServidor.get(i - 1).getTimestamp();
                        long seg = Duration.between(dataInicioCaptura, dataFinalCaptura).toSeconds();
                        Integer minutos = (int) Math.max(1, (seg + 59)/60);
                        contadorMinutos += minutos;
                    }
                    contadorDuracao = 0;
                    dataInicioCaptura = null;
                }
            }

            if (contadorDuracao >= duracaoDisco && dataInicioCaptura != null){
                LocalDateTime dataFinalCaptura = logsServidor.get(logsServidor.size() - 1).getTimestamp();
                long seg = Duration.between(dataInicioCaptura, dataFinalCaptura).toSeconds();
                Integer minutos = (int) Math.max(1, (seg + 59)/60);
                contadorMinutos += minutos;
            }

            if (contadorMinutos > 0){
                String apelidoServidor = logsServidor.get(0).getApelido();
                String classificacao = "";
                LogsGiuliaCriticidade bolha = new LogsGiuliaCriticidade(id, apelidoServidor, maxPercentual, contadorMinutos, classificacao);

                if (contadorMinutos >= 30) {
                    classificacao = "CRITICO";
                }

                else if (contadorMinutos >= 5) {
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
        awsCon.uploadBucketClient(PASTA_CLIENT, "criticidadeDisco.json");
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
                           "apelido": "%s",
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
