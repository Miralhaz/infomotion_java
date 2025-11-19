package org.example;

import org.example.classesMiralha.TratamentoTemperaturaCpu;
import org.example.classesMiralha.TratamentoTemperaturaDisco;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import software.amazon.awssdk.services.s3.endpoints.internal.Value;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.*;


public class Main {

    static AwsConnection aws = new AwsConnection();

    public static void gravaArquivoCsv(List<Logs> lista, String nomeArq){
        OutputStreamWriter saida = null;
        Boolean deuRuim = false;
        nomeArq += ".csv";

        try {
            saida = new OutputStreamWriter(new FileOutputStream(nomeArq), StandardCharsets.UTF_8);

        }catch (IOException erro){
            System.out.println("Erro ao abrir o arquivo gravaArquivoCsv");
            System.exit(1);
        }

        try {
            saida.append("fk_servidor;nomeMaquina;timestamp;cpu;ram;disco;temperatura_cpu;temperatura_disco;memoria_swap;quantidade_processos;download_bytes;upload_bytes;pacotes_recebidos;pacotes_enviados;dropin;dropout;numero_leituras;numero_escritas;bytes_lidos;bytes_escritos;tempo_leitura;tempo_escrita\n");

            for (Logs log : lista){
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
                saida.write(String.format("%d;%s;%s;%.2f;%.2f;%.2f;%.2f;%.2f;%.2f;%d;%d;%d;%d;%d;%d;%d;%d;%d;%d;%d;%d;%d\n",
                        log.getFk_servidor(), log.getNomeMaquina(), log.getDataHora().format(formatter), log.getCpu(),log.getRam(),log.getDisco(),log.getTmp_cpu(),log.getTmp_disco(),log.getMemoria_swap(),log.getQtd_processos(), log.getDownload_bytes(), log.getUpload_bytes(), log.getPacotes_recebidos(), log.getPacotes_enviados(), log.getDropin(), log.getDropout(), log.getNumero_leituras(), log.getNumero_escritas(), log.getBytes_lidos(), log.getBytes_escritos(), log.getTempo_leitura(), log.getTempo_escrita()));
            }

        }catch (IOException erro){
            System.out.println("Erro ao gravar o arquivo");
            erro.printStackTrace();
            deuRuim = true;
        }finally {
            try {
                saida.close();
            } catch (IOException erro) {
                System.out.println("Erro ao fechar o arquivo");
                deuRuim = true;
            }
            if (deuRuim){
                System.exit(1);
            }
        }
    }
    public static List<Logs> lerJson() {
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream("data.json");
        } catch (FileNotFoundException erro) {
            System.out.println("Arquivo não encontrado");
            System.exit(1);
        }

        LogMapper logMapper = new LogMapper();
        List<Logs> listaLog = new ArrayList<>();
        try {
            listaLog = logMapper.mapearLogs(inputStream);

        } catch (IOException erro) {
            System.out.println("Erro ao mapear o json lerJson");
            erro.printStackTrace();
        } finally {
            try {

                inputStream.close();
            } catch (IOException erro) {
                System.out.println("Erro ao fechar o arquivo json");

            }

        }
        return listaLog;
    }


    public static void gravaArquivoJson(List<Logs> lista, String nomeArq) {

        OutputStreamWriter saida = null;
        Boolean deuRuim = false;
        nomeArq += ".json";

        try {
            saida = new OutputStreamWriter(new FileOutputStream(nomeArq), StandardCharsets.UTF_8);

        } catch (IOException erro) {
            System.out.println("Erro ao abrir o arquivo gravaArquivoJson");
            System.exit(1);
        }

        try {
            saida.append("[");
            Integer contador = 0;
            for (Logs log : lista) {
                contador ++;
                if (contador == lista.size()){
                    saida.write(String.format(Locale.US,"""
                           {
                           "fk_servidor": "%d",
                           "nomeMaquina": "%s",
                           "timestamp": "%s",
                           "cpu": "%.2f",
                           "ram": "%.2f",
                           "disco": "%.2f",
                           "temperatura_cpu": "%.2f",
                           "temperatura_disco": "%.2f",
                           "memoria_swap": "%.2f",
                           "quantidade_processos": "%d",
                           "download_bytes": "%d",
                           "upload_bytes" : "%d",
                           "pacotes_recebidos": "%d",
                           "pacotes_enviados": "%d",
                           "dropin": "%d",
                           "dropout": "%d",
                           "numero_leituras": "%d",
                           "numero_escritas": "%d",
                           "bytes_lidos": "%d",
                           "bytes_escritos": "%d",
                           "tempo_leitura": "%d",
                           "tempo_escrita": "%d"
                           }""",
                            log.getFk_servidor(), log.getNomeMaquina(), log.getDataHoraString(),log.getCpu(),log.getRam(),log.getDisco(),log.getTmp_cpu(),log.getTmp_disco(),log.getMemoria_swap(),log.getQtd_processos(), log.getDownload_bytes(), log.getUpload_bytes(), log.getPacotes_recebidos(), log.getPacotes_enviados(), log.getDropin(), log.getDropout(), log.getNumero_leituras(), log.getNumero_escritas(), log.getBytes_lidos(), log.getBytes_escritos(), log.getTempo_leitura(), log.getTempo_escrita()));
                }else {
                    saida.write(String.format(Locale.US,"""
                               {
                               "fk_servidor": "%d",
                               "nomeMaquina": "%s",
                               "timestamp": "%s",
                               "cpu": "%.2f",
                               "ram": "%.2f",
                               "disco": "%.2f",
                               "temperatura_cpu": "%.2f",
                               "temperatura_disco": "%.2f",
                               "memoria_swap": "%.2f",
                               "quantidade_processos": "%d",
                               "download_bytes": "%d",
                               "upload_bytes" : "%d",
                               "pacotes_recebidos": "%d",
                               "pacotes_enviados": "%d",
                               "dropin": "%d",
                               "dropout": "%d",
                               "numero_leituras": "%d",
                               "numero_escritas": "%d",
                               "bytes_lidos": "%d",
                               "bytes_escritos": "%d",
                               "tempo_leitura": "%d",
                               "tempo_escrita": "%d"
                               },""",
                            log.getFk_servidor(), log.getNomeMaquina(), log.getDataHoraString(),log.getCpu(),log.getRam(),log.getDisco(),log.getTmp_cpu(),log.getTmp_disco(),log.getMemoria_swap(),log.getQtd_processos(), log.getDownload_bytes(), log.getUpload_bytes(), log.getPacotes_recebidos(), log.getPacotes_enviados(), log.getDropin(), log.getDropout(), log.getNumero_leituras(), log.getNumero_escritas(), log.getBytes_lidos(), log.getBytes_escritos(), log.getTempo_leitura(), log.getTempo_escrita()));
                }
            }
            saida.append("]");
        } catch (IOException erro) {
            System.out.println("Erro ao gravar o arquivo");
            erro.printStackTrace();
            deuRuim = true;
        } finally {
            try {
                saida.close();
            } catch (IOException erro) {
                System.out.println("Erro ao fechar o arquivo");
                deuRuim = true;
            }
            if (deuRuim) {
                System.exit(1);
            }
        }
    }
    public static  List<Logs> leImportaArquivoCsv(String nomeArq){
        Reader arq = null; // objeto aquivo
        BufferedReader entrada = null; // objeto leitor de arquivo
        nomeArq += ".csv";
        List<Logs> listaLogs = new ArrayList<>();

        try {
            arq = new InputStreamReader(new FileInputStream(nomeArq), StandardCharsets.UTF_8);
            entrada = new BufferedReader(arq);
        } catch (IOException e) {
            System.out.println("Erro ao abrir o arquivo leImportaArquivoCsv");
            System.exit(1);
        }

        try {
            String[] registro;
            //readLine é usado para ler uma linha do arquivo
            String linha = entrada.readLine();

            // separa cada da linha usando o delimitador ";"
            // resgistro = linha.split(";");
            // printa os titulos da coluna
             System.out.println("Lendo e Importando CSV de dados do bucket-raw");

            linha = entrada.readLine();
            // converte de string para integer
            // caso fosse de string para int usa-se parseint
            while (linha != null) {

                registro = linha.split(";");
                Integer fk_servidor = Integer.valueOf(registro[1]);
                String nomeMaquina = registro[2];
                String dataHoraString = registro[3];
                Double cpu = Double.valueOf(registro[4]);
                Double ram = Double.valueOf(registro[5]);
                Double disco = Double.valueOf(registro[6]);
                Double tmp_cpu = Double.valueOf(registro[7]);
                Double tmp_disco = Double.valueOf(registro[8]);
                Double memoria_swap = Double.valueOf(registro[9]);
                Integer qtd_processos = Integer.valueOf(registro[10]);
                Integer download_bytes = Integer.valueOf(registro[11]);
                Integer upload_bytes = Integer.valueOf(registro[12]);
                Integer pacotes_recebidos = Integer.valueOf(registro[13]);
                Integer pacotes_enviados = Integer.valueOf(registro[14]);
                Integer dropin = Integer.valueOf(registro[15]);
                Integer dropout = Integer.valueOf(registro[16]);
                Integer numero_leituras = Integer.valueOf(registro[17]);
                Integer numero_escritas = Integer.valueOf(registro[18]);
                Long bytes_lidos = Long.valueOf(registro[19]);
                Long bytes_escritos = Long.valueOf(registro[20]);
                Integer tempo_leitura = Integer.valueOf(registro[21]);
                Integer tempo_escrita = Integer.valueOf(registro[22]);
                Logs Log = new Logs(fk_servidor, nomeMaquina, dataHoraString, cpu, ram, disco, tmp_cpu, tmp_disco, memoria_swap, qtd_processos, download_bytes, upload_bytes, pacotes_recebidos, pacotes_enviados, dropin, dropout, numero_leituras, numero_escritas, bytes_lidos, bytes_escritos, tempo_leitura, tempo_escrita);
                listaLogs.add(Log);
                linha = entrada.readLine();
            }
        }catch (IOException e ){
            System.out.println("Erro ao ler o arquivo");
            e.printStackTrace();
            System.exit(1);

        }finally {
            try {
                System.out.println("Arquivo importado com sucesso!\n");
                entrada.close();
                arq.close();
            } catch (IOException e) {
                System.out.println("Erro ao fechar o arquivo");
            }
        }
      return listaLogs;
    }


    public static void main(String[] args) throws Exception {
        aws.downloadBucket("data.csv");
        List<Logs> lista = leImportaArquivoCsv("data");
        gravaArquivoJson(lista,"data");
        List<Logs> listaLogs = lerJson(); // Transforma o obj em json

        System.out.println("Estabelecendo conexão ao banco...");
        Connection connection = new Connection();
        JdbcTemplate con = new JdbcTemplate(connection.getDataSource());
        System.out.println("Conexão estabelecida com sucesso!\n");


        // Área de tratamento MIRALHA
        TratamentoTemperaturaCpu tratarTemperatura = new TratamentoTemperaturaCpu(aws, con);
        tratarTemperatura.tratamentoDeTemperaturaCpu(listaLogs, "temperaturaUsoCpu");

        TratamentoTemperaturaDisco tratarTemperaturaDisco = new TratamentoTemperaturaDisco(aws, con);
        tratarTemperaturaDisco.tratamentoDeTemperaturaDisco(listaLogs, "temperaturaUsoDisco");


        // Instânciando a lista de alertas
        List<Logs> listaAlertas = new ArrayList<>();
        // Pegando o id do servidor
         Integer fk_servidor = listaLogs.get(1).getFk_servidor();

        String selectParametroPorServidor = "SELECT * FROM parametro_alerta where fk_servidor = (?);";
        List<Parametro_alerta> metrica = con.query(selectParametroPorServidor,
                new BeanPropertyRowMapper<>(Parametro_alerta.class),
                fk_servidor);


        Boolean existeAlertaDisco = false;
        for (int i = 0; i < metrica.size(); i++) {
            System.out.printf("ETL em processamento: %d/%d\n", i + 1, metrica.size());
            Integer contador = 1;

            Integer contadorCPUPorcentagem = 0;
            Integer contadorCPUTemperatura = 0;
            Integer contadorRamPorcentagem = 0;
            Integer contadorDiscoTemperatura = 0;
            Integer contadorSwap = 0;

            for (int j = 0; j < listaLogs.size() - contador; j++) { // For das métricas por servidor

                Integer fk_componente = metrica.get(i).getFk_componente();
                Double max = metrica.get(i).getMax();
                Integer duracao_min = metrica.get(i).getDuracao_min(); // Define a quantidade de capturas em seguida que devem ser maior que o parametro para ser considerado um alerta
                Integer fk_parametroAlerta = metrica.get(i).getId();

                Double maxCPUPorcentagem = 0.0;
                Double minCPUPorcentagem = 100.0;

                Double maxCPUTemperatura = 0.0;
                Double minCPUTemperatura = 100.0;

                Double maxRamPorcentagem = 0.0;
                Double minRamPorcentagem = 100.0;

                Double maxDiscoPorcentagem = 0.0;
                Double minDiscoPorcentagem = 100.0;

                Double maxDiscoTemperatura = 0.0;
                Double minDiscoTemperatura = 100.0;

                Double maxSwap = 0.0;
                Double minSwap = 99999999.9;


                List<Logs> miniLista = new ArrayList<>();
                for (int k = 1; k <= duracao_min; k++) { // for para criar a mini-lista
                    try {
                        miniLista.add(listaLogs.get(contador));
                        contador++;
                    } catch (IndexOutOfBoundsException erro) {
                    }
                }


                for (int l = 0; l < miniLista.size(); l++) { // For de análise da mini-lista

                    String selectTipo = (
                            "select tipo from parametro_alerta pa\n" +
                                    "inner join componentes c on c.id = pa.fk_componente where fk_componente = (?);"
                    );
                    String tipo = con.queryForObject(selectTipo, String.class, fk_componente);

                    String selectUnidadeMedida = (
                            "select unidade_medida from parametro_alerta pa\n" +
                                    "inner join componentes c on c.id = pa.fk_componente where fk_componente = (?);"
                    );
                    String unidadeMedida = con.queryForObject(selectUnidadeMedida, String.class, fk_componente);

                    Logs logAtual = miniLista.get(l);

                    if (tipo.equalsIgnoreCase("CPU")) {
                        if (unidadeMedida.equalsIgnoreCase("%")) {
                            if (logAtual.getCpu() > max) {
                                contadorCPUPorcentagem++;
                                if (logAtual.getCpu() > maxCPUPorcentagem) {
                                    maxCPUPorcentagem = logAtual.getCpu();
                                } else if (logAtual.getCpu() < minCPUPorcentagem) {
                                    minCPUPorcentagem = logAtual.getCpu();
                                }
                            }
                        } else {
                            if (logAtual.getTmp_cpu() > max) {
                                contadorCPUTemperatura++;

                                if (logAtual.getTmp_cpu() > maxCPUTemperatura) {
                                    maxCPUTemperatura = logAtual.getTmp_cpu();
                                } else if (logAtual.getTmp_cpu() < minCPUTemperatura) {
                                    minCPUTemperatura = logAtual.getTmp_cpu();
                                }
                            }
                        }
                    } else if (tipo.equalsIgnoreCase("RAM")) {
                        if (logAtual.getRam() > max) {
                            contadorRamPorcentagem++;
                            if (logAtual.getRam() > maxRamPorcentagem) {
                                maxRamPorcentagem = logAtual.getRam();
                            } else if (logAtual.getRam() < minRamPorcentagem) {
                                minRamPorcentagem = logAtual.getRam();
                            }
                        }
                    } else if (tipo.equalsIgnoreCase("DISCO")) {
                        if (unidadeMedida.equalsIgnoreCase("%")) {
                            if (logAtual.getDisco() > max) {
                                existeAlertaDisco = true;
                            }
                            maxDiscoPorcentagem = logAtual.getDisco();
                            minDiscoPorcentagem = logAtual.getDisco();
                        } else {
                            if (logAtual.getTmp_disco() > max) {
                                contadorDiscoTemperatura++;
                                if (logAtual.getTmp_disco() > maxDiscoTemperatura) {
                                    maxDiscoTemperatura = logAtual.getTmp_disco();
                                } else if (logAtual.getTmp_disco() < minDiscoTemperatura) {
                                    minDiscoTemperatura = logAtual.getTmp_disco();
                                }
                            }
                        }
                    } else if (tipo.equalsIgnoreCase("SWAP")) {
                        if (logAtual.getMemoria_swap() > max) {
                            contadorSwap++;
                            if (logAtual.getMemoria_swap() > maxSwap) {
                                maxSwap = logAtual.getMemoria_swap();
                            } else if (logAtual.getMemoria_swap() < minSwap) {
                                minSwap = logAtual.getMemoria_swap();
                            }
                        }
                    }

                    if (contadorCPUPorcentagem.equals(duracao_min)) {
                        contadorCPUPorcentagem = 0;
                        String insertCPUPorcentagem = "INSERT INTO alertas (fk_parametro, max, min)\n" +
                                "VALUES\n" +
                                "((?),(?),(?));";
                        con.update(insertCPUPorcentagem, fk_parametroAlerta, maxCPUPorcentagem, minCPUPorcentagem);
                        Logs ultimoLog = miniLista.get(miniLista.size() - 1);
                        String tituloJira = String.format("Alerta Crítico: CPU em %.2f%% no Servidor %d",
                                maxCPUPorcentagem, fk_servidor);
                        String descricaoCorpo = String.format(
                                "Alerta de CPU.\n" +
                                        "Pico: %.2f%%\n" +
                                        "Duração: %d minutos\n" +
                                        "Usuário: %s\n" +
                                        "Timestamp: %s",
                                maxCPUPorcentagem,
                                duracao_min,
                                ultimoLog.getNomeMaquina(),
                                ultimoLog.getDataHora().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
                        );
                        System.out.println("Alerta de Uso de CPU!");
                        try {
                            JiraService.createAlertTicket(tituloJira, descricaoCorpo);
                        } catch (Exception e) {
                            System.err.println("Falha ao criar ticket Jira para CPU %: " + e.getMessage());
                        }
                        String mensagemSlack = "\n ⚠\uFE0F Alerta de uso da CPU no servidor: " + fk_servidor +
                                "\nPico do alerta:" + maxCPUPorcentagem +
                                "\nMínimo do alerta:" + minCPUPorcentagem +
                                "\nDuração do alerta: " + duracao_min + " minutos";
                        SlackNotifier.sendSlackMessage(mensagemSlack);
                        listaAlertas.addAll(miniLista);
                    } else if (contadorCPUTemperatura.equals(duracao_min)) {
                        String insertCPUTemperatura = "INSERT INTO alertas (fk_parametro, max, min)\n" +
                                "VALUES\n" +
                                "((?),(?),(?));";
                        con.update(insertCPUTemperatura, fk_parametroAlerta, maxCPUTemperatura, minCPUTemperatura);
                        Logs ultimoLog = miniLista.get(miniLista.size() - 1); // Pega o último log

                        String tituloJira = String.format("Alerta Crítico: Temp. CPU em %.2fºC no Servidor %d",
                                maxCPUTemperatura, fk_servidor);
                        String descricaoCorpo = String.format(
                                "Alerta de Temperatura da CPU.\n" +
                                        "Pico: %.2fºC\n" +
                                        "Duração: %d minutos\n" +
                                        "Usuário: %s\n" +
                                        "Data e hora do alerta: %s",
                                maxCPUTemperatura,
                                duracao_min,
                                ultimoLog.getNomeMaquina(),
                                ultimoLog.getDataHora().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
                        );
                        System.out.println("\nAlerta de Temperatura de CPU!");
                        try {
                            JiraService.createAlertTicket(tituloJira, descricaoCorpo);
                        } catch (Exception e) {
                            System.err.println("Falha ao criar ticket Jira para Temp. CPU: " + e.getMessage());
                        }
                        String mensagemSlack = "\n ⚠\uFE0F Alerta de temperatura da CPU no servidor: " + fk_servidor +
                                "\nPico do alerta:" + maxCPUTemperatura +
                                "\nMínimo do alerta:" + minCPUTemperatura +
                                "\nDuração do alerta: " + duracao_min + " minutos";
                        SlackNotifier.sendSlackMessage(mensagemSlack);
                        listaAlertas.addAll(miniLista);
                    } else if (contadorDiscoTemperatura.equals(duracao_min)) {
                        String insertDiscoTemperatura = "INSERT INTO alertas (fk_parametro, max, min)\n" +
                                "VALUES\n" +
                                "((?),(?),(?));";
                        con.update(insertDiscoTemperatura, fk_parametroAlerta, maxDiscoTemperatura, minDiscoTemperatura);
                        Logs ultimoLog = miniLista.get(miniLista.size() - 1); // Pega o último log
                        // --- LÓGICA JIRA: TEMPERATURA DISCO ---
                        String tituloJira = String.format("Alerta Crítico: Temp. Disco em %.2fºC no Servidor %d",
                                maxDiscoTemperatura, fk_servidor);
                        String descricaoCorpo = String.format(
                                "Alerta de Temperatura do Disco.\n" +
                                        "Pico: %.2fºC\n" +
                                        "Duração: %d minutos\n" +
                                        "Usuário: %s\n" +
                                        "Data e hora do alerta: %s",
                                maxDiscoTemperatura,
                                duracao_min,
                                ultimoLog.getNomeMaquina(),
                                ultimoLog.getDataHora().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
                        );
                        System.out.println("\nAlerta de Temperatura de Disco!");
                        try {
                            JiraService.createAlertTicket(tituloJira, descricaoCorpo);
                        } catch (Exception e) {
                            System.err.println(" Falha ao criar ticket Jira para Temp. Disco: " + e.getMessage());
                        }
                        String mensagemSlack = "\n ⚠\uFE0F Alerta de temperatura do Disco no servidor: " + fk_servidor +
                                "\nPico do alerta:" + maxDiscoTemperatura +
                                "\nMínimo do alerta:" + minDiscoTemperatura +
                                "\nDuração do alerta: " + duracao_min + " minutos";
                        SlackNotifier.sendSlackMessage(mensagemSlack);
                        listaAlertas.addAll(miniLista);
                    } else if (existeAlertaDisco) {
                        String insertDisco = "INSERT INTO alertas (fk_parametro, max, min)\n" +
                                "VALUES\n" +
                                "((?),(?),(?));";
                        con.update(insertDisco, fk_parametroAlerta, maxDiscoPorcentagem, minDiscoPorcentagem);
                        Logs ultimoLog = miniLista.get(miniLista.size() - 1);

                        String tituloJira = String.format("Alerta Crítico: Disco em %.2f%% no Servidor %d",
                                maxDiscoPorcentagem, fk_servidor);
                        String descricaoCorpo = String.format(
                                "Alerta de Uso do Disco.\n" +
                                        "Pico: %.2f%%\n" +
                                        "Duração: %d minutos\n" +
                                        "Usuário: %s\n" +
                                        "Data e hora do alerta: %s",
                                maxDiscoPorcentagem,
                                duracao_min,
                                ultimoLog.getNomeMaquina(),
                                ultimoLog.getDataHora().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
                        );
                        System.out.println("\nAlerta de Uso de Disco!");
                        try {
                            JiraService.createAlertTicket(tituloJira, descricaoCorpo);
                        } catch (Exception e) {
                            System.err.println(" Falha ao criar ticket Jira para Uso do Disco: " + e.getMessage());
                        }
                        String mensagemSlack = "\n ⚠\uFE0F Alerta de uso do Disco no servidor: " + fk_servidor +
                                "\nPico do alerta:" + maxDiscoPorcentagem +
                                "\nMínimo do alerta:" + minDiscoPorcentagem +
                                "\nDuração do alerta: " + duracao_min + " minutos";
                        SlackNotifier.sendSlackMessage(mensagemSlack);
                        listaAlertas.addAll(miniLista);
                    } else if (contadorRamPorcentagem.equals(duracao_min)) {
                        String insertRamPorcentagem = "INSERT INTO alertas (fk_parametro, max, min)\n" +
                                "VALUES\n" +
                                "((?),(?),(?));";
                        con.update(insertRamPorcentagem, fk_parametroAlerta, maxRamPorcentagem, minRamPorcentagem);
                        Logs ultimoLog = miniLista.get(miniLista.size() - 1);

                        String tituloJira = String.format("Alerta Crítico: RAM em %.2f%% no Servidor %d",
                                maxRamPorcentagem, fk_servidor);
                        String descricaoCorpo = String.format(
                                "Alerta de Uso da RAM.\n" +
                                        "Pico: %.2f%%\n" +
                                        "Duração: %d minutos\n" +
                                        "Usuário: %s\n" +
                                        "Data e hora do alerta: %s",
                                maxRamPorcentagem,
                                duracao_min,
                                ultimoLog.getNomeMaquina(),
                                ultimoLog.getDataHora().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
                        );
                        System.out.println("\nAlerta de Uso de RAM!");
                        try {
                            JiraService.createAlertTicket(tituloJira, descricaoCorpo);
                        } catch (Exception e) {
                            System.err.println(" Falha ao criar ticket Jira para RAM %: " + e.getMessage());
                        }
                        String mensagemSlack = "\n ⚠\uFE0F Alerta de uso da RAM no servidor: " + fk_servidor +
                                "\nPico do alerta:" + maxRamPorcentagem +
                                "\nMínimo do alerta:" + minRamPorcentagem +
                                "\nDuração do alerta: " + duracao_min + " minutos";
                        SlackNotifier.sendSlackMessage(mensagemSlack);
                        listaAlertas.addAll(miniLista);
                    } else if (contadorSwap.equals(duracao_min)) {
                        String insertSwap = "INSERT INTO alertas (fk_parametro, max, min)\n" +
                                "VALUES\n" +
                                "((?),(?),(?));";
                        con.update(insertSwap, fk_parametroAlerta, maxSwap, minSwap);
                        Logs ultimoLog = miniLista.get(miniLista.size() - 1);

                        String tituloJira = String.format("Alerta Crítico: SWAP em %.2f%% no Servidor %d",
                                maxSwap, fk_servidor);
                        String descricaoCorpo = String.format(
                                "Alerta de Uso da Swap.\n" +
                                        "Pico: %.2f%%\n" +
                                        "Duração: %d minutos\n" +
                                        "Usuário: %s\n" +
                                        "Data e hora do alerta: %s",
                                maxSwap,
                                duracao_min,
                                ultimoLog.getNomeMaquina(),
                                ultimoLog.getDataHora().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
                        );
                        System.out.println("\nAlerta de Uso de SWAP!");
                        try {
                            JiraService.createAlertTicket(tituloJira, descricaoCorpo);
                        } catch (Exception e) {
                            System.err.println(" Falha ao criar ticket Jira para SWAP: " + e.getMessage());
                        }
                        String mensagemSlack = "\n ⚠\uFE0F Alerta de uso do Swap no servidor: " + fk_servidor +
                                "\nPico do alerta:" + maxSwap +
                                "\nMínimo do alerta:" + minSwap +
                                "\nDuração do alerta: " + duracao_min + " minutos";
                        SlackNotifier.sendSlackMessage(mensagemSlack);
                        listaAlertas.addAll(miniLista);
                    }
                }


            }
            gravaArquivoCsv(listaAlertas, "alertas");
            aws.uploadBucket("alertas.csv");
        }
        aws.limparTemporarios();


        System.out.println("\nChamando a ETL do Trusted para Client...");

        try {
            // 2. Instancia a nova ETL
            TrustedParaCliente Etlcliente = new TrustedParaCliente(aws);

            // 3. Executa a ETL para o arquivo de teste
            Etlcliente.rodarProcesso("alertas.csv");

        } catch (Exception e) {
            System.err.println("Ocorreu um erro no Main: " + e.getMessage());
            // O tratamento de erro detalhado já está dentro do runEtl
        } finally {
            // Chamada para limpeza geral, se for o caso
            // awsConnection.limparTemporarios(); registro_alerta-1.0-SNAPSHOT.jar
        }

        System.out.println("\n--- Processo Principal Finalizado ---");
    }

}
