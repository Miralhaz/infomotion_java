package org.example.classesGiulia;

import org.example.AwsConnection;
import org.example.Logs;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
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


    public void classificarAlertas(Integer dias) {
        out.println("\n⚠\uFE0F Classificando alertas de um servidor...");

        LocalDateTime inicio = LocalDateTime.now().minusDays(dias);
        Timestamp inicioTs = Timestamp.valueOf(inicio);

        String select = """
                    select s.id as fk_servidor, s.apelido, date_format(a.dt_registro, '%d/%m/%Y') as timestamp, (?) as dias,
                    sum(case when c.tipo = 'CPU' then 1 else 0 end) as alertasCpu,
                    sum(case when c.tipo = 'RAM' then 1 else 0 end) as alertasRam,
                    sum(case when c.tipo = 'DISCO' then 1 else 0 end) as alertasDisco,
                    sum(case when c.tipo = 'REDE' then 1 else 0 end) as alertasRede
                    from alertas a
                    join parametro_alerta pa on pa.id = a.fk_parametro
                    join componentes c on c.id = pa.fk_componente
                    join servidor s on s.id = pa.fk_servidor
                    where a.dt_registro >= (now() - interval (?) day)
                    group by s.id, s.apelido, timestamp
                    order by s.id, timestamp;
                    """;

        List<Map<String, Object>> lista = con.queryForList(select, dias, dias);

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
                Map<String, Object> map = lista.get(i);

                Integer fk_servidor = ((Number) map.get("fk_servidor")).intValue();
                String apelido = map.get("apelido").toString();
                String timestamp = map.get("timestamp").toString();
                Integer dias = ((Number) map.get("dias")).intValue();
                Integer alertasCpu = ((Number) map.get("alertasCpu")).intValue();
                Integer alertasRam = ((Number) map.get("alertasRam")).intValue();
                Integer alertasDisco = ((Number) map.get("alertasDisco")).intValue();
                Integer alertasRede = ((Number) map.get("alertasRede")).intValue();
                Integer totalAlertas = alertasCpu + alertasRam + alertasDisco + alertasRede;

                if (i > 0) {
                    saida.append(",");
                }

                saida.write(String.format(Locale.US,""" 
                           {
                                "fk_servidor": %d,
                                "apelido": "%s",
                                "timestamp": "%s",
                                "dias": %d,
                                "alertasCpu": %d,
                                "alertasRam": %d,
                                "alertasDisco": %d,
                                "alertasRede": %d
                           }
                           """, fk_servidor, apelido, timestamp, dias, alertasCpu, alertasRam, alertasDisco, alertasRede
                ));

            }
            saida.append("]");
            System.out.println("Arquivo Json de hisórico de alertas gerado com sucesso!");

        }

        catch (IOException erro) {
            System.out.println("Erro ao gravar o arquivo Json de hisórico de alertas!");
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
                System.out.println("Erro ao fechar o arquivo Json de hisórico de alertas");
                deuRuim = true;
            }

            if (deuRuim) {
                System.exit(1);
            }
        }
    }
}