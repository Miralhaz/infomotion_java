package org.example.classesWillian;

import org.example.AwsConnection;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class TratamentoKpi {

    private BancoParametros repo;
    private AwsConnection aws;

    public TratamentoKpi(BancoParametros repo, AwsConnection aws) {
        this.repo = repo;
        this.aws = aws;
    }

    public void tratarAlertaDisco(List<LogsWillianDisco> logs) {

        Map<Integer, Double> limitesCache = new HashMap<>();
        List<String> alertas = new ArrayList<>();

        for (LogsWillianDisco log : logs) {

            Integer fk = log.getFkServidor();

            // pegar limite do banco (ou cache)
            Double limite;
            if (!limitesCache.containsKey(fk)) {
                limite = repo.buscarLimiteDisco(fk);
                limitesCache.put(fk, limite);
            } else {
                limite = limitesCache.get(fk);
            }

            // se passou do limite → alerta
            if (log.getUsoDisco() >= limite) {

                String texto = String.format(
                        "%s: %.2f%% em uso (limite %.0f%%)",
                        log.getNomeMaquina(),
                        log.getUsoDisco(),
                        limite
                );

                alertas.add(texto);
            }
        }

        salvarJson(alertas, "discos_alerta_espaco.json");
        aws.uploadBucketClient("discos_alerta_espaco.json");

        System.out.println("JSON de alerta de espaço gerado e enviado ao bucket client.");
    }

    private void salvarJson(List<String> alertas, String nomeArq) {
        try (OutputStreamWriter w = new OutputStreamWriter(
                new FileOutputStream(nomeArq), StandardCharsets.UTF_8)) {

            w.write("[\n");
            for (int i = 0; i < alertas.size(); i++) {
                w.write("  \"" + alertas.get(i) + "\"");
                if (i < alertas.size() - 1) w.write(",");
                w.write("\n");
            }
            w.write("]");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
