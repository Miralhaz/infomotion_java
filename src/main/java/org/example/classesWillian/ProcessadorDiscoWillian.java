package org.example.classesWillian;

import org.example.AwsConnection;
import org.example.Connection;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProcessadorDiscoWillian {
    private final AwsConnection awsConnection;
    private final Connection dbConnection;
    private static final DateTimeFormatter CSV_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public ProcessadorDiscoWillian(AwsConnection awsConnection, Connection dbConnection) {
        this.awsConnection = awsConnection;
        this.dbConnection = dbConnection;
    }

    public void executarTratamento() {
        String nomeArquivoCsv = "logs_consolidados_servidores.csv";

        // 1. Baixa o CSV do S3 Trusted para o disco local
        awsConnection.downloadBucketTrusted(nomeArquivoCsv);

        // 2. Agora abre o arquivo local como InputStream
        try (InputStream csvStream = new FileInputStream(nomeArquivoCsv)) {
            // 3. Lê e processa normalmente
            List<RegistroDisco> todosRegistros = lerCsvDisco(csvStream);
            System.out.println("Registros de disco lidos: " + todosRegistros.size());

            completarFkEmpresa(todosRegistros);
            List<Integer> empresasUnicas = pegarEmpresasUnicas(todosRegistros);

            for (int i = 0; i < empresasUnicas.size(); i++) {
                Integer idEmpresa = empresasUnicas.get(i);
                List<RegistroDisco> registrosDaEmpresa = new ArrayList<>();

                for (int j = 0; j < todosRegistros.size(); j++) {
                    RegistroDisco r = todosRegistros.get(j);
                    if (r.fk_empresa != null && r.fk_empresa.equals(idEmpresa)) {
                        registrosDaEmpresa.add(r);
                    }
                }

                String nomeArquivoJson = "DiscoTratamentoEmpresa_" + idEmpresa + ".json";
                gerarJson(registrosDaEmpresa, nomeArquivoJson);
                awsConnection.uploadBucketClient("tratamentos_willian", nomeArquivoJson);
                System.out.println("JSON gerado e enviado: " + nomeArquivoJson);
            }



        } catch (IOException e) {
            System.err.println("Erro ao abrir o arquivo CSV baixado: " + e.getMessage());
            return;
        }

        System.out.println("Tratamento concluído com sucesso!");
    }


    public List<RegistroDisco> lerCsvDisco(InputStream csvStream) {
        List<RegistroDisco> registros = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(csvStream, StandardCharsets.UTF_8))) {
            String linha;
            boolean primeira = true;
            while ((linha = reader.readLine()) != null) {
                if (primeira) {
                    primeira = false;
                    continue; // pula cabeçalho
                }
                String[] colunas = linha.split(";");
                if (colunas.length < 22) continue;

                RegistroDisco r = new RegistroDisco();
                r.fk_servidor = Integer.valueOf(colunas[0].trim());
                r.nomeMaquina = colunas[1].trim();
                r.timestamp = LocalDateTime.parse(colunas[2].trim(), CSV_DATE_FORMAT);
                r.disco = parseDoubleComVirgula(colunas[5].trim());
                r.temperatura_disco = parseDoubleComVirgula(colunas[7].trim());
                r.quantidade_processos = Integer.valueOf(colunas[9].trim());
                r.numero_leituras = parseDoubleComVirgula(colunas[16].trim());
                r.numero_escritas = parseDoubleComVirgula(colunas[17].trim());
                r.bytes_lidos = parseDoubleComVirgula(colunas[18].trim());
                r.bytes_escritos = parseDoubleComVirgula(colunas[19].trim());
                r.tempo_leitura = parseDoubleComVirgula(colunas[20].trim());
                r.tempo_escrita = parseDoubleComVirgula(colunas[21].trim());

                registros.add(r);
                // Dentro do while, após pular o cabeçalho:
                if (registros.size() < 2) {
                    System.out.println("Linha lida: " + linha);
                    System.out.println("Número de colunas: " + colunas.length);
                    if (colunas.length > 5) System.out.println("Exemplo coluna 5 (disco): '" + colunas[5] + "'");
                }
            }

            return registros;
        } catch (Exception e) {
            System.err.println("Erro ao ler CSV: " + e.getMessage());
            return registros;
        }
    }

    public Double parseDoubleComVirgula(String valor) {
        if (valor == null || valor.isEmpty()) return 0.0;
        try {
            return Double.parseDouble(valor.replace(',', '.'));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    public void completarFkEmpresa(List<RegistroDisco> registros) {
        // 1. Pegar todos os servidores únicos
        List<Integer> servidoresUnicos = new ArrayList<>();
        for (int i = 0; i < registros.size(); i++) {
            Integer servidor = registros.get(i).fk_servidor;
            if (servidor != null) {
                boolean jaExiste = false;
                for (int j = 0; j < servidoresUnicos.size(); j++) {
                    if (servidoresUnicos.get(j).equals(servidor)) {
                        jaExiste = true;
                        break;
                    }
                }
                if (!jaExiste) {
                    servidoresUnicos.add(servidor);
                }
            }
        }

        // 2. Para cada servidor único, buscar sua empresa e guardar em duas listas paralelas
        List<Integer> listaServidores = new ArrayList<>();
        List<Integer> listaEmpresas = new ArrayList<>();

        for (int i = 0; i < servidoresUnicos.size(); i++) {
            Integer servidor = servidoresUnicos.get(i);
            Integer empresa = buscarFkEmpresaPorServidor(servidor);
            listaServidores.add(servidor);
            listaEmpresas.add(empresa); // pode ser -1 se não encontrar
        }

        // 3. Preencher fk_empresa em cada registro com base nas listas paralelas
        for (int i = 0; i < registros.size(); i++) {
            RegistroDisco r = registros.get(i);
            if (r.fk_servidor != null) {
                Integer empresaEncontrada = -1;
                for (int j = 0; j < listaServidores.size(); j++) {
                    if (listaServidores.get(j).equals(r.fk_servidor)) {
                        empresaEncontrada = listaEmpresas.get(j);
                        break;
                    }
                }
                r.fk_empresa = empresaEncontrada;
            } else {
                r.fk_empresa = -1;
            }
        }
    }

    private Integer buscarFkEmpresaPorServidor(Integer fkServidor) {
        java.sql.Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = dbConnection.getDataSource().getConnection();
            String sql = "SELECT e.id FROM empresa e INNER JOIN servidor s ON s.fk_empresa = e.id WHERE s.id = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, fkServidor);
            rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("id");
            } else {
                System.out.println("Aviso: Servidor " + fkServidor + " sem empresa.");
                return -1;
            }
        } catch (Exception e) {
            System.err.println("Erro no banco para servidor " + fkServidor + ": " + e.getMessage());
            return -1;
        } finally {
            try { if (rs != null) rs.close(); } catch (Exception ignored) {}
            try { if (stmt != null) stmt.close(); } catch (Exception ignored) {}
            try { if (conn != null) conn.close(); } catch (Exception ignored) {}
        }
    }

    public List<Integer> pegarEmpresasUnicas(List<RegistroDisco> registros) {
        List<Integer> empresas = new ArrayList<>();
        for (int i = 0; i < registros.size(); i++) {
            RegistroDisco r = registros.get(i);
            if (r.fk_empresa != null && r.fk_empresa != -1) {
                boolean jaTem = false;
                for (int j = 0; j < empresas.size(); j++) {
                    if (empresas.get(j).equals(r.fk_empresa)) {
                        jaTem = true;
                        break;
                    }
                }
                if (!jaTem) {
                    empresas.add(r.fk_empresa);
                }
            }
        }
        return empresas;
    }

    public void gerarJson(List<RegistroDisco> registros, String nomeArquivo) {
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(nomeArquivo), StandardCharsets.UTF_8)) {
            writer.write("[\n");
            for (int i = 0; i < registros.size(); i++) {
                RegistroDisco r = registros.get(i);
                String timestampIso = r.timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

                writer.write(String.format(Locale.US,
                        "  {\n" +
                                "    \"fk_servidor\": %d,\n" +
                                "    \"nomeMaquina\": \"%s\",\n" +
                                "    \"timestamp\": \"%s\",\n" +
                                "    \"disco\": %.2f,\n" +
                                "    \"temperatura_disco\": %.2f,\n" +
                                "    \"quantidade_processos\": %d,\n" +
                                "    \"numero_leituras\": %.0f,\n" +
                                "    \"numero_escritas\": %.0f,\n" +
                                "    \"bytes_lidos\": %.0f,\n" +
                                "    \"bytes_escritos\": %.0f,\n" +
                                "    \"tempo_leitura\": %.0f,\n" +
                                "    \"tempo_escrita\": %.0f\n" +
                                "  }",
                        r.fk_servidor,
                        r.nomeMaquina.replace("\"", "\\\""),
                        timestampIso,
                        r.disco,
                        r.temperatura_disco,
                        r.quantidade_processos,
                        r.numero_leituras,
                        r.numero_escritas,
                        r.bytes_lidos,
                        r.bytes_escritos,
                        r.tempo_leitura,
                        r.tempo_escrita
                ));

                if (i < registros.size() - 1) {
                    writer.write(",\n");
                } else {
                    writer.write("\n");
                }
            }
            writer.write("]\n");
        } catch (IOException e) {
            System.err.println("Erro ao gerar JSON " + nomeArquivo + ": " + e.getMessage());
        }
    }
}