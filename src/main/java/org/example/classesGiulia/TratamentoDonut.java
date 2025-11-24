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

    public TratamentoDonut(AwsConnection awsCon, JdbcTemplate con) {
        this.awsCon = awsCon;
        this.con = con;
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

        if (logsConsolidados == null || logsConsolidados.isEmpty()) {
            System.out.println("Nenhum log recebido para calcular criticidade (donut).");
            return;
        }

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
                where c.tipo = (?) and pa.fk_servidor = (?) and pa.unidade_medida = '%';
            """);

        for (Integer id : idsServidores){
            List<LogsGiuliaCriticidade> logsServidor = new ArrayList<>();

            for (LogsGiuliaCriticidade log : listaLogs){
                if (log.getFk_servidor().equals(id)){
                    logsServidor.add(log);
                }
            }

            logsServidor.sort(Comparator.comparing(LogsGiuliaCriticidade::getTimestamp));

            List<Double> listaCpu   = con.queryForList(selectTipo, Double.class, "CPU", id);
            List<Double> listaRam   = con.queryForList(selectTipo, Double.class, "RAM", id);
            List<Double> listaDisco = con.queryForList(selectTipo, Double.class, "DISCO", id);


            if (listaCpu.isEmpty() || listaRam.isEmpty() || listaDisco.isEmpty()) {
                System.out.printf(
                        "Servidor %d sem parametro_alerta completo (CPU/RAM/DISCO com unidade '%%'). Pulando esse servidor.%n",
                        id
                );
                continue;
            }

            Double limiteCpu   = listaCpu.get(0);
            Double limiteRam   = listaRam.get(0);
            Double limiteDisco = listaDisco.get(0);

            if (limiteCpu == null && limiteRam == null && limiteDisco == null) {
                System.out.printf("Servidor %d sem parametros de CPU/RAM/DISCO em %% - ignorando no donut.%n", id);
                continue;
            }

            Integer minCpu = 0;
            Integer minRam = 0;
            Integer minDisco = 0;

            if (limiteCpu != null) {
                minCpu = calcularMinutosAcimaComponente(logsServidor, limiteCpu, "CPU");
            }
            if (limiteRam != null) {
                minRam = calcularMinutosAcimaComponente(logsServidor, limiteRam, "RAM");
            }
            if (limiteDisco != null) {
                minDisco = calcularMinutosAcimaComponente(logsServidor, limiteDisco, "DISCO");
            }

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
                log.setMinutos(maiorQtdMinutos);
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
