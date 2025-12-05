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

    public void gerarBolhas(String tipo, String unidade){
        System.out.println("\n-> Gerando bolhas...");

        String select = """
                SELECT
                s.id AS fk_servidor,
                s.apelido AS apelido,
                ROUND(COALESCE(MAX(a.max), 0), 2) AS captura,
                CASE
                WHEN MAX(
                CASE
                WHEN CAST(REPLACE(REPLACE(a.duracao,'min',''),' ','') AS UNSIGNED)
                >= (CAST(pa.duracao_min AS UNSIGNED) * 2)
                THEN 1 ELSE 0
                END
                ) = 1 THEN 'CRITICO'
                WHEN MAX(
                CASE
                WHEN CAST(REPLACE(REPLACE(a.duracao,'min',''),' ','') AS UNSIGNED)
                >= CAST(pa.duracao_min AS UNSIGNED)
                THEN 1 ELSE 0
                END
                ) = 1 THEN 'ATENCAO'
                ELSE 'OK'
                END AS classificacao
                FROM servidor s
                JOIN parametro_alerta pa ON pa.fk_servidor = s.id
                JOIN componentes c ON c.id = pa.fk_componente
                JOIN alertas a
                ON a.fk_parametro = pa.id
                AND a.dt_registro >= (NOW() - INTERVAL 1 DAY)
                WHERE c.tipo = (?)
                AND pa.unidade_medida = (?)
                GROUP BY s.id, s.apelido
                HAVING classificacao <> 'OK'
                ORDER BY s.id;
                """;

        List<Map<String, Object>> lista = con.queryForList(select, tipo, unidade);
        String nome = "bolhas_" + tipo + "_" + unidade;
        gravaArquivoJson(lista, nome);
        awsCon.uploadBucketClient(PASTA_CLIENT, nome + ".json");

    }



    public static void gravaArquivoJson(List<Map<String, Object>> lista, String nomeArq) {

        String nome = nomeArq.endsWith(".json") ? nomeArq : nomeArq + ".json";
        OutputStreamWriter saida = null;
        Boolean deuRuim = false;

        try {
            saida = new OutputStreamWriter(new FileOutputStream("/tmp/" + nome), StandardCharsets.UTF_8);
            saida.append("[");

            for (int i = 0; i < lista.size(); i++) {
                Map<String, Object> l = lista.get(i);

                Integer fk_servidor = ((Number) l.get("fk_servidor")).intValue();
                String apelido = l.get("apelido").toString();
                Double captura = ((Number) l.get("captura")).doubleValue();
                String classificacao = String.valueOf(l.get("classificacao"));

                if(i > 0){
                    saida.write(',');
                }

                saida.write(String.format(Locale.US, """
                {
                  "fk_servidor": %d,
                  "apelido": "%s",
                  "captura": %.2f,
                  "classificacao": "%s"
                }
                """, fk_servidor, apelido, captura, classificacao));


            }

            saida.append("]");
            saida.flush();
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