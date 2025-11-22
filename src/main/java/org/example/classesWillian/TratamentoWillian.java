package org.example.classesWillian;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.math3.stat.regression.SimpleRegression;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;

import java.time.temporal.ChronoUnit;
import org.apache.commons.csv.CSVRecord;
import org.example.AwsConnection; // Importa as classes existentes
import org.example.Connection;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class TratamentoWillian {

    private final AwsConnection awsConnection;
    private final Connection dbConnection;
    private static final String NOME_ARQUIVO_JSON_FINAL = "dashboard_data.json";
    private static final String NOME_ARQUIVO_CSV = "logs_consolidados_servidores.csv";
    private static final String NOME_ARQUIVO_JSON = "dados_servidores_disco_tratado.json";
    private static final String PASTA_CLIENT = "tratamentos_willian"; // Pasta de destino no S3 Client

    public TratamentoWillian(AwsConnection awsConnection, Connection dbConnection) {
        this.awsConnection = awsConnection;
        this.dbConnection = dbConnection;
    }

    /**
     * Ponto de entrada para o tratamento individual de disco/servidor.
     */
    public void executarTratamento() {
        System.out.println("--- Iniciando Tratamento ETL Dashboard ---");

        try {
            // 1. BAIXAR O ARQUIVO DO TRUSTED
            // Usando downloadBucketProcessosTrusted que assume a chave = nome do arquivo
            awsConnection.downloadBucketProcessosTrusted(NOME_ARQUIVO_CSV);
            System.out.println("Arquivo baixado do Trusted: " + NOME_ARQUIVO_CSV);

            // 2. GERAR TODOS OS DADOS, KPIS E GRÁFICOS (CSV -> POJO -> JSON)
            // Este método contém toda a lógica de tratamento, regressão e agregação.
            gerarDashboardData(); // <--- Chamada para o método principal
            System.out.println("Geração dos dados do Dashboard concluída: " + NOME_ARQUIVO_JSON_FINAL);

        } catch (Exception e) {
            System.err.println("Erro FATAL no Tratamento Willian: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Garante que os arquivos locais temporários sejam removidos
            awsConnection.deleteCsvLocal(NOME_ARQUIVO_CSV);
            awsConnection.deleteCsvLocal(NOME_ARQUIVO_JSON_FINAL); // O JSON também é deletado
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
            colunasDeInteresse.put("timestamp", "STRING");

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

    private List<LogServidor> lerCsvESerializar(Path csvPath) throws IOException {
        List<LogServidor> logs = new ArrayList<>();

        try (Reader in = new FileReader(csvPath.toFile())) {

            Iterable<CSVRecord> records = CSVFormat.DEFAULT
                    .builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setDelimiter(';') // Ponto-e-vírgula
                    .build()
                    .parse(in);

            // Define as colunas que precisam de conversão de vírgula (,) para ponto (.)
            String[] colunasDecimais = {
                    "disco", "temperatura_disco", "numero_leituras", "numero_escritas",
                    "bytes_lidos", "bytes_escritos"
            };

            for (CSVRecord record : records) {
                LogServidor log = new LogServidor();

                try {
                    // Conversão de Tipos
                    log.fk_servidor = Integer.parseInt(record.get("fk_servidor"));
                    log.nomeMaquina = record.get("nomeMaquina").toUpperCase().replace("-", "_");
                    log.quantidade_processos = Integer.parseInt(record.get("quantidade_processos"));
                    log.tempo_leitura = Integer.parseInt(record.get("tempo_leitura"));
                    log.tempo_escrita = Integer.parseInt(record.get("tempo_escrita"));

                    // Tratamento Especial para Timestamp (que já faz o parsing para LocalDateTime)
                    log.setTimestamp(record.get("timestamp"));

                    // Tratamento de Decimais
                    for (String header : colunasDecimais) {
                        String rawValue = record.get(header).replace(',', '.');
                        double valor = Double.parseDouble(rawValue);

                        switch (header) {
                            case "disco": log.disco = valor; break;
                            case "temperatura_disco": log.temperatura_disco = valor; break;
                            case "numero_leituras": log.numero_leituras = valor; break;
                            case "numero_escritas": log.numero_escritas = valor; break;
                            case "bytes_lidos": log.bytes_lidos = valor; break;
                            case "bytes_escritos": log.bytes_escritos = valor; break;
                        }
                    }

                    logs.add(log);

                } catch (Exception e) {
                    // Loga a linha com erro e continua (importante para ETLs robustas)
                    System.err.println("Erro ao serializar log: " + record.toString() + ". Erro: " + e.getMessage());
                    // Ignoramos a linha problemática
                }
            }
        }

        // Ordenar por data/hora para garantir a Regressão Linear correta (do mais antigo para o mais novo)
        logs.sort(Comparator.comparing(LogServidor::getTimestampObj));

        return logs;
    }

    public void gerarDashboardData() throws IOException { // Adicionar throws IOException
        System.out.println("--- Iniciando Geração de Dashboard Data ---");

        // 1. BAIXAR E CONVERTER DADOS
        List<LogServidor> dadosAtuais = lerCsvESerializar(Paths.get(NOME_ARQUIVO_CSV));

        if (dadosAtuais.isEmpty()) {
            System.out.println("Aviso: Nenhum dado atualizado para processar.");
            return;
        }

        // 2. OBTER PARÂMETRO GLOBAL DE REFERÊNCIA
        // Pega o ID do primeiro servidor do CSV para buscar o parâmetro
        int idServidorReferencia = dadosAtuais.get(0).fk_servidor;

        // Busca o parâmetro uma única vez. Se falhar no banco, usa 85.0.
        double limiteGlobal = dbConnection.getParametroLimiteDisco(idServidorReferencia);
        double limiteDisco = limiteGlobal;
        double limiteTemp = limiteGlobal; // Usa o mesmo parâmetro para disco e temperatura

        System.out.printf("Parâmetro Global de Limite Crítico (Servidor %d): %.2f%%\n", idServidorReferencia, limiteGlobal);

        // 3. ESTRUTURA FINAL (ObjectMapper, ObjectNode)
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode dashboardData = mapper.createObjectNode();

        // 4. Implementação das Métricas (Passando o Parâmetro ÚNICO)

        // A. Filtrar Alertas
        List<LogServidor> discosEmAlerta = dadosAtuais.stream()
                .filter(log -> log.disco > limiteDisco || log.temperatura_disco > limiteTemp)
                .collect(Collectors.toList());

        // --- Chamadas dos Métodos de Cálculo (Voltam a usar o 'double' do limite) ---

        // B. KPI 1: Status Geral
        dashboardData.set("kpi_status_geral", calcularKpiStatusGeral(mapper, discosEmAlerta));

        // C. LISTA 1: Disco em alerta de armazenamento (Top 5)
        dashboardData.set("lista_disco_alerta_armazenamento",
                calcularListaAlertaArmazenamento(mapper, dadosAtuais, limiteDisco)); // <--- Passando o double

        // D. LISTA 3: Discos em alerta de alta temperatura
        dashboardData.set("lista_disco_alerta_temperatura",
                calcularListaAlertaTemperatura(mapper, dadosAtuais, limiteTemp)); // <--- Passando o double

        // E. Gráfico de Barras: Criticidade por Servidor (Top 5)
        // Ajustaremos este método para aceitar os dois doubles
        dashboardData.set("grafico_criticidade_servidor",
                calcularGraficoCriticidadeServidor(mapper, dadosAtuais, limiteDisco, limiteTemp)); // <--- Passando os doubles

        // F. Gráfico de Linhas: Previsão de Tendência
        dashboardData.set("grafico_tendencia_espaco",
                calcularGraficoTendencia(mapper, dadosAtuais));

        // ... (restante da escrita e upload)
    }

    // Em TratamentoWillian.java (Adicione no final da classe)

// Métodos auxiliares para cálculo




    // Em TratamentoWillian.java

    private ObjectNode calcularKpiStatusGeral(ObjectMapper mapper, List<LogServidor> alertas) {
        ObjectNode node = mapper.createObjectNode();
        if (!alertas.isEmpty()) {
            node.put("status", "Discos Instáveis");
            node.put("cor", "VERMELHO");
            node.put("totalAlertas", alertas.size());
        } else {
            node.put("status", "Operando Normalmente");
            node.put("cor", "VERDE");
            node.put("totalAlertas", 0);
        }
        return node;
    }

    private ObjectNode calcularKpiAlertasHoje(ObjectMapper mapper, int totalAlertasHoje, int totalAlertasOntem) {
        // Lógica será implementada, por enquanto, apenas um stub
        return mapper.createObjectNode().put("totalAlertas", totalAlertasHoje);
    }

    // Em TratamentoWillian.java

    private ArrayNode calcularListaAlertaArmazenamento(ObjectMapper mapper, List<LogServidor> dadosAtuais, double limiteDisco) {

        // 1. Classificar pelo percentual de disco em ordem decrescente
        List<LogServidor> topDiscos = dadosAtuais.stream()
                .sorted(Comparator.comparingDouble(LogServidor::getDisco).reversed())
                .limit(5)
                .collect(Collectors.toList());

        ArrayNode array = mapper.createArrayNode();

        for (LogServidor log : topDiscos) {
            ObjectNode item = mapper.createObjectNode();

            item.put("nomeServidor", log.nomeMaquina);
            item.put("usoDisco", log.disco);

            // 2. Classificação de Risco
            String risco;
            if (log.disco > limiteDisco) {
                risco = "ALTO RISCO"; // Será Vermelho no Front
            } else if (log.disco >= limiteDisco * 0.9) { // 90% do parâmetro
                risco = "MODERADO"; // Será Amarelo no Front
            } else {
                risco = "NORMAL";
            }

            item.put("risco", risco);
            array.add(item);
        }

        return array;
    }

    private ArrayNode calcularListaMaisRequisicoes(ObjectMapper mapper, List<LogServidor> dadosAtuais) {

        // 1. Calculamos a Média Global (para definir o threshold "razoável")
        // Usamos o último registro de cada máquina para o cálculo da média
        List<LogServidor> ultimosLogs = dadosAtuais.stream()
                .collect(Collectors.groupingBy(log -> log.nomeMaquina))
                .values().stream()
                .map(list -> list.stream()
                        .max(Comparator.comparing(LogServidor::getTimestampObj))
                        .orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        double mediaBytesLidos = ultimosLogs.stream().mapToDouble(LogServidor::getBytes_lidos).average().orElse(0.0);
        double mediaBytesEscritos = ultimosLogs.stream().mapToDouble(LogServidor::getBytes_escritos).average().orElse(0.0);

        // Threshold definido como 150% da média para ser "acima de um valor razoável"
        final double THRESHOLD_LEITURA = mediaBytesLidos * 1.5;
        final double THRESHOLD_ESCRITA = mediaBytesEscritos * 1.5;

        // 2. Classificar o Top 3 (relevância operacional)
        List<LogServidor> topRequisicoes = ultimosLogs.stream()
                .sorted(Comparator.comparingDouble((LogServidor log) ->
                        // Fator de Relevância: Bytes lidos + Bytes escritos + Processos (ponderados, por exemplo, por 10)
                        log.bytes_lidos + log.bytes_escritos + (log.quantidade_processos * 10.0)
                ).reversed())
                .limit(3)
                .collect(Collectors.toList());

        ArrayNode array = mapper.createArrayNode();

        for (LogServidor log : topRequisicoes) {
            ObjectNode item = mapper.createObjectNode();

            item.put("nomeServidor", log.nomeMaquina);
            item.put("bytesLidos", log.bytes_lidos);
            item.put("bytesEscritos", log.bytes_escritos);
            item.put("qtdProcessos", log.quantidade_processos);

            // 3. Classificação de Risco (Importante: Azul)
            String classificacao = "NORMAL";
            if (log.bytes_lidos > THRESHOLD_LEITURA || log.bytes_escritos > THRESHOLD_ESCRITA) {
                classificacao = "IMPORTANTE";
            }

            item.put("classificacao", classificacao);
            array.add(item);
        }

        return array;
    }


    private ArrayNode calcularGraficoCriticidadeServidor(ObjectMapper mapper, List<LogServidor> dadosAtuais, double limiteDisco, double limiteTemp) {

        // 1. Agrupar os logs por servidor e calcular a taxa de criticidade para cada um
        List<ServidorCriticidade> criticidades = dadosAtuais.stream()
                .collect(Collectors.groupingBy(log -> log.nomeMaquina)) // Agrupa por servidor
                .entrySet().stream()
                .map(entry -> {
                    String nomeMaquina = entry.getKey();
                    List<LogServidor> historico = entry.getValue();

                    long totalLogs = historico.size();
                    // Conta quantos logs estão em estado de alerta (disco OU temperatura)
                    long totalAlertas = historico.stream()
                            .filter(log -> log.disco > limiteDisco || log.temperatura_disco > limiteTemp)
                            .count();

                    double mediaAlerta = (totalLogs > 0) ? (double) totalAlertas / totalLogs * 100.0 : 0.0;

                    return new ServidorCriticidade(nomeMaquina, mediaAlerta);
                })
                .collect(Collectors.toList());

        // 2. Calcular a Média Geral de Alertas (para toda a ETL)
        double mediaGeralAlertas = criticidades.stream()
                .mapToDouble(sc -> sc.mediaAlerta)
                .average()
                .orElse(0.0);

        // 3. Top 5 e Ordenação (mesmo que não haja alerta, ordena por criticidade)
        List<ServidorCriticidade> top5Criticidade = criticidades.stream()
                .sorted(Comparator.comparingDouble(ServidorCriticidade::getMediaAlerta).reversed())
                .limit(5)
                .collect(Collectors.toList());

        ArrayNode array = mapper.createArrayNode();

        // 4. Montar o JSON, aplicando a classificação (próximo ou acima da média geral)
        for (ServidorCriticidade sc : top5Criticidade) {
            ObjectNode item = mapper.createObjectNode();

            item.put("nomeServidor", sc.nomeMaquina);
            item.put("mediaAlertaPercentual", sc.mediaAlerta);

            String classificacao;
            // Se a média do servidor for 10% acima da média geral, é classificado como Acima
            if (sc.mediaAlerta > mediaGeralAlertas * 1.1) {
                classificacao = "ACIMA DA MÉDIA CRÍTICA";
            }
            // Se estiver entre a média geral e 10% acima (ou perto, mas abaixo)
            else if (sc.mediaAlerta >= mediaGeralAlertas * 0.9) {
                classificacao = "PRÓXIMO DA MÉDIA";
            } else {
                classificacao = "NORMAL";
            }

            // Se a média for 0, mas ele estiver no top 5 (apenas para exibição)
            if (sc.mediaAlerta == 0.0) {
                classificacao = "NORMAL_ORDENADO";
            }

            item.put("classificacao", classificacao);
            item.put("mediaGeralReferencia", mediaGeralAlertas);
            array.add(item);
        }

        return array;
    }



    // Classe Auxiliar (Pode ser definida dentro de TratamentoWillian ou em um arquivo separado)
    private static class ServidorCriticidade {
        String nomeMaquina;
        double mediaAlerta;

        public ServidorCriticidade(String nomeMaquina, double mediaAlerta) {
            this.nomeMaquina = nomeMaquina;
            this.mediaAlerta = mediaAlerta;
        }

        public double getMediaAlerta() {
            return mediaAlerta;
        }
    }

    private ArrayNode calcularListaAlertaTemperatura(ObjectMapper mapper, List<LogServidor> dadosAtuais, double limiteTemp) {

        ArrayNode array = mapper.createArrayNode();

        // 1. Agrupar os dados por nome da máquina (servidor)
        Map<String, List<LogServidor>> logsPorServidor = dadosAtuais.stream()
                .filter(log -> log.getTimestampObj() != null)
                .collect(Collectors.groupingBy(log -> log.nomeMaquina));

        // 2. Analisar cada histórico de servidor
        for (Map.Entry<String, List<LogServidor>> entry : logsPorServidor.entrySet()) {
            String nomeMaquina = entry.getKey();
            List<LogServidor> historico = entry.getValue();

            // Ordenar o histórico para análise cronológica (se não estiver ordenado)
            historico.sort(Comparator.comparing(LogServidor::getTimestampObj));

            if (historico.isEmpty()) continue;

            LogServidor ultimoLog = historico.get(historico.size() - 1);
            String classificacao = "NORMAL";
            LocalDateTime inicioPeriodoAcima = null;

            // 3. Iterar de trás para frente para encontrar o período crítico mais recente
            for (int i = historico.size() - 1; i >= 0; i--) {
                LogServidor log = historico.get(i);

                if (log.temperatura_disco > limiteTemp) {
                    // Se a temperatura está alta, marcamos este como o potencial "início" do período
                    inicioPeriodoAcima = log.getTimestampObj();

                } else if (inicioPeriodoAcima != null) {
                    // Se a temperatura abaixou, calculamos a duração do período que estava alta
                    long duracaoMinutos = ChronoUnit.MINUTES.between(log.getTimestampObj(), ultimoLog.getTimestampObj());

                    // Classificação
                    if (duracaoMinutos > 3) {
                        classificacao = "CRÍTICO";
                    } else {
                        classificacao = "OSCILAÇÃO MOMENTÂNEA";
                    }
                    break; // Encontramos a classificação e saímos do loop
                }

                // Caso chegamos ao primeiro registro (i=0) e a temperatura ainda está alta:
                if (i == 0 && inicioPeriodoAcima != null) {
                    long duracaoMinutos = ChronoUnit.MINUTES.between(inicioPeriodoAcima, ultimoLog.getTimestampObj());
                    if (duracaoMinutos > 3) {
                        classificacao = "CRÍTICO";
                    } else {
                        classificacao = "OSCILAÇÃO MOMENTÂNEA";
                    }
                    break;
                }
            }

            // Se a classificação não for NORMAL, adicionamos à lista
            if (!classificacao.equals("NORMAL")) {
                ObjectNode item = mapper.createObjectNode();
                item.put("nomeServidor", nomeMaquina);
                item.put("temperaturaAtual", ultimoLog.temperatura_disco);
                item.put("limiteParametro", limiteTemp);
                item.put("classificacao", classificacao);
                array.add(item);
            }
        }

        return array;
    }

    private ArrayNode calcularGraficoTendencia(ObjectMapper mapper, List<LogServidor> dadosAtuais) {

        ArrayNode tendenciaArray = mapper.createArrayNode();

        if (dadosAtuais.isEmpty()) {
            return tendenciaArray;
        }

        // 1. Agrupar os dados por nome da máquina (servidor)
        Map<String, List<LogServidor>> logsPorServidor = dadosAtuais.stream()
                // Filtra logs que têm um timestamp válido, essencial para a regressão
                .filter(log -> log.getTimestampObj() != null)
                .collect(Collectors.groupingBy(log -> log.nomeMaquina));

        // 2. Identificar os Top 5 Discos para Análise
        // Usamos o *último* registro de cada máquina para determinar o Top 5 de maior uso atual.
        List<String> top5Discos = logsPorServidor.entrySet().stream()
                .map(entry -> entry.getValue().stream()
                        .max(Comparator.comparing(LogServidor::getTimestampObj))
                        .orElse(null))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingDouble(LogServidor::getDisco).reversed())
                .limit(5)
                .map(log -> log.nomeMaquina)
                .collect(Collectors.toList());

        // 3. Executar Regressão para cada Top 5 Disco
        for (String nomeMaquina : top5Discos) {
            List<LogServidor> historico = logsPorServidor.get(nomeMaquina);

            // Se o histórico tiver menos de 2 pontos, a regressão não pode ser feita
            if (historico.size() < 2) {
                System.out.println("Aviso: Histórico insuficiente para regressão em " + nomeMaquina);
                continue;
            }

            // Ponto de Referência (Tempo 0): O registro mais antigo
            final LogServidor logInicial = historico.get(0);
            SimpleRegression regression = new SimpleRegression();

            // Popular os dados da Regressão
            for (LogServidor log : historico) {
                // X: Tempo em horas desde o primeiro registro
                double x_horas = (double) ChronoUnit.HOURS.between(logInicial.getTimestampObj(), log.getTimestampObj());
                // Y: Uso de Disco
                double y_uso_disco = log.disco;

                regression.addData(x_horas, y_uso_disco);
            }

            // Se a regressão foi bem-sucedida, calculamos as projeções
            if (regression.getN() > 0) {

                // Variável para a projeção: Horas entre o ÚLTIMO log e o futuro
                LogServidor logMaisRecente = historico.get(historico.size() - 1);
                double horasDesdeInicio = (double) ChronoUnit.HOURS.between(logInicial.getTimestampObj(), logMaisRecente.getTimestampObj());

                // Projeções futuras (X = horas a partir do registro inicial)
                double projecao1h = regression.predict(horasDesdeInicio + 1.0);       // +1 hora
                double projecao24h = regression.predict(horasDesdeInicio + 24.0);     // +24 horas
                double projecao3d = regression.predict(horasDesdeInicio + (3.0 * 24)); // +3 dias (72 horas)

                // Criar o Objeto JSON de Saída
                ObjectNode tendencia = mapper.createObjectNode();
                tendencia.put("nomeServidor", nomeMaquina);
                tendencia.put("usoAtual", logMaisRecente.disco);
                tendencia.put("tendenciaCrescimento", regression.getSlope() * 24.0); // Tendência de mudança em 24h
                tendencia.put("projecao1h", Math.max(0.0, Math.min(100.0, projecao1h))); // Limita a [0, 100]
                tendencia.put("projecao24h", Math.max(0.0, Math.min(100.0, projecao24h)));
                tendencia.put("projecao3d", Math.max(0.0, Math.min(100.0, projecao3d)));

                // Classificação da Tendência
                if (regression.getSlope() > 0.05) { // Se a inclinação for positiva (crescimento), por exemplo, > 0.05% por hora
                    tendencia.put("classificacao", "CRESCIMENTO");
                } else if (regression.getSlope() < -0.05) {
                    tendencia.put("classificacao", "DECLÍNIO");
                } else {
                    tendencia.put("classificacao", "ESTÁVEL");
                }

                tendenciaArray.add(tendencia);
            }
        }

        return tendenciaArray;
    }
// Fim dos métodos auxiliares



}
