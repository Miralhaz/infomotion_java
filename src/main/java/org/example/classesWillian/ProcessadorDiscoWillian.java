package org.example.classesWillian;

import org.example.AwsConnection;
import org.example.Connection;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProcessadorDiscoWillian {
    public final AwsConnection awsConnection;
    public final Connection dbConnection;
    public static final DateTimeFormatter CSV_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public ProcessadorDiscoWillian(AwsConnection awsConnection, Connection dbConnection) {
        this.awsConnection = awsConnection;
        this.dbConnection = dbConnection;
    }

    // inicio de tratamento e ordenacao

    public List<RegistroDisco> pegarUltimoRegistroPorServidor(List<RegistroDisco> registros) {
        List<RegistroDisco> ultimos = new ArrayList<>();

        for (int i = 0; i < registros.size(); i++) {
            RegistroDisco atual = registros.get(i);
            boolean jaAdicionado = false;

            // Verifica se já temos esse servidor na lista de "últimos"
            for (int j = 0; j < ultimos.size(); j++) {
                RegistroDisco existente = ultimos.get(j);
                if (existente.fk_servidor.equals(atual.fk_servidor)) {
                    jaAdicionado = true;
                    // Se o atual for mais recente, substitui
                    if (atual.timestamp.isAfter(existente.timestamp)) {
                        ultimos.set(j, atual);
                    }
                    break;
                }
            }

            if (!jaAdicionado) {
                ultimos.add(atual);
            }
        }

        return ultimos;
    }

    private String buscarApelidoDisco(Integer fkServidor) {
        java.sql.Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = dbConnection.getDataSource().getConnection();

            // Sua query para pegar o apelido do disco
            String sql =
                    "SELECT c.apelido " +
                            "FROM componentes c " +
                            "WHERE c.fk_servidor = ? AND c.tipo = 'DISCO'";

            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, fkServidor);
            rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getString("apelido");
            }
        } catch (Exception e) {
            System.err.println("Erro ao buscar apelido do disco para servidor " + fkServidor);
        }

        return "Disco-" + fkServidor; // fallback
    }


    public void executarTratamento() {
        try {
            System.out.println("inicio do meu tratamento de disco (Willian)...");

            String nomeArquivoCsv = "logs_consolidados_servidores.csv";
            awsConnection.downloadBucketTrusted(nomeArquivoCsv);
            System.out.println("baixando csv do bucket trusted.");

            try (InputStream csvStream = new FileInputStream(nomeArquivoCsv)) {
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

                    // Processa últimos registros

                    List<Integer> idsServidores = new ArrayList<>();
                    List<ParametroAlerta> parametrosLista = new ArrayList<>();
                    List<RegistroDisco> servidoresAtuais = pegarUltimoRegistroPorServidor(registrosDaEmpresa);

                    for (int j = 0; j < servidoresAtuais.size(); j++) {
                        RegistroDisco r = servidoresAtuais.get(j);
                        if (r.fk_servidor == null) continue;
                        idsServidores.add(r.fk_servidor);
                        ParametroAlerta p = buscarParametroPorServidor(r.fk_servidor);
                        parametrosLista.add(p);
                        if (r.fk_servidor != null) {
                            r.apelidoDisco = buscarApelidoDisco(r.fk_servidor);
                            r.capacidade = buscarCapacidadeDisco(r.fk_servidor);
                        }
                    }

                    // Gera JSON principal (últimos registros + parâmetros)
                    String nomeJsonAtual = "DiscoTratamentoEmpresa_" + idEmpresa + ".json";
                    gerarJsonComParametros(servidoresAtuais, idsServidores, parametrosLista, nomeJsonAtual);
                    awsConnection.uploadBucketClient("tratamento_willian", nomeJsonAtual);
                    System.out.println("Upload concluído para CLIENT: " + nomeJsonAtual);

                    // 👇 NOVO: Gera JSON de HISTÓRICO (todos os registros)
                    String nomeJsonHistorico = "DiscoHistoricoEmpresa_" + idEmpresa + ".json";
                    gerarJsonHistorico(registrosDaEmpresa, nomeJsonHistorico);
                    awsConnection.uploadBucketClient("tratamento_willian", nomeJsonHistorico);
                    System.out.println("Upload concluído para CLIENT: " + nomeJsonHistorico);
                }

                System.out.println("✅ Tratamento de disco concluído com sucesso!");
            }
        } catch (Exception e) {
            System.err.println("💥 ERRO FATAL no tratamento de disco: " + e.getMessage());
            e.printStackTrace(); // ← ISSO É IMPORTANTE!
        }

        System.out.println("Tratamento concluído com sucesso!");
    }

    public void gerarJsonHistorico(List<RegistroDisco> registros, String nomeArquivo) {
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(nomeArquivo), StandardCharsets.UTF_8)) {
            writer.write("[\n");
            for (int i = 0; i < registros.size(); i++) {
                RegistroDisco r = registros.get(i);
                writer.write(String.format(Locale.US,
                        "  {\"fk_servidor\":%d,\"timestamp\":\"%s\",\"disco\":%.2f,\"temperatura_disco\":%.2f}",
                        r.fk_servidor,
                        r.timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        r.disco,
                        r.temperatura_disco
                ));
                if (i < registros.size() - 1) writer.write(",");
                writer.write("\n");
            }
            writer.write("]\n");
        } catch (IOException e) {
            System.err.println("Erro ao gerar histórico: " + e.getMessage());
        }
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

    public Integer buscarFkEmpresaPorServidor(Integer fkServidor) {
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


    public void gerarJsonComParametros(
            List<RegistroDisco> registros,
            List<Integer> idsServidores,
            List<ParametroAlerta> parametrosLista,
            String nomeArquivo) {

        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(nomeArquivo), StandardCharsets.UTF_8)) {

            // Início do JSON como objeto
            writer.write("{\n");

            // 1. Parâmetros de alerta
            writer.write("  \"parametrosAlerta\": {\n");
            for (int i = 0; i < idsServidores.size(); i++) {
                if (i >= parametrosLista.size()) {
                    System.err.println("⚠️ Aviso: parâmetro faltando para servidor na posição " + i);
                    continue;
                }
                Integer id = idsServidores.get(i);
                ParametroAlerta p = parametrosLista.get(i);
                writer.write(String.format( Locale.US,
                        "    \"%d\": { \"limiteDisco\": %.2f, \"limiteTemperatura\": %.2f }",
                        id,
                        p.limiteDisco,
                        p.limiteTemperatura
                ));
                if (i < idsServidores.size() - 1) writer.write(",");
                writer.write("\n");
            }
            writer.write("  },\n");

            // 2. Lista de servidores (seu código antigo, mas dentro de "servidores")
            writer.write("  \"servidores\": [\n");
            for (int i = 0; i < registros.size(); i++) {
                RegistroDisco r = registros.get(i);
                String timestampIso = r.timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

                writer.write(String.format(Locale.US,
                        "    {\n" +
                                "      \"fk_servidor\": %d,\n" +
                                "      \"nomeMaquina\": \"%s\",\n" +
                                "      \"timestamp\": \"%s\",\n" +
                                "      \"disco\": %.2f,\n" +
                                "      \"temperatura_disco\": %.2f,\n" +
                                "      \"quantidade_processos\": %d,\n" +
                                "      \"numero_leituras\": %.0f,\n" +
                                "      \"numero_escritas\": %.0f,\n" +
                                "      \"bytes_lidos\": %.0f,\n" +
                                "      \"bytes_escritos\": %.0f,\n" +
                                "      \"tempo_leitura\": %.0f,\n" +
                                "      \"tempo_escrita\": %.0f,\n" +
                                "      \"apelidoDisco\": \"%s\",\n" +
                                "      \"capacidade\": \"%s\"\n" +
                                "    }",
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
                        r.tempo_escrita,
                        r.apelidoDisco,
                        r.capacidade
                ));

                if (i < registros.size() - 1) writer.write(",");
                else writer.write("\n");
            }
            writer.write("  ]\n");
            writer.write("}\n");

        } catch (IOException e) {
            System.err.println("Erro ao gerar JSON com parâmetros: " + e.getMessage());
        }
    }

    // buscando parametro no banco, para tratar
    public class ParametroAlerta {
        public Double limiteDisco;
        public Double limiteTemperatura;
        public ParametroAlerta(Double disco, Double temp) {
            this.limiteDisco = disco;
            this.limiteTemperatura = temp;
        }
    }


    public ParametroAlerta buscarParametroPorServidor(Integer fkServidor) {
        Double limiteDisco = 90.0;   // valor padrão
        Double limiteTemperatura = 45.0; // valor padrão

        java.sql.Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = dbConnection.getDataSource().getConnection();

            // 1. Busca limite para DISCO (%)
            String sqlDisco = "SELECT p.max FROM parametro_alerta p " +
                    "INNER JOIN servidor s ON s.id = p.fk_servidor " +
                    "INNER JOIN componentes c ON c.fk_servidor = s.id " +
                    "WHERE p.fk_servidor = ? AND c.tipo = 'DISCO' AND p.unidade_medida = '%'";
            stmt = conn.prepareStatement(sqlDisco);
            stmt.setInt(1, fkServidor);
            rs = stmt.executeQuery();
            if (rs.next()) {
                limiteDisco = rs.getDouble("max");
            }
            rs.close();
            stmt.close();

            // 2. Busca limite de temp
            String sqlTemp = "SELECT p.max FROM parametro_alerta p " +
                    "INNER JOIN servidor s ON s.id = p.fk_servidor " +
                    "INNER JOIN componentes c ON c.fk_servidor = s.id " +
                    "WHERE p.fk_servidor = ? AND c.tipo = 'DISCO' AND p.unidade_medida = 'C'";
            stmt = conn.prepareStatement(sqlTemp);
            stmt.setInt(1, fkServidor);
            rs = stmt.executeQuery();
            if (rs.next()) {
                limiteTemperatura = rs.getDouble("max");
            }

        } catch (Exception e) {
            System.err.println("Erro ao buscar parâmetros para servidor " + fkServidor + ": " + e.getMessage());
        } finally {
            try { if (rs != null) rs.close(); } catch (Exception ignored) {}
            try { if (stmt != null) stmt.close(); } catch (Exception ignored) {}
            try { if (conn != null) conn.close(); } catch (Exception ignored) {}
        }

        return new ParametroAlerta(limiteDisco, limiteTemperatura);
    }

    private String buscarCapacidadeDisco(Integer fkServidor) {
        java.sql.Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = dbConnection.getDataSource().getConnection();

            String sql =
                    "SELECT e.valor " +
                            "FROM especificacao_componente e " +
                            "INNER JOIN componentes c ON c.id = e.fk_componente " +
                            "INNER JOIN servidor s ON s.id = c.fk_servidor " +
                            "WHERE s.id = ? AND c.tipo = 'DISCO' AND e.nome_especificacao = 'Capacidade total disco (GB)'";

            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, fkServidor);
            rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getString("valor");
            }
            return "Desconhecido";

        } catch (Exception e) {
            System.err.println("Erro ao buscar capacidade: " + e.getMessage());
            return "Desconhecido";
        } finally {
            try { if (rs != null) rs.close(); } catch (Exception ignored) {}
            try { if (stmt != null) stmt.close(); } catch (Exception ignored) {}
            try { if (conn != null) conn.close(); } catch (Exception ignored) {}
        }
    }

}