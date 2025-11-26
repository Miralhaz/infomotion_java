package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.File;
import java.io.FileWriter;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TratamentoCardsServidores {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void atualizarStatusServidor(
            Integer fk_servidor,
            List<Logs> logsDoArquivo,
            AwsConnection aws,
            JdbcTemplate con) {

        try {
            System.out.println("\nAtualizando status do servidor " + fk_servidor + "...");

            String nomeArquivo = "servidores_status_atual.json";
            List<Map<String, Object>> todosStatus = new ArrayList<>();

            try {
                aws.downloadCardsServidoresBucket(nomeArquivo);

                ObjectMapper mapper = new ObjectMapper();
                File arquivo = new File(nomeArquivo);
                if (arquivo.exists()) {
                    todosStatus = mapper.readValue(arquivo, List.class);
                }
            } catch (Exception e) {
                System.out.println("Arquivo de status não existe, criando novo...");
            }

            String selectServidor = "select apelido, ip from servidor where id = ?";
            Map<String, Object> infoServidor = con.queryForMap(selectServidor, fk_servidor);

            Logs ultimoLog = logsDoArquivo.get(logsDoArquivo.size() - 1);

            Map<String, Object> novoStatus = new HashMap<>();
            novoStatus.put("fk_servidor", fk_servidor);
            novoStatus.put("apelido", infoServidor.get("apelido"));
            novoStatus.put("ip", infoServidor.get("ip"));
            novoStatus.put("uso_cpu", ultimoLog.getCpu());
            novoStatus.put("uso_ram", ultimoLog.getRam());
            novoStatus.put("uso_disco", ultimoLog.getDisco());
            novoStatus.put("ultimo_update", ultimoLog.getDataHora().format(FORMATTER));

            todosStatus.removeIf(status ->
                    status.get("fk_servidor").toString().equals(fk_servidor.toString())
            );

            todosStatus.add(novoStatus);

            ObjectMapper mapper = new ObjectMapper();
            mapper.writerWithDefaultPrettyPrinter()
                    .writeValue(new File(nomeArquivo), todosStatus);

            aws.uploadCardsServidoresBucket(nomeArquivo);

            System.out.println("Status atualizado com sucesso!");

        } catch (Exception e) {
            System.err.println("Erro ao atualizar status: " + e.getMessage());
            e.printStackTrace();
        }
    }
}