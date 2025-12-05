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
                select s.id as fk_servidor, s.apelido as apelido, round(coalesce(max(a.max), 0), 2) as captura,
                case
                when max(case when cast(replace(replace(a.duracao,'min',''),' ','') as unsigned) >= (cast(pa.duracao_min as unsigned) * 2) then 1 else 0 end) = 1 then 'CRITICO'
                when max(case when cast(replace(replace(a.duracao,'min',''),' ','') as unsigned) >= cast(pa.duracao_min as unsigned) then 1 else 0 end) = 1 then 'ATENCAO'
                else 'OK'
                end as classificacao from servidor s
                join parametro_alerta pa on pa.fk_servidor = s.id
                join componentes c on c.id = pa.fk_componente
                join alertas a on a.fk_parametro = pa.id
                and a.dt_registro >= (select date_sub(coalesce(max(dt_registro), now()), interval 1 day) from alertas)
                where c.tipo = (?) and pa.unidade_medida = (?)
                group by s.id, s.apelido
                having classificacao <> 'OK'
                order by s.id;
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
            saida = new OutputStreamWriter(new FileOutputStream(nome), StandardCharsets.UTF_8);
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
