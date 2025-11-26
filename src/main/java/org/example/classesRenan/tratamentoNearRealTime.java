package org.example.classesRenan;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.example.*;
import org.example.Connection;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class tratamentoNearRealTime {
    private static final Log log = LogFactory.getLog(tratamentoNearRealTime.class);

    public static List<LogsNearRealTime> csvGeral(List<Logs> lista){
        List<LogsNearRealTime> logsNear = new ArrayList<>();

        for (Logs log : lista) {
            LogsNearRealTime logsNearRealTime = new LogsNearRealTime( log.getFk_servidor(), log.getDownload_bytes(),
                    log.getUpload_bytes(), log.getTmp_disco(), log.getTmp_cpu(), log.getCpu(),
                    log.getDisco(), log.getRam(), log.getDataHora());
            logsNear.add(logsNearRealTime);
        }

        return logsNear;
    }

    public static List<Integer> getIdsServidores(List<LogsNearRealTime> lista){
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


    public static void logsEspecifico(List<Logs> lista){
        List<LogsNearRealTime> listalog = csvGeral(lista);
        List<Integer> listaIds = getIdsServidores(listalog);
        List<LogsNearRealTime> logServidor = new ArrayList<>();
        List<LogsNearRealTime> logfinal = new ArrayList<>();

        for (int i = 0; i < listaIds.size(); i++) {
            logServidor.clear();
            for (int j = 0; j < listalog.size(); j++) {
                if (listaIds.get(i).equals(listalog.get(j).getFk_servidor())){
                    logServidor.add(listalog.get(j));
                }
            }
            if (!logServidor.isEmpty()) {
                LogsNearRealTime ultimoLog = logServidor.getLast();
                logfinal.add(ultimoLog);

                String nomeArquivo = "data" + ultimoLog.getFk_servidor() + ".json";
                JdbcTemplate con = new JdbcTemplate(new Connection().getDataSource());
                ParametrosServidor params = carregarParametros(ultimoLog.getFk_servidor(), con);

                try {
                    gerarJson(ultimoLog, params, nomeArquivo);
                    new AwsConnection().uploadBucketClient("DashNearRealTime", nomeArquivo);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

        }
    }
    public static ParametrosServidor carregarParametros(Integer fkServidor, JdbcTemplate con) {

        ParametrosServidor params = new ParametrosServidor();

        String url = "jdbc:mysql://localhost:3306/infomotion";
        String user = "root";
        String pass = "041316miralha";

        String sql = """
        SELECT max, tipo, unidade_medida
        FROM parametro_alerta p
        INNER JOIN componentes c ON c.id = p.fk_componente
        WHERE p.fk_servidor = ?;
    """;

        try (Connection conn = DriverManager.getConnection(url, user, pass);
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

        if (params.maxCpuUso == null)      params.maxCpuUso = 50.0;
        if (params.maxCpuTemp == null)     params.maxCpuTemp = 70.0;

        if (params.maxRam == null)         params.maxRam = 50.0;

        if (params.maxDiscoUso == null)    params.maxDiscoUso = 50.0;
        if (params.maxDiscoTemp == null)   params.maxDiscoTemp = 60.0;

        if (params.maxRedeDownload == null) params.maxRedeDownload = 5000000L;
        if (params.maxRedeUpload == null)   params.maxRedeUpload = 5000000L;

        return params;
    }



    public static void gerarJson(LogsNearRealTime log, ParametrosServidor params,  String nomeArq) throws IOException {
        OutputStreamWriter saida = null;
        boolean deuRuim = false;

        try {
            saida = new OutputStreamWriter(new FileOutputStream(nomeArq), StandardCharsets.UTF_8);

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
}
""",
                    log.getFk_servidor(),
                    log.getTimeStamp(),

                    // ATUAIS
                    log.getRam(),
                    log.getCpu(),
                    log.getDisco(),
                    log.getTemperatura_cpu(),
                    log.getTemperatura_disco(),
                    uploadMB,
                    downloadMB,

                    // MAXIMOS
                    params.maxRam,
                    params.maxCpuUso,
                    params.maxCpuTemp,
                    params.maxDiscoUso,
                    params.maxDiscoTemp,
                    maxDownloadMB,
                    maxUploadMB
            ));

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
