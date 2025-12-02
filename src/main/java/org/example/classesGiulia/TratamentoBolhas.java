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
                    log.getTmp_cpu(),
                    log.getTmp_disco(),
                    log.getUpload_bytes(),
                    log.getDownload_bytes(),
                    log.getPacotes_recebidos(),
                    log.getPacotes_enviados(),
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

            for (int i = 0; i < logsServidor.size(); i++) {
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

            for (int i = 0; i < logsServidor.size(); i++) {
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

            for (int i = 0; i < logsServidor.size(); i++) {
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

    public void gerarBolhasTempCpu(List<Logs> logsConsolidados) {
        System.out.println("\n\uD83E\uDEE7 Gerando bolhas de Temperatura de CPU...");

        List<LogsGiuliaCriticidade> listaLogs = transformarLogs(logsConsolidados);
        Set<Integer> idsServidores = new LinkedHashSet<>();

        for (LogsGiuliaCriticidade log : listaLogs) {
            idsServidores.add(log.getFk_servidor());
        }

        List<LogsGiuliaCriticidade> listaBolhasTempCpu = new ArrayList<>();
        String selectTipo = ("""
                select cast(pa.max as decimal(10,2)) as limite
                from parametro_alerta pa
                inner join componentes c on c.id = pa.fk_componente
                where c.tipo = 'CPU' and pa.fk_servidor = (?) and pa.unidade_medida = 'C';
            """);

        String selectDuracao = ("""
                select cast(pa.duracao_min as unsigned) as duracao
                from parametro_alerta pa
                inner join componentes c on c.id = pa.fk_componente
                where c.tipo = 'CPU' and pa.fk_servidor = (?) and pa.unidade_medida = 'C';
            """);

        for (Integer id : idsServidores) {
            List<LogsGiuliaCriticidade> logsServidor = new ArrayList<>();

            for (LogsGiuliaCriticidade log : listaLogs) {
                if (log.getFk_servidor().equals(id)) {
                    logsServidor.add(log);
                }
            }

            logsServidor.sort(Comparator.comparing(LogsGiuliaCriticidade::getTimestamp));

            List<Double> listaCpu = con.queryForList(selectTipo, Double.class, id);
            List<Integer> listaDuracaoTempCpu = con.queryForList(selectDuracao, Integer.class, id);

            if (listaCpu.isEmpty() || listaDuracaoTempCpu.isEmpty()) {
                System.out.printf("Servidor %d sem parametro de Temp de CPU em %% - pulando.%n", id);
                continue;
            }

            Double limiteCpu = listaCpu.get(0);
            Integer duracaoCpu = listaDuracaoTempCpu.get(0);


            Boolean emPico = false;
            LocalDateTime inicioPico = null;
            LocalDateTime ultimoAcima = null;

            Integer totalMinutosAcima = 0;
            Boolean critico = false;
            Double maxTemp = 0.0;

            for (int i = 0; i < logsServidor.size(); i++) {
                LogsGiuliaCriticidade logAtual = logsServidor.get(i);
                LocalDateTime t = logAtual.getTimestamp();

                if (t == null){
                    continue;
                }

                Double temp = logAtual.getTempCpu();
                maxTemp = Math.max(maxTemp, temp);
                Boolean acimaLimite = temp > limiteCpu;

                if (acimaLimite){
                    if (!emPico){
                        emPico = true;
                        inicioPico = t;
                    }
                    ultimoAcima = t;
                }

                else{
                    if (emPico && inicioPico != null && ultimoAcima != null){
                        long seg = Duration.between(inicioPico, ultimoAcima).toSeconds();
                        Integer minutos = (int) Math.max(1, (seg + 59)/60);

                        totalMinutosAcima += minutos;
                        if (minutos >= duracaoCpu){
                            critico = true;
                        }
                    }
                    emPico = false;
                    inicioPico = null;
                    ultimoAcima = null;
                }
            }

            if (emPico && inicioPico != null && ultimoAcima != null){
                long seg = Duration.between(inicioPico, ultimoAcima).toSeconds();
                Integer minutos = (int) Math.max(1, (seg + 59)/60);

                totalMinutosAcima += minutos;
                if (minutos >= duracaoCpu){
                    critico = true;
                }
            }

            if (totalMinutosAcima > 0){
                String apelidoServidor = logsServidor.get(0).getApelido();
                String classificacao = critico ? "CRITICO" : "ATENCAO";
                LogsGiuliaCriticidade bolha = new LogsGiuliaCriticidade(id, apelidoServidor, maxTemp, totalMinutosAcima, classificacao);

                listaBolhasTempCpu.add(bolha);
            }
            else {
                String apelidoServidor = logsServidor.get(0).getApelido();
                String classificacao = "OK";
                LogsGiuliaCriticidade bolha = new LogsGiuliaCriticidade(id, apelidoServidor, maxTemp, totalMinutosAcima, classificacao);

                listaBolhasTempCpu.add(bolha);
            }
        }
        gravaArquivoJson(listaBolhasTempCpu, "criticidadeTempCpu");
        awsCon.uploadBucketClient(PASTA_CLIENT, "criticidadeTempCpu.json");
    }

    public void gerarBolhasTempDisco(List<Logs> logsConsolidados) {
        System.out.println("\n\uD83E\uDEE7 Gerando bolhas de Temperatura de DISCO...");

        List<LogsGiuliaCriticidade> listaLogs = transformarLogs(logsConsolidados);
        Set<Integer> idsServidores = new LinkedHashSet<>();

        for (LogsGiuliaCriticidade log : listaLogs) {
            idsServidores.add(log.getFk_servidor());
        }

        List<LogsGiuliaCriticidade> listaBolhasTempDisco = new ArrayList<>();
        String selectTipo = ("""
                select cast(pa.max as decimal(10,2)) as limite
                from parametro_alerta pa
                inner join componentes c on c.id = pa.fk_componente
                where c.tipo = 'DISCO' and pa.fk_servidor = (?) and pa.unidade_medida = 'C';
            """);

        String selectDuracao = ("""
                select cast(pa.duracao_min as unsigned) as duracao
                from parametro_alerta pa
                inner join componentes c on c.id = pa.fk_componente
                where c.tipo = 'DISCO' and pa.fk_servidor = (?) and pa.unidade_medida = 'C';
            """);

        for (Integer id : idsServidores) {
            List<LogsGiuliaCriticidade> logsServidor = new ArrayList<>();

            for (LogsGiuliaCriticidade log : listaLogs) {
                if (log.getFk_servidor().equals(id)) {
                    logsServidor.add(log);
                }
            }

            logsServidor.sort(Comparator.comparing(LogsGiuliaCriticidade::getTimestamp));

            List<Double> listaDisco = con.queryForList(selectTipo, Double.class, id);
            List<Integer> listaDuracaoTempDisco = con.queryForList(selectDuracao, Integer.class, id);

            if (listaDisco.isEmpty() || listaDuracaoTempDisco.isEmpty()) {
                System.out.printf("Servidor %d sem parametro de Temp de CPU em %% - pulando.%n", id);
                continue;
            }

            Double limiteDisco = listaDisco.get(0);
            Integer duracaoDisco = listaDuracaoTempDisco.get(0);


            Boolean emPico = false;
            LocalDateTime inicioPico = null;
            LocalDateTime ultimoAcima = null;

            Integer totalMinutosAcima = 0;
            Boolean critico = false;
            Double maxTemp = 0.0;

            for (int i = 0; i < logsServidor.size(); i++) {
                LogsGiuliaCriticidade logAtual = logsServidor.get(i);
                LocalDateTime t = logAtual.getTimestamp();

                if (t == null){
                    continue;
                }

                Double temp = logAtual.getTempDisco();
                maxTemp = Math.max(maxTemp, temp);
                Boolean acimaLimite = temp > limiteDisco;

                if (acimaLimite){
                    if (!emPico){
                        emPico = true;
                        inicioPico = t;
                    }
                    ultimoAcima = t;
                }

                else{
                    if (emPico && inicioPico != null && ultimoAcima != null){
                        long seg = Duration.between(inicioPico, ultimoAcima).toSeconds();
                        Integer minutos = (int) Math.max(1, (seg + 59)/60);

                        totalMinutosAcima += minutos;
                        if (minutos >= duracaoDisco){
                            critico = true;
                        }
                    }
                    emPico = false;
                    inicioPico = null;
                    ultimoAcima = null;
                }
            }

            if (emPico && inicioPico != null && ultimoAcima != null){
                long seg = Duration.between(inicioPico, ultimoAcima).toSeconds();
                Integer minutos = (int) Math.max(1, (seg + 59)/60);

                totalMinutosAcima += minutos;
                if (minutos >= duracaoDisco){
                    critico = true;
                }
            }

            if (totalMinutosAcima > 0){
                String apelidoServidor = logsServidor.get(0).getApelido();
                String classificacao = critico ? "CRITICO" : "ATENCAO";
                LogsGiuliaCriticidade bolha = new LogsGiuliaCriticidade(id, apelidoServidor, maxTemp, totalMinutosAcima, classificacao);

                listaBolhasTempDisco.add(bolha);
            } else {
                String apelidoServidor = logsServidor.get(0).getApelido();
                String classificacao = "OK";
                LogsGiuliaCriticidade bolha = new LogsGiuliaCriticidade(id, apelidoServidor, maxTemp, totalMinutosAcima, classificacao);

                listaBolhasTempDisco.add(bolha);
            }
        }
        gravaArquivoJson(listaBolhasTempDisco, "criticidadeTempDisco");
        awsCon.uploadBucketClient(PASTA_CLIENT, "criticidadeTempDisco.json");
    }

    private long getValorRede(LogsGiuliaCriticidade log, String unidade) {
        return switch (unidade.toUpperCase()) {
            case "UPLOAD" -> log.getUploadByte();
            case "DOWNLOAD" -> log.getDownloadByte();
            case "PCKT_RCVD" -> log.getPacketReceived();
            case "PCKT_SNT" -> log.getPacketSent();
            default -> 0L;
        };
    }

    public void gerarBolhasRede(List<Logs> logsConsolidados, String unidadeMedida) {
        System.out.printf("\n\uD83E\uDEE7 Gerando bolhas de REDE (%s)...", unidadeMedida);

        List<LogsGiuliaCriticidade> listaLogs = transformarLogs(logsConsolidados);
        Set<Integer> idsServidores = new LinkedHashSet<>();
        for (LogsGiuliaCriticidade log : listaLogs) {
            idsServidores.add(log.getFk_servidor());
        }

        List<LogsGiuliaCriticidade> listaBolhas = new ArrayList<>();

        String selectLimite = ("""
        select cast(pa.max as decimal(18,2)) as limite
        from parametro_alerta pa
        inner join componentes c on c.id = pa.fk_componente
        where c.tipo = 'REDE' and pa.fk_servidor = (?) and pa.unidade_medida = (?);
    """);

        String selectDuracao = ("""
        select cast(pa.duracao_min as unsigned) as duracao
        from parametro_alerta pa
        inner join componentes c on c.id = pa.fk_componente
        where c.tipo = 'REDE' and pa.fk_servidor = (?) and pa.unidade_medida = (?);
    """);

        for (Integer id : idsServidores) {
            List<LogsGiuliaCriticidade> logsServidor = new ArrayList<>();
            for (LogsGiuliaCriticidade log : listaLogs) {
                if (log.getFk_servidor().equals(id)) logsServidor.add(log);
            }
            if (logsServidor.isEmpty()) {
                continue;
            }
            logsServidor.sort(Comparator.comparing(LogsGiuliaCriticidade::getTimestamp));

            List<Double> limites = con.queryForList(selectLimite, Double.class, id, unidadeMedida);
            List<Integer> duracoes = con.queryForList(selectDuracao, Integer.class, id, unidadeMedida);

            if (limites.isEmpty() || duracoes.isEmpty()) {
                System.out.printf("Servidor %d sem parametro REDE (%s) - pulando.%n", id, unidadeMedida);
                continue;
            }

            Double limite = limites.get(0);
            Integer duracaoMin = duracoes.get(0);

            Boolean emFaixa = false;
            Integer faixaAtual = 0;
            LocalDateTime inicio = null;
            LocalDateTime fim = null;

            Integer minutosCritico = 0;
            Integer minutosAtencao = 0;

            Double piorValorObservado = Double.POSITIVE_INFINITY;

            for (LogsGiuliaCriticidade log : logsServidor) {
                LocalDateTime t = log.getTimestamp();
                if (t == null) {
                    continue;
                }

                long usoLong = getValorRede(log, unidadeMedida);
                Double uso = (double) usoLong;
                piorValorObservado = Math.min(piorValorObservado, uso);

                Integer faixa;
                if (uso < limite) {
                    faixa = 2;
                }
                else if (uso <= 2 * limite) {
                    faixa = 1;
                }
                else {
                    faixa = 0;
                }

                if (emFaixa && (faixa == 0 || faixa != faixaAtual)) {
                    long seg = Duration.between(inicio, fim).toSeconds();
                    Integer minutos = (int) Math.max(1, (seg + 59)/60);

                    if (minutos >= duracaoMin) {
                        if (faixaAtual == 2) minutosCritico += minutos;
                        else if (faixaAtual == 1) minutosAtencao += minutos;
                    }

                    emFaixa = false;
                    faixaAtual = 0;
                    inicio = null;
                    fim = null;
                }

                if (faixa > 0) {
                    if (!emFaixa) {
                        emFaixa = true;
                        faixaAtual = faixa;
                        inicio = t;
                    }
                    fim = t;
                }
            }

            if (emFaixa && inicio != null && fim != null) {
                long seg = Duration.between(inicio, fim).toSeconds();
                Integer minutos = (int) Math.max(1, (seg + 59) / 60);

                if (minutos >= duracaoMin) {
                    if (faixaAtual == 2) {
                        minutosCritico += minutos;
                    }
                    else if (faixaAtual == 1) {
                        minutosAtencao += minutos;
                    }
                }
            }

            String apelidoServidor = logsServidor.get(0).getApelido();

            String classificacao;
            int minutos;
            if (minutosCritico > 0) {
                classificacao = "CRITICO";
                minutos = minutosCritico;
            } else if (minutosAtencao > 0) {
                classificacao = "ATENCAO";
                minutos = minutosAtencao;
            } else {
                classificacao = "OK";
                minutos = 0;
            }

            if (piorValorObservado == Double.POSITIVE_INFINITY) {
                piorValorObservado = 0.0;
            }

            LogsGiuliaCriticidade bolha = new LogsGiuliaCriticidade(id, apelidoServidor, piorValorObservado, minutos, classificacao);
            listaBolhas.add(bolha);
        }

        String nomeArq = "criticidade" + unidadeMedida;
        gravaArquivoJson(listaBolhas, nomeArq);
        awsCon.uploadBucketClient(PASTA_CLIENT, nomeArq + ".json");
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
                           "captura": %.2f,
                           "minutos": %d,
                           "classificacao": "%s"
                           }""",
                        log.getFk_servidor(), log.getApelido(), log.getCaptura(), log.getMinutos(), log.getClassificacao() == null ? "" : log.getClassificacao()));
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
