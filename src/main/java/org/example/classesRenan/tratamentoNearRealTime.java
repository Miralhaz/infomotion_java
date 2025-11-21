package org.example.classesRenan;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.example.AwsConnection;
import org.example.Logs;

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

                try {
                    gerarJson(ultimoLog, nomeArquivo);
                    new AwsConnection().uploadBucketClient("DashNearRealTime", nomeArquivo);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

        }
    }

    public static void gerarJson(LogsNearRealTime log, String nomeArq) throws IOException {
        OutputStreamWriter saida = null;
        boolean deuRuim = false;

        try {
            saida = new OutputStreamWriter(new FileOutputStream(nomeArq), StandardCharsets.UTF_8);

            double uploadMB = log.getUploadByte() / 1024.0 / 1024.0;
            double downloadMB = log.getDownloadByte() / 1024.0 / 1024.0;

            saida.write(String.format(Locale.US, """
            {
                "fk_servidor": %d,
                "timeStamp": "%s",
                "ram": %.2f,
                "cpu": %.2f,
                "disco": %.2f,
                "temperatura_cpu": %.2f,
                "temperatura_disco": %.2f,
                "uploadMB": %.2f,
                "downloadMB": %.2f
            }
            """,
                    log.getFk_servidor(),
                    log.getTimeStamp(),
                    log.getRam(),
                    log.getCpu(),
                    log.getDisco(),
                    log.getTemperatura_cpu(),
                    log.getTemperatura_disco(),
                    uploadMB,
                    downloadMB
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
