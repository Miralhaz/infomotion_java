package org.example.classesWillian;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.example.AwsConnection; // Importa as classes existentes
import org.example.TrustedParaCliente; // Opcional, para rodar dentro da TrustedParaCliente
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;

public class TratamentoWillian {

    private final AwsConnection awsConnection;
    private static final String NOME_ARQUIVO_CSV = "logs_consolidados_servidores.csv";
    private static final String NOME_ARQUIVO_JSON = "dados_servidores_disco_tratado.json";
    private static final String PASTA_CLIENT = "tratamentos_willian"; // Pasta de destino no S3 Client

    public TratamentoWillian(AwsConnection awsConnection) {
        this.awsConnection = awsConnection;
    }

    /**
     * Ponto de entrada para o tratamento individual de disco/servidor.
     */
    public void executarTratamento() {
        System.out.println("--- Iniciando Tratamento Individual Willian: CSV -> JSON -> Tratamento Disco/Servidor ---");

        try {
            // 1. BAIXAR O ARQUIVO DO TRUSTED
            awsConnection.downloadBucketProcessosTrusted(NOME_ARQUIVO_CSV);
            System.out.println("Arquivo baixado do Trusted: " + NOME_ARQUIVO_CSV);

            // 2. CONVERTER CSV PARA JSON (e já fazer o tratamento)
            Path jsonPath = Paths.get(NOME_ARQUIVO_JSON);
            converterEConsolidarJson(Paths.get(NOME_ARQUIVO_CSV), jsonPath);
            System.out.println("Conversão e tratamento para JSON concluídos: " + NOME_ARQUIVO_JSON);

            // 3. FAZER UPLOAD DO JSON PARA O BUCKET CLIENT
            uploadJsonParaClient(NOME_ARQUIVO_JSON);
            System.out.println("Upload concluído para o Client (JSON): " + NOME_ARQUIVO_JSON);

        } catch (Exception e) {
            System.err.println("Erro FATAL no Tratamento Willian: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Garante que os arquivos locais temporários sejam removidos
            awsConnection.deleteCsvLocal(NOME_ARQUIVO_CSV);
            awsConnection.deleteCsvLocal(NOME_ARQUIVO_JSON);
        }
    }

    /**
     * Converte o CSV em JSON, aplicando o tratamento focado em Disco, Servidor, e I/O.
     * @param csvPath Caminho do CSV local.
     * @param jsonPath Caminho do JSON de saída.
     */
    private void converterEConsolidarJson(Path csvPath, Path jsonPath) throws IOException {
        System.out.println("Iniciando conversão de CSV (delimitador ';') para JSON e tratamento focado...");

        try (Reader in = new FileReader(csvPath.toFile())) {

            // 1. Configuração do CSVFormat: Delimitador ';' e Header/Skip
            Iterable<CSVRecord> records = CSVFormat.DEFAULT
                    .builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setDelimiter(';') // Ponto-e-vírgula
                    .build()
                    .parse(in);

            ObjectMapper mapper = new ObjectMapper();
            ArrayNode jsonArray = mapper.createArrayNode();

            // 2. Colunas de interesse e como tratá-las
            final java.util.Map<String, String> colunasDeInteresse = new java.util.HashMap<>();
            colunasDeInteresse.put("fk_servidor", "INTEGER");
            colunasDeInteresse.put("nomeMaquina", "STRING");
            colunasDeInteresse.put("disco", "DOUBLE");
            colunasDeInteresse.put("temperatura_disco", "DOUBLE");
            colunasDeInteresse.put("quantidade_processos", "INTEGER");
            colunasDeInteresse.put("numero_leituras", "DOUBLE"); // Pode ser grande demais para INT
            colunasDeInteresse.put("numero_escritas", "DOUBLE"); // Pode ser grande demais para INT
            colunasDeInteresse.put("bytes_lidos", "DOUBLE"); // Bytes são grandes
            colunasDeInteresse.put("bytes_escritos", "DOUBLE"); // Bytes são grandes
            colunasDeInteresse.put("tempo_leitura", "INTEGER");
            colunasDeInteresse.put("tempo_escrita", "INTEGER");

            for (CSVRecord record : records) {
                ObjectNode jsonObject = mapper.createObjectNode();

                // Itera sobre as colunas de interesse
                for (java.util.Map.Entry<String, String> entry : colunasDeInteresse.entrySet()) {
                    String header = entry.getKey();
                    String dataType = entry.getValue();

                    // Pega o valor bruto. O Commons CSV garante que o header existe no record.
                    String rawValue = record.get(header);
                    String value = (rawValue != null) ? rawValue.trim() : "";

                    try {
                        switch (dataType) {
                            case "STRING":
                                // Tratamento de Servidor (Ex: Padronização)
                                if (header.equalsIgnoreCase("nomeMaquina")) {
                                    value = value.toUpperCase().replace("-", "_");
                                }
                                jsonObject.put(header, value);
                                break;

                            case "INTEGER":
                                // fk_servidor, tempo_leitura, tempo_escrita
                                jsonObject.put(header, Integer.parseInt(value));
                                break;

                            case "DOUBLE":
                                // Colunas que usam vírgula como separador decimal (disco, temperatura)
                                // OU colunas numéricas grandes (leituras/escritas em bytes)
                                String cleanValue = value.replace(',', '.');
                                double numValue = Double.parseDouble(cleanValue);
                                jsonObject.put(header, numValue);

                                // Exemplo de Tratamento Específico: Alerta de Disco
                                if (header.equalsIgnoreCase("disco")) {
                                    if (numValue > 85.0) {
                                        jsonObject.put("alerta_disco_alto", true);
                                    } else {
                                        jsonObject.put("alerta_disco_alto", false);
                                    }
                                }
                                break;

                            default:
                                jsonObject.put(header, value);
                        }
                    } catch (NumberFormatException ignored) {
                        // Se a conversão falhar (ex: valor nulo ou texto inesperado), coloca N/A
                        jsonObject.put(header, "N/A");
                    }
                }

                jsonArray.add(jsonObject);
            }

            // Escreve o Array JSON em um arquivo
            try (FileWriter file = new FileWriter(jsonPath.toFile())) {
                mapper.writerWithDefaultPrettyPrinter().writeValue(file, jsonArray);
            }

        } catch (IOException e) {
            System.err.println("Erro durante a conversão CSV para JSON ou tratamento: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Faz o upload do arquivo JSON para o S3 Client.
     * @param nomeArq Nome do arquivo JSON local.
     */
    // Em TratamentoWillian.java (Método Corrigido)

    private void uploadJsonParaClient(String nomeArq) {
        String key = String.format("%s/%s", PASTA_CLIENT, nomeArq);
        try {
            // CORREÇÃO: Usamos o getS3Client() e construímos o PutObjectRequest com o Content-Type correto.
            awsConnection.getS3Client().putObject(
                    PutObjectRequest.builder()
                            .bucket("s3-client-infomotion-1")
                            .key(key)
                            .contentType("application/json") // Content-Type correto para JSON
                            .build(),
                    RequestBody.fromFile(Path.of(nomeArq))
            );
        }
        catch (Exception e) {
            System.err.println("Erro ao fazer upload de JSON para CLIENT: " + nomeArq + ": " + e.getMessage());
            throw new RuntimeException("Falha no upload do JSON para Client", e);
        }
    }
}
