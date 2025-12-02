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
import java.time.format.DateTimeFormatter;
import java.util.*;

import static java.lang.System.out;

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
            LogsGiuliaCriticidade logCriticidade = new LogsGiuliaCriticidade(log.getFk_servidor(), log.getNomeMaquina(), timestamp, 0, log.getCpu(), log.getRam(), log.getDisco(), log.getTmp_cpu(), log.getTmp_disco(), log.getUpload_bytes(), log.getDownload_bytes(), log.getPacotes_recebidos(), log.getPacotes_enviados(), 0, 0, 0, 0, 0);
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

    public Map<LocalDateTime, Integer> calcularAlertas(List<LogsGiuliaCriticidade> logsServidor, Double limite, Integer duracao, String componente, Integer dias, String medida, Boolean precisaDuracao){

        Map<LocalDateTime, Integer> contadorAlertas = new TreeMap<>();
        Boolean emPico = false;
        LocalDateTime inicioPico = null;
        LocalDateTime ultimoAcima = null;

        for (int i = 0; i < logsServidor.size(); i++) {
            LogsGiuliaCriticidade logAtual = logsServidor.get(i);
            LocalDateTime t = logAtual.getTimestamp();
            if (t == null){
                continue;
            }
            Double valor = 0.0;

            if (componente.equalsIgnoreCase("CPU") && medida.equals("%")){
                valor = logAtual.getUsoCpu();
            }

            if (componente.equalsIgnoreCase("RAM")){
                valor = logAtual.getUsoRam();
            }

            if(componente.equalsIgnoreCase("DISCO") && medida.equals("%")){
                valor = logAtual.getUsoDisco();
            }

            if (componente.equalsIgnoreCase("CPU") && medida.equals("C")){
                valor = logAtual.getTempCpu();
            }

            if (componente.equalsIgnoreCase("DISCO") && medida.equals("C")){
                valor = logAtual.getTempDisco();
            }

            Boolean acimaLimite = valor > limite;

            if (acimaLimite){
                if (!emPico) {
                    emPico = true;
                    inicioPico = t;
                }
                ultimoAcima = t;
            }

            else{
                if (emPico && inicioPico != null && ultimoAcima != null){
                    long seg = Duration.between(inicioPico, ultimoAcima).toSeconds();
                    Integer minutos = (int) Math.max(1, (seg + 59) / 60);

                    Boolean calcular = !precisaDuracao || minutos >= duracao;

                    if (calcular){
                        LocalDateTime ts = validarDias(ultimoAcima, dias);
                        Integer atual = contadorAlertas.get(ts);
                        if (atual == null) contadorAlertas.put(ts, 1);
                        else contadorAlertas.put(ts, atual + 1);

                    }
                }
                emPico = false;
                inicioPico = null;
                ultimoAcima = null;
            }
        }

        if (emPico && inicioPico != null && ultimoAcima != null) {
            long seg = Duration.between(inicioPico, ultimoAcima).toSeconds();
            long minutos = Math.max(1, (seg + 59) / 60);

            Boolean conta = !precisaDuracao || minutos >= duracao;
            if (conta) {
                LocalDateTime ts = validarDias(ultimoAcima, dias);
                Integer atual = contadorAlertas.get(ts);
                if (atual == null) contadorAlertas.put(ts, 1);
                else contadorAlertas.put(ts, atual + 1);
            }
        }
        return contadorAlertas;
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

    public Map<LocalDateTime, Integer> calcularAlertasRede(List<LogsGiuliaCriticidade> logsServidor, Double limite, Integer duracao, Integer dias, String unidadeMedida) {
        Map<LocalDateTime, Integer> contador = new TreeMap<>();

        Boolean emFaixa = false;
        Integer faixaAtual = 0;
        LocalDateTime inicio = null;
        LocalDateTime fim = null;

        for (LogsGiuliaCriticidade log : logsServidor) {
            LocalDateTime t = log.getTimestamp();
            if (t == null) {
                continue;
            }

            Double uso = (double) getValorRede(log, unidadeMedida);

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

            if (emFaixa && inicio != null && fim != null && (faixa == 0 || faixa != faixaAtual)) {
                long seg = Duration.between(inicio, fim).toSeconds();
                Integer minutos = (int) Math.max(1, (seg + 59) / 60);

                if (minutos >= duracao) {
                    LocalDateTime ts = validarDias(fim, dias);
                    contador.merge(ts, 1, Integer::sum);
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
            int minutos = (int) Math.max(1, (seg + 59) / 60);

            if (minutos >= duracao) {
                LocalDateTime ts = validarDias(fim, dias);
                contador.merge(ts, 1, Integer::sum);
            }
        }
        return contador;
    }


    public void classificarAlertas(List<Logs> logsConsolidados, Integer dias) {
        out.println("\n⚠\uFE0F Classificando alertas de um servidor...");

        if (logsConsolidados == null || logsConsolidados.isEmpty()) {
            out.println("Nenhum log recebido para calcular alertas");
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
            List<Double> listaUp = con.queryForList(selectTipo, Double.class, "REDE", id, "UPLOAD");
            List<Double> listaDown = con.queryForList(selectTipo, Double.class, "REDE", id, "DOWNLOAD");
            List<Double> listaRcvd = con.queryForList(selectTipo, Double.class, "REDE", id, "PCKT_RCVD");
            List<Double> listaSnt = con.queryForList(selectTipo, Double.class, "REDE", id, "PCKT_SNT");

            if (listaCpu.isEmpty() || listaRam.isEmpty() || listaDisco.isEmpty() || listaTempCpu.isEmpty() || listaTempDisco.isEmpty() || listaUp.isEmpty() || listaDown.isEmpty() || listaRcvd.isEmpty() || listaSnt.isEmpty()) {
                out.printf("Servidor %d sem parametro_alerta completo (CPU/RAM/DISCO/TempCPU/TempDISCO/UP/DOWN/RCVD/SNT). Pulando.%n", id);
                continue;
            }

            Double limiteCpu = listaCpu.get(0);
            Double limiteRam = listaRam.get(0);
            Double limiteDisco = listaDisco.get(0);
            Double limiteTempCpu = listaTempCpu.get(0);
            Double limiteTempDisco = listaTempDisco.get(0);
            Double limiteUp = listaUp.get(0);
            Double limiteDown = listaDown.get(0);
            Double limiteRcvd = listaRcvd.get(0);
            Double limiteSnt = listaSnt.get(0);

            List<Integer> listaDuracaoCpu = con.queryForList(selectDuracao, Integer.class, "CPU", id, "%");
            List<Integer> listaDuracaoRam = con.queryForList(selectDuracao, Integer.class, "RAM", id, "%");
            List<Integer> listaDuracaoDisco = con.queryForList(selectDuracao, Integer.class, "DISCO", id, "%");
            List<Integer> listaDuracaoTempCpu = con.queryForList(selectDuracao, Integer.class, "CPU", id, "C");
            List<Integer> listaDuracaoTempDisco = con.queryForList(selectDuracao, Integer.class, "DISCO", id, "C");
            List<Integer> listaDuracaoUp = con.queryForList(selectDuracao, Integer.class, "REDE", id, "UPLOAD");
            List<Integer> listaDuracaoDown = con.queryForList(selectDuracao, Integer.class, "REDE", id, "DOWNLOAD");
            List<Integer> listaDuracaoRcvd = con.queryForList(selectDuracao, Integer.class, "REDE", id, "PCKT_RCVD");
            List<Integer> listaDuracaoSnt = con.queryForList(selectDuracao, Integer.class, "REDE", id, "PCKT_SNT");

            if (listaDuracaoCpu.isEmpty() || listaDuracaoRam.isEmpty() || listaDuracaoDisco.isEmpty() || listaDuracaoTempCpu.isEmpty() || listaDuracaoTempDisco.isEmpty() || listaDuracaoUp.isEmpty() || listaDuracaoDown.isEmpty() || listaDuracaoRcvd.isEmpty() || listaDuracaoSnt.isEmpty()) {
                out.printf("Servidor %d sem duracao_min completo. Pulando.%n", id);
                continue;
            }

            Integer duracaoCpu = listaDuracaoCpu.get(0);
            Integer duracaoRam = listaDuracaoRam.get(0);
            Integer duracaoDisco = listaDuracaoDisco.get(0);
            Integer duracaoTempCpu = listaDuracaoTempCpu.get(0);
            Integer duracaoTempDisco = listaDuracaoTempDisco.get(0);
            Integer duracaoUp = listaDuracaoUp.get(0);
            Integer duracaoDown = listaDuracaoDown.get(0);
            Integer duracaoRcvd = listaDuracaoRcvd.get(0);
            Integer duracaoSnt = listaDuracaoSnt.get(0);

            Map<LocalDateTime, Integer> alertasCpu = calcularAlertas(logsPeriodo, limiteCpu, duracaoCpu, "CPU", dias, "%", true);
            Map<LocalDateTime, Integer> alertasRam = calcularAlertas(logsPeriodo, limiteRam, duracaoRam, "RAM", dias, "%", true);
            Map<LocalDateTime, Integer> alertasDisco = calcularAlertas(logsPeriodo, limiteDisco, duracaoDisco, "DISCO", dias, "%", true);
            Map<LocalDateTime, Integer> alertasTempCpu = calcularAlertas(logsPeriodo, limiteTempCpu, duracaoTempCpu, "CPU", dias, "C", false);
            Map<LocalDateTime, Integer> alertasTempDisco = calcularAlertas(logsPeriodo, limiteTempDisco, duracaoTempDisco, "DISCO", dias, "C", false);
            Map<LocalDateTime, Integer> alertasUp = calcularAlertasRede(logsPeriodo, limiteUp, duracaoUp, dias, "UPLOAD");
            Map<LocalDateTime, Integer> alertasDown = calcularAlertasRede(logsPeriodo, limiteDown, duracaoDown, dias, "DOWNLOAD");
            Map<LocalDateTime, Integer> alertasRcvd = calcularAlertasRede(logsPeriodo, limiteRcvd, duracaoRcvd, dias, "PCKT_RCVD");
            Map<LocalDateTime, Integer> alertasSnt = calcularAlertasRede(logsPeriodo, limiteSnt, duracaoSnt, dias, "PCKT_SNT");

            Map<LocalDateTime, Integer> alertasCpuFinal = new TreeMap<>();
            alertasCpuFinal.putAll(alertasCpu);
            for (var e : alertasTempCpu.entrySet()) {
                alertasCpuFinal.merge(e.getKey(), e.getValue(), Integer::sum);
            }

            Map<LocalDateTime, Integer> alertasDiscoFinal = new TreeMap<>();
            alertasDiscoFinal.putAll(alertasDisco);
            for (var e : alertasTempDisco.entrySet()) {
                alertasDiscoFinal.merge(e.getKey(), e.getValue(), Integer::sum);
            }

            Map<LocalDateTime, Integer> alertasRede = new TreeMap<>();
            for (var e : alertasUp.entrySet()) {
                alertasRede.merge(e.getKey(), e.getValue(), Integer::sum);
            }

            for (var e : alertasDown.entrySet()) {
                alertasRede.merge(e.getKey(), e.getValue(), Integer::sum);
            }

            for (var e : alertasRcvd.entrySet()) {
                alertasRede.merge(e.getKey(), e.getValue(), Integer::sum);
            }

            for (var e : alertasSnt.entrySet()) {
                alertasRede.merge(e.getKey(), e.getValue(), Integer::sum);
            }

            Set<LocalDateTime> alertas = new TreeSet<>();
            alertas.addAll(alertasCpuFinal.keySet());
            alertas.addAll(alertasRam.keySet());
            alertas.addAll(alertasDiscoFinal.keySet());
            alertas.addAll(alertasRede.keySet());

            String apelido = logsServidor.isEmpty() ? "" : logsServidor.get(0).getApelido();

            DateTimeFormatter fmt = (dias == 1) ? DateTimeFormatter.ofPattern("dd/MM/yyyy HH:00") : DateTimeFormatter.ofPattern("dd/MM/yyyy");

            for (LocalDateTime k : alertas) {
                Integer tmpCpu = alertasCpuFinal.get(k);
                Integer aCpu = (tmpCpu == null ? 0 : tmpCpu);

                Integer tmpRam = alertasRam.get(k);
                Integer aRam = (tmpRam == null ? 0 : tmpRam);

                Integer tmpDisco = alertasDiscoFinal.get(k);
                Integer aDisco = (tmpDisco == null ? 0 : tmpDisco);

                Integer tmpRede = alertasRede.get(k);
                Integer aRede = (tmpRede == null ? 0 : tmpRede);

                Map<String, Object> row = new LinkedHashMap<>();
                row.put("fk_servidor", id);
                row.put("apelido", logsServidor.isEmpty() ? "" : logsServidor.get(0).getApelido());
                row.put("timestamp", k.format(fmt));
                row.put("alertasCpu", aCpu);
                row.put("alertasRam", aRam);
                row.put("alertasDisco", aDisco);
                row.put("alertasRede", aRede);
                row.put("totalAlertas", aCpu + aRam + aDisco + aRede);

                lista.add(row);
            }

        }
        String nome = "historicoAlertasLinhas_" + dias;
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
                Map<String, Object> obj = lista.get(i);
                if (i > 0) {
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
                           "alertasRede": %d,
                           "totalAlertas": %d
                           }""",
                        (Integer) obj.get("fk_servidor"),
                        (String) obj.get("apelido"),
                        (String) obj.get("timestamp"),
                        (Integer) obj.get("alertasCpu"),
                        (Integer) obj.get("alertasRam"),
                        (Integer) obj.get("alertasDisco"),
                        (Integer) obj.get("alertasRede"),
                        (Integer) obj.get("totalAlertas")
                ));
            }
            saida.append("]");
            out.println("Arquivo Json de histórico de um servidor gerado com sucesso!");

        }

        catch (IOException erro) {
            out.println("Erro ao gravar o arquivo Json de histórico de um servidor!");
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
                out.println("Erro ao fechar o arquivo Json de histórico de um servidor!");
                deuRuim = true;
            }

            if (deuRuim) {
                System.exit(1);
            }
        }
    }
}
