package org.example.classesGiulia;

import org.example.*;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class TratamentoDonut {

    // Atributos:
    private AwsConnection awsCon;
    private final JdbcTemplate con;
    private static final String PASTA_CLIENT = "tratamentos_giulia";

    // Construtor:
    public TratamentoDonut(AwsConnection awsCon, JdbcTemplate con) {
        this.awsCon = awsCon;
        this.con = con;
    }

    // Métodos:
    private List<LogsGiuliaCriticidade> transformarLogs(List<Logs> logs){
        List<LogsGiuliaCriticidade> listaLogs = new ArrayList<>();

        for (Logs log : logs){
            LocalDateTime timestamp = log.getDataHora();
            LogsGiuliaCriticidade logCriticidade = new LogsGiuliaCriticidade(log.getFk_servidor(), log.getNomeMaquina(), 0, timestamp, log.getCpu(), log.getRam(), log.getDisco(), log.getTmp_cpu(), log.getTmp_disco(), log.getUpload_bytes(), log.getDownload_bytes(), log.getPacotes_recebidos(), log.getPacotes_enviados(), "");
            listaLogs.add(logCriticidade);
        }
        return listaLogs;
    }


    public void classificarCriticidade() {
        System.out.println("\n Classificando criticidade de componentes...");

        String select = """
                select classificacao, count(*) as quantidade
                from (select s.id as fk_servidor, s.apelido,
                case
                when max(case when (replace(a.duracao,'min','')+0) >= (pa.duracao_min * 2) then 1 else 0 end) = 1 then 'CRITICO'
                when max(case when (replace(a.duracao,'min','')+0) >= pa.duracao_min  then 1 else 0 end) = 1 then 'ATENCAO'
                else 'OK'
                end as classificacao
                from servidor s
                left join parametro_alerta pa on pa.fk_servidor = s.id
                left join alertas a on a.fk_parametro = pa.id
                and a.dt_registro >= (now() - interval 1 day)
                group by s.id, s.apelido
                ) x
                group by classificacao;
                """;

        List<Map<String, Object>> lista = con.queryForList(select);

        Integer ok = 0;
        Integer atencao = 0;
        Integer critico = 0;

        for (Map<String, Object> l : lista){
            String c = l.get("classificacao").toString();
            Integer qtd = ((Number) l.get("quantidade")).intValue();

            if ("OK".equalsIgnoreCase(c)){
                ok = qtd;
            } else if ("ATENCAO".equalsIgnoreCase(c)){
                atencao = qtd;
            } else if ("CRITICO".equalsIgnoreCase(c)){
                critico = qtd;
            }

        }

        gravaArquivoJson(ok, atencao, critico, "nivelCriticidadeDonut");
        awsCon.uploadBucketClient(PASTA_CLIENT, "nivelCriticidadeDonut.json");

        System.out.printf("Donut:  OK: %d | ATENCAO: %d | CRITICO: %d%n", ok, atencao, critico);
    }

    public static void gravaArquivoJson(Integer ok, Integer atencao, Integer critico, String nomeArq) {

        String nome = nomeArq.endsWith(".json") ? nomeArq : nomeArq + ".json";

        if(ok == null){
            ok = 0;
        }

        if(atencao == null){
            atencao = 0;
        }

        if(critico == null){
            critico = 0;
        }

        Integer total = (ok + atencao + critico);
        Double porcOk = total == 0 ? 0 : (ok * 100.0 / total);
        Double porcAtencao = total == 0 ? 0 : (atencao * 100.0 / total);
        Double porcCritico = total == 0 ? 0 : (critico * 100.0 / total);

                String json = (String.format(Locale.US,""" 
                           [
                               {"classificacao": "OK", "quantidade": %d, "percentual": %.2f},
                               {"classificacao": "ATENCAO", "quantidade": %d, "percentual": %.2f},
                               {"classificacao": "CRITICO", "quantidade": %d, "percentual": %.2f}
                           ]
                           """, ok, porcOk, atencao, porcAtencao, critico, porcCritico
                        ));

        try {
            OutputStreamWriter saida = new OutputStreamWriter(new FileOutputStream(nome), StandardCharsets.UTF_8);
            saida.write(json);
            saida.flush();
            System.out.println("Arquivo Json de Criticidade (donut) gerado com sucesso!");
        }

        catch (IOException erro) {
            System.out.println("Erro ao gravar o arquivo Json de Criticidade (donut)!");
            erro.printStackTrace();
        }
    }
}
