package org.example.classesRenan;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.example.*;
import org.example.Connection;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class tratamentoNearRealTime {
    private static final Log log = LogFactory.getLog(tratamentoNearRealTime.class);

    public static List<LogsNearRealTime> csvGeral(List<Logs> lista) {
        List<LogsNearRealTime> logsNear = new ArrayList<>();

        for (Logs log : lista) {
            LogsNearRealTime logsNearRealTime = new LogsNearRealTime(log.getFk_servidor(), log.getDownload_bytes(),
                    log.getUpload_bytes(), log.getTmp_disco(), log.getTmp_cpu(), log.getCpu(),
                    log.getDisco(), log.getRam(), log.getDataHora());
            logsNear.add(logsNearRealTime);
        }

        return logsNear;
    }

    public static List<Integer> getIdsServidores(List<LogsNearRealTime> lista) {
        List<Integer> ids = new ArrayList<>();
        for (LogsNearRealTime log : lista) {
            Integer idServidores = log.getFk_servidor();
            Boolean tem = false;
            for (int i = 0; i < ids.size(); i++) {
                if (ids.get(i).equals(log.getFk_servidor())) {
                    tem = true;
                }
            }
            if (!tem) {
                ids.add(idServidores);
            }
        }
        return ids;
    }


    public static void logsEspecifico(List<Logs> lista) throws IOException {
        List<LogsNearRealTime> listalog = csvGeral(lista);
        List<Integer> listaIds = getIdsServidores(listalog);
        List<LogsNearRealTime> logServidor = new ArrayList<>();
        List<LogsNearRealTime> logfinal = new ArrayList<>();

        String caminhoEspecificacoes = "/tmp/logs_especificados_consolidados_servidores.csv";
        Map<Integer, LogsEspecificacoes> mapaEspecificacoes = carregarEspecificacoes(caminhoEspecificacoes);

        for (int i = 0; i < listaIds.size(); i++) {
            logServidor.clear();
            for (int j = 0; j < listalog.size(); j++) {
                if (listaIds.get(i).equals(listalog.get(j).getFk_servidor())) {
                    logServidor.add(listalog.get(j));
                }
            }
            if (!logServidor.isEmpty()) {
                LogsNearRealTime ultimoLog = logServidor.getLast();
                logfinal.add(ultimoLog);

                String nomeArquivo = "data" + ultimoLog.getFk_servidor() + ".json";

                JdbcTemplate con = new JdbcTemplate(new Connection().getDataSource());
                ParametrosServidor params = carregarParametros(ultimoLog.getFk_servidor(), con);

                LogsEspecificacoes espec = mapaEspecificacoes.get(ultimoLog.getFk_servidor());

                try {
                    gerarJson(ultimoLog, params, espec, nomeArquivo);
                    new AwsConnection().uploadBucketClient("DashNearRealTime", nomeArquivo);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

        }
    }

    public static ParametrosServidor carregarParametros(Integer fkServidor, JdbcTemplate con) {
        ParametrosServidor params = new ParametrosServidor();

        String sql = """
                    SELECT max, tipo, unidade_medida
                    FROM parametro_alerta p
                    INNER JOIN componentes c ON c.id = p.fk_componente
                    WHERE p.fk_servidor = ?;
                """;

        try (java.sql.Connection conn = con.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, fkServidor);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                double max = rs.getDouble("max");
                String tipo = rs.getString("tipo");
                String unidade = rs.getString("unidade_medida");

                switch (tipo) {

                    case "CPU" -> {
                        if (unidade.equals("%"))
                            params.maxCpuUso = max;
                        else if (unidade.equals("C"))
                            params.maxCpuTemp = max;
                    }

                    case "RAM" -> params.maxRam = max;

                    case "DISCO" -> {
                        if (unidade.equals("%"))
                            params.maxDiscoUso = max;
                        else if (unidade.equals("C"))
                            params.maxDiscoTemp = max;
                    }

                    case "REDE" -> {
                        if (unidade.equals("DOWNLOAD"))
                            params.maxRedeDownload = (long) max;
                        else if (unidade.equals("UPLOAD"))
                            params.maxRedeUpload = (long) max;
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (params.maxCpuUso == null) params.maxCpuUso = 50.0;
        if (params.maxCpuTemp == null) params.maxCpuTemp = 70.0;

        if (params.maxRam == null) params.maxRam = 50.0;

        if (params.maxDiscoUso == null) params.maxDiscoUso = 50.0;
        if (params.maxDiscoTemp == null) params.maxDiscoTemp = 60.0;

        if (params.maxRedeDownload == null) params.maxRedeDownload = 5000000L;
        if (params.maxRedeUpload == null) params.maxRedeUpload = 5000000L;

        return params;
    }

    public static Map<Integer, LogsEspecificacoes> carregarEspecificacoes(String caminhoCsv) throws IOException {
        Map<Integer, LogsEspecificacoes> mapa = new HashMap<>();

        AwsConnection aws = new AwsConnection();

        aws.downloadBucketTrusted("logs_especificados_consolidados_servidores.csv");

        if (!Files.exists(Paths.get(caminhoCsv))) {
            System.out.println("Aviso: Arquivo de especificações não encontrado: " + caminhoCsv);
            System.out.println("Continuando sem especificações de hardware...");
            return mapa;
        }

        List<String> linhas = Files.readAllLines(Paths.get(caminhoCsv), StandardCharsets.UTF_8);

        if (linhas.size() <= 1) return mapa;

        for (int i = 1; i < linhas.size(); i++) {
            String linha = linhas.get(i);
            if (linha == null || linha.isBlank()) continue;

            String[] campos = linha.split(";", -1);
            try {

                if (campos.length < 9) {
                    System.err.printf("Linha ignorada (campos insuf.): %d -> %s%n", i+1, linha);
                    continue;
                }

                Integer fk = tryParseInt(campos[0], null);
                if (fk == null) {
                    System.err.printf("Linha ignorada (fk inválido): %d -> %s%n", i+1, linha);
                    continue;
                }

                Double swap = tryParseDouble(campos[1], 0.0);
                Double ram = tryParseDouble(campos[2], 0.0);
                Integer qtdCpu = tryParseInt(campos[3], 0);
                Integer qtdNucleos = tryParseInt(campos[4], 0);
                Double capacidade = tryParseDouble(campos[5], 0.0);
                Double qtdParticoes = tryParseDouble(campos[6], 0.0);


                String textoParticoes = campos[7];
                List<Particao> listaParticoes = new ArrayList<>();

                if (textoParticoes != null && !textoParticoes.isBlank()) {
                    String[] blocos = textoParticoes.split("\\|");
                    for (String bloco : blocos) {
                        if (bloco == null) continue;
                        bloco = bloco.trim();

                        if (bloco.isEmpty()) continue;


                        String[] partes = bloco.split(":", 2);
                        if (partes.length < 2) {

                            continue;
                        }

                        String nome = partes[0].trim();
                        String valorStr = partes[1].replace("%", "").trim();

                        if (nome.isEmpty() || valorStr.isEmpty()) continue;

                        try {
                            double uso = Double.parseDouble(valorStr.replace(",", "."));
                            listaParticoes.add(new Particao(nome, uso));
                        } catch (NumberFormatException nfe) {
                            System.err.printf("Uso inválido em partição na linha %d: '%s' (valor='%s')%n", i+1, bloco, valorStr);

                        }
                    }
                }

                String dataHora = campos[8].trim();

                List<String> listaStr = listaParticoes.stream()
                        .map(p -> p.getNome() + ": " + p.getUso() + "%")
                        .toList();

                LogsEspecificacoes espec = new LogsEspecificacoes(
                        fk, swap, ram, qtdCpu, qtdNucleos, capacidade,
                        qtdParticoes, listaStr, dataHora
                );

                mapa.put(fk, espec);

            } catch (Exception ex) {
                System.err.printf("Erro ao processar linha %d: %s%n -> %s%n", i+1, linha, ex.getMessage());
                ex.printStackTrace();
            }
        }

        return mapa;
    }

    private static Integer tryParseInt(String s, Integer defaultValue) {
        try {
            if (s == null || s.isBlank()) return defaultValue;
            return Integer.valueOf(s.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private static Double tryParseDouble(String s, Double defaultValue) {
        try {
            if (s == null || s.isBlank()) return defaultValue;
            return Double.valueOf(s.trim().replace(",", "."));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static void gerarJson(LogsNearRealTime log, ParametrosServidor params, LogsEspecificacoes espec, String nomeArq) throws IOException {
        OutputStreamWriter saida = null;
        boolean deuRuim = false;

        if (espec == null) {
            espec = new LogsEspecificacoes( log.getFk_servidor() );
        }


        try {
            saida = new OutputStreamWriter(new FileOutputStream("/tmp/" + nomeArq), StandardCharsets.UTF_8);

            double uploadMB = log.getUploadByte() / 1024.0 / 1024.0;
            double downloadMB = log.getDownloadByte() / 1024.0 / 1024.0;
            double maxDownloadMB = params.maxRedeDownload / 1024.0 / 1024.0;
            double maxUploadMB = params.maxRedeUpload / 1024.0 / 1024.0;

            saida.write(String.format(Locale.US, """
        {
            "fk_servidor": %d,
            "timeStamp": "%s",

            "atual": {
                "ram": %.2f,
                "cpu": %.2f,
                "disco": %.2f,
                "temperatura_cpu": %.2f,
                "temperatura_disco": %.2f,
                "uploadByte": %.2f,
                "downloadByte": %.2f
            },

            "maximo": {
                "maxRam": %.2f,
                "maxCpuUso": %.2f,
                "maxCpuTemp": %.2f,
                "maxDiscoUso": %.2f,
                "maxDiscoTemp": %.2f,
                "maxDownload": %.2f,
                "maxUpload" : %.2f
            },
        """,
                    log.getFk_servidor(),
                    log.getTimeStamp(),


                    log.getRam(),
                    log.getCpu(),
                    log.getDisco(),
                    log.getTemperatura_cpu(),
                    log.getTemperatura_disco(),
                    uploadMB,
                    downloadMB,


                    params.maxRam,
                    params.maxCpuUso,
                    params.maxCpuTemp,
                    params.maxDiscoUso,
                    params.maxDiscoTemp,
                    maxDownloadMB,
                    maxUploadMB
            ));



            saida.write("""
            "particoes": [
        """);

            List<Particao> listaParticoes = espec.getParticoes();
            for (int i = 0; i < listaParticoes.size(); i++) {
                Particao p = listaParticoes.get(i);

                saida.write(String.format(Locale.US,
                        """
                            { "nome": "%s", "uso": %.2f }%s
                        """,
                        p.getNome(),
                        p.getUso(),
                        (i < listaParticoes.size() - 1 ? "," : "")
                ));
            }

            saida.write("\n    ]\n}\n");

        } catch (IOException e) {
            deuRuim = true;
            throw e;
        } finally {
            if (saida != null) {
                try {
                    saida.close();
                } catch (IOException e) {
                    deuRuim = true;
                }
            }

            if (deuRuim) {
                System.err.println("Erro ao gerar JSON " + nomeArq);
            }
        }
    }


}