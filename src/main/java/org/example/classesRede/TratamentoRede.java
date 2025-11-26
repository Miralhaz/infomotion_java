package org.example.classesRede;

import org.example.*;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.example.AwsConnection;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;



public class TratamentoRede {
    private LocalDateTime dataHoje = LocalDateTime.now(ZoneId.of("America/Sao_Paulo"));
    private static AwsConnection awsConnection;
    private static final String nomePasta = "Dashboard_Rede";
    private static JdbcTemplate banco;

    public TratamentoRede(AwsConnection awsConnection, JdbcTemplate banco) {
        this.awsConnection = awsConnection;
        this.banco = banco;
    }

    public AwsConnection getAwsConnection() {
        return awsConnection;
    }

    public static List<LogConexao> csvJsonConexao (Integer idServidor) {


        String nomeArq = "conexoes" + idServidor;


        Reader arq = null; // objeto aquivo
        BufferedReader entrada = null; // objeto leitor de arquivo
        nomeArq += ".csv";
        List<LogConexao> listaLogsConexao = new ArrayList<>();

        awsConnection.downloadBucketRaw(nomeArq);

        try {
            arq = new InputStreamReader(new FileInputStream(nomeArq), StandardCharsets.UTF_8);
            entrada = new BufferedReader(arq);
        } catch (IOException e) {
            System.out.println("Erro ao abrir o arquivo [csvJsonConexao]");
            System.exit(1);
        }

        try {
            String[] registro;
            //readLine é usado para ler uma linha do arquivo
            String linha = entrada.readLine();

            // separa cada da linha usando o delimitador ";"
            // resgistro = linha.split(";");
            // printa os titulos da coluna

            linha = entrada.readLine();
            // converte de string para integer
            // caso fosse de string para int usa-se parseint
            while (linha != null) {

                registro = linha.split(";");
                String nomeProcesso = String.valueOf(registro[1]);
                Integer fk_servidor = Integer.valueOf(registro[2]);
                String dataHoraString = registro[3];
                Integer idProcessoConexao = Integer.valueOf(registro[4]);
                String laddr = registro[7];
                String raddr = registro[8];
                String status = registro[9];
                LogConexao logConexao = new LogConexao(nomeProcesso, fk_servidor, dataHoraString, idProcessoConexao, laddr, raddr, status);
                listaLogsConexao.add(logConexao);
                linha = entrada.readLine();
            }
        }catch (IOException e ){
            System.out.println("Erro ao ler o arquivo");
            e.printStackTrace();
            System.exit(1);

        }finally {
            try {
                entrada.close();
                arq.close();
            } catch (IOException e) {
                System.out.println("Erro ao fechar o arquivo");
            }
        }
        awsConnection.deleteCsvLocal(nomeArq);
        return listaLogsConexao;
    }

    public static List<LogRede> filtrandoLogRede(List<Logs> lista, Integer idServidor, Integer tempoHoras) {

        List<LogRede> logsRede = new ArrayList<>();

        String selectParametroPorServidor = "SELECT * FROM parametro_alerta where fk_servidor = (?);";
        List<Parametro_alerta> metrica = banco.query(selectParametroPorServidor,
                new BeanPropertyRowMapper<>(Parametro_alerta.class),
                1);

        Double parametroDown = 0.0;
        Double parametroUp = 0.0;
        Double parametroPacotesRecebidos = 0.0;
        Double parametroPacotesEnviados = 0.0;

        for (Parametro_alerta pa : metrica){

            if (pa.getUnidadeMedida().equalsIgnoreCase("DOWNLOAD")){
                parametroDown = pa.getMax();
            } else if (pa.getUnidadeMedida().equalsIgnoreCase("UPLOAD")){
                parametroUp = pa.getMax();
            } else if (pa.getUnidadeMedida().equalsIgnoreCase("PCKT_RCVD")){
                parametroPacotesRecebidos = pa.getMax();
            } else if (pa.getUnidadeMedida().equalsIgnoreCase("PCKT_SNT")){
                parametroPacotesEnviados = pa.getMax();
            }

        }


        for (Logs log : lista) {

            String dataString = log.getDataHoraString();
            DateTimeFormatter formatador = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
            LocalDateTime dataFinal = LocalDateTime.parse(dataString, formatador);
            LocalDateTime limite = LocalDateTime.now().minusHours(tempoHoras);


            if (dataFinal.isAfter(limite)) {

                if (log.getFk_servidor().equals(idServidor)) {
                    LogRede logRede = new LogRede(log.getDataHoraString(), log.getUpload_bytes(), log.getDownload_bytes(), log.getPacotes_recebidos(), log.getPacotes_enviados(), log.getDropin(), log.getDropout(), log.getFk_servidor(), parametroDown, parametroUp, parametroPacotesRecebidos, parametroPacotesEnviados);
                    logsRede.add(logRede);
                }
            }
        }
        return logsRede;
    }

    public static void gravaArquivoJson(List<Integer> listaServidores) {

        for (Integer i : listaServidores) {
            List<LogConexao> lista = csvJsonConexao(i);
            String nomeArq = "conexoes" + i;
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
                for (LogConexao log : lista) {
                    contador++;
                    if (contador == lista.size()) {
                        System.out.println("Log que vai virar Json de conexão" + log);
                        saida.write(String.format(Locale.US, """
                                        {
                                        "nome_processo": "%s",
                                        "fk_servidor": "%d",
                                        "timeStamp": "%s",
                                        "idProcessoConexao": "%s",
                                        "laddr": "%s",
                                        "raddr": "%s",
                                        "status": "%s"
                                        }""",
                                log.getNomeConexao() ,log.getFk_servidor(), log.getDataHoraString(), log.getIdProcessoConexao(), log.getLaddr(), log.getRaddr(), log.getStatus()));
                    } else {
                        saida.write(String.format(Locale.US, """
                                        {
                                        "nome_processo": "%s",
                                        "fk_servidor": "%d",
                                        "timeStamp": "%s",
                                        "idProcessoConexao": "%s",
                                        "laddr": "%s",
                                        "raddr": "%s",
                                        "status": "%s"
                                        },""",
                                log.getNomeConexao() ,log.getFk_servidor(), log.getDataHoraString(), log.getIdProcessoConexao(), log.getLaddr(), log.getRaddr(), log.getStatus()));
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

            awsConnection.uploadBucketClient(nomePasta, nomeArq);
            awsConnection.deleteCsvLocal(nomeArq);
        }
    }

    public static void gravaArquivoJsonRede(List<Logs> lista, List<Integer> idServidor) {

        // uma hora, um dia. 7 dias
        int[] listaHoras = {1, 24, 168};

        for (Integer i : idServidor) {
            for (Integer tempoHoras : listaHoras) {

                List<LogRede> listaRede = filtrandoLogRede(lista, i, tempoHoras);
                OutputStreamWriter saida = null;
                Boolean deuRuim = false;

                String nomeArq = "jsonRede_" + i + "_" + tempoHoras;
                nomeArq += ".json";

                try {
                    saida = new OutputStreamWriter(new FileOutputStream(nomeArq), StandardCharsets.UTF_8);
                } catch (IOException erro) {
                    System.out.println("Erro ao abrir o arquivo gravaArquivoJson");
                    System.exit(1);
                }

                try {
                    saida.append("[\n");
                    Integer contador = 0;
                    for (LogRede log : listaRede) {
                        contador++;
                        if (contador == listaRede.size()) {
                            saida.write(String.format(Locale.US, """
                                            {
                                            "fk_servidor": "%d",
                                            "timeStamp": "%s",
                                            "uploadByte": "%d",
                                            "downloadByte": "%d",
                                            "packetSent": "%d",
                                            "packetReceived": "%d",
                                            "packetLossSent": "%d",
                                            "packetLossReceived": "%d",
                                            "parametroDown": "%.0f",
                                            "parametroUp": "%.0f",
                                            "parametroPacotesRecebidos": "%.0f",
                                            "parametroPacotesEnviados": "%.0f"
                                            }""",
                                    log.getFk_servidor(), log.getDataHoraString(), log.getUploadByte(), log.getDownloadByte(), log.getPacketSent(), log.getPacketReceived(), log.getPacketLossSent(), log.getPacketLossReceived(), log.getParametroDown(), log.getParametroUp(), log.getParametroPacotesRecebidos(), log.getParametroPacotesEnviados()));
                        } else {

                            saida.write(String.format(Locale.US, """
                                            {
                                            "fk_servidor": "%d",
                                            "timeStamp": "%s",
                                            "uploadByte": "%d",
                                            "downloadByte": "%d",
                                            "packetSent": "%d",
                                            "packetReceived": "%d",
                                            "packetLossSent": "%d",
                                            "packetLossReceived": "%d",
                                            "parametroDown": "%.0f",
                                            "parametroUp": "%.0f",
                                            "parametroPacotesRecebidos": "%.0f",
                                            "parametroPacotesEnviados": "%.0f"
                                            },""",
                                    log.getFk_servidor(), log.getDataHoraString(), log.getUploadByte(), log.getDownloadByte(), log.getPacketSent(), log.getPacketReceived(), log.getPacketLossSent(), log.getPacketLossReceived(), log.getParametroDown(), log.getParametroUp(), log.getParametroPacotesRecebidos(), log.getParametroPacotesEnviados()));
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
                awsConnection.uploadBucketClient(nomePasta, nomeArq);
                awsConnection.deleteCsvLocal(nomeArq);
            }
        }
    }




    public static void detectandoAlertasRede(List<Logs> lista, Double max, Integer duracao_min, Integer fk_parametroAlerta, String unidadeMedida, Integer idServidor) {

        List<LogRede> listaRede = filtrandoLogRede(lista, idServidor, 1);
        List<LogRede> alertasRede = new ArrayList<>();
        Integer fk_servidor = listaRede.getFirst().getFk_servidor();

        long valorMax = Long.MAX_VALUE;
        Long zero = 0L;

        Long maxDown = zero;
        Long minDown = valorMax;
        Long maxUp = zero;
        Long minUp = valorMax;
        Long maxpacketSent = zero;
        Long minpacketSent = valorMax;
        Long maxpacketReceived = zero;
        Long minpacketReceived = valorMax;

        Integer contadorDownload = 0;
        Integer contadorUpload = 0;
        Integer contadorPacketSent = 0;
        Integer contadorPacketReceived = 0;


        Connection connection = new Connection();
        JdbcTemplate con = new JdbcTemplate(connection.getDataSource());


        for (LogRede log : listaRede) {
            if (log.getPacketLossSent() >= 1 || log.getPacketLossReceived() >= 1) {
                alertasRede.add(log);
            }

            if (unidadeMedida.equalsIgnoreCase("DOWNLOAD")) {
                if (log.getDownloadByte() > max) {
                    contadorDownload++;
                    if (log.getDownloadByte() > maxDown) {
                        maxDown = log.getDownloadByte();
                    } else if (log.getDownloadByte() < minDown) {
                        minDown = log.getDownloadByte();
                    }
                }
            }else if (unidadeMedida.equalsIgnoreCase("UPLOAD")) {
                if (log.getUploadByte() > max) {
                    contadorUpload++;
                    if (log.getUploadByte() > maxUp) {
                        maxUp = log.getUploadByte();
                    } else if (log.getUploadByte() < minDown) {
                        minDown = log.getUploadByte();
                    }
                }
            }else if (unidadeMedida.equalsIgnoreCase("PCKT_RCVD")) {
                if (log.getPacketReceived() > max) {
                    contadorPacketReceived++;
                    if (log.getPacketReceived() > maxpacketReceived) {
                        maxpacketReceived = log.getPacketReceived();
                    } else if (log.getPacketReceived() < minpacketReceived) {
                        minpacketReceived = log.getPacketReceived();
                    }
                }
            }else if (unidadeMedida.equalsIgnoreCase("PCKT_SNT")) {
                if (log.getPacketSent() > max) {
                    contadorUpload++;
                    if (log.getPacketSent() > maxUp) {
                        maxUp = log.getPacketSent();
                    } else if (log.getPacketSent() < minDown) {
                        minDown = log.getPacketSent();
                    }
                }
            }



        }

        String insert = "INSERT INTO alertas (fk_parametro, max, min)\n" +
                "VALUES\n" +
                "((?),(?),(?));";

        if (contadorDownload.equals(duracao_min)) {
            contadorDownload = 0;

            con.update(insert, fk_parametroAlerta, maxDown, minDown);
            Logs ultimoLog = lista.get(lista.size() - 1);
            String tituloJira = String.format("Alerta Crítico: Rede em %d no Servidor %d",
                    maxDown, fk_servidor);
            String descricaoCorpo = String.format(
                    "Alerta de Download.\n" +
                            "Pico: %d\n" +
                            "Duração: %d minutos\n" +
                            "Usuário: %s\n" +
                            "Timestamp: %s",
                    maxDown,
                    duracao_min,
                    ultimoLog.getDataHora().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
            );
            System.out.println("Alerta de valor de Download!");
            try {
                JiraService.createAlertTicket(tituloJira, descricaoCorpo);
            } catch (Exception e) {
                System.err.println("Falha ao criar ticket Jira para Download %: " + e.getMessage());
            }
            String mensagemSlack = "\n ⚠\uFE0F Alerta de valor de Download no servidor: " + fk_servidor +
                    "\nPico do alerta:" + maxDown +
                    "\nMínimo do alerta:" + minDown +
                    "\nDuração do alerta: " + duracao_min + " minutos";
            try {
                SlackNotifier.sendSlackMessage(mensagemSlack);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            alertasRede.addAll(listaRede);
        }
        else if (contadorUpload.equals(duracao_min)) {
            contadorUpload = 0;
            con.update(insert, fk_parametroAlerta, maxUp, minUp);
            Logs ultimoLog = lista.get(lista.size() - 1);
            String tituloJira = String.format("Alerta Crítico: Rede em %d no Servidor %d",
                    maxUp, fk_servidor);
            String descricaoCorpo = String.format(
                    "Alerta de Upload.\n" +
                            "Pico: %d\n" +
                            "Duração: %d minutos\n" +
                            "Usuário: %s\n" +
                            "Timestamp: %s",
                    maxUp,
                    duracao_min,
                    ultimoLog.getDataHora().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
            );
            System.out.println("Alerta de valor de Upload!");
            try {
                JiraService.createAlertTicket(tituloJira, descricaoCorpo);
            } catch (Exception e) {
                System.err.println("Falha ao criar ticket Jira para Upload %: " + e.getMessage());
            }
            String mensagemSlack = "\n ⚠\uFE0F Alerta de valor de Upload no servidor: " + fk_servidor +
                    "\nPico do alerta:" + maxUp +
                    "\nMínimo do alerta:" + minUp +
                    "\nDuração do alerta: " + duracao_min + " minutos";
            try {
                SlackNotifier.sendSlackMessage(mensagemSlack);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            alertasRede.addAll(listaRede);
        }
        else if (contadorPacketSent.equals(duracao_min)) {
            contadorPacketSent = 0;
            con.update(insert, fk_parametroAlerta, maxpacketSent, minpacketSent);
            Logs ultimoLog = lista.get(lista.size() - 1);
            String tituloJira = String.format("Alerta Crítico: Rede em %d no Servidor %d",
                    maxpacketSent, fk_servidor);
            String descricaoCorpo = String.format(
                    "Alerta de Pacotes enviados.\n" +
                            "Pico: %d\n" +
                            "Duração: %d minutos\n" +
                            "Usuário: %s\n" +
                            "Timestamp: %s",
                    maxpacketSent,
                    duracao_min,
                    ultimoLog.getDataHora().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
            );
            System.out.println("Alerta de valor de Pacotes enviados!");
            try {
                JiraService.createAlertTicket(tituloJira, descricaoCorpo);
            } catch (Exception e) {
                System.err.println("Falha ao criar ticket Jira para Pacotes enviados %: " + e.getMessage());
            }
            String mensagemSlack = "\n ⚠\uFE0F Alerta de valor de pacotes enviados no servidor: " + fk_servidor +
                    "\nPico do alerta:" + maxpacketSent +
                    "\nMínimo do alerta:" + minpacketSent +
                    "\nDuração do alerta: " + duracao_min + " minutos";
            try {
                SlackNotifier.sendSlackMessage(mensagemSlack);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            alertasRede.addAll(listaRede);
        }
        else if (contadorPacketReceived.equals(duracao_min)) {
            contadorPacketReceived = 0;
            con.update(insert, fk_parametroAlerta, maxpacketReceived, minpacketReceived);
            Logs ultimoLog = lista.get(lista.size() - 1);
            String tituloJira = String.format("Alerta Crítico: Rede em %d no Servidor %d",
                    maxpacketReceived, fk_servidor);
            String descricaoCorpo = String.format(
                    "Alerta de Pacotes Recebidos.\n" +
                            "Pico: %d\n" +
                            "Duração: %d minutos\n" +
                            "Usuário: %s\n" +
                            "Timestamp: %s",
                    maxpacketReceived,
                    duracao_min,
                    ultimoLog.getDataHora().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
            );
            System.out.println("Alerta de valor de Pacotes Recebidos!");
            try {
                JiraService.createAlertTicket(tituloJira, descricaoCorpo);
            } catch (Exception e) {
                System.err.println("Falha ao criar ticket Jira para Pacotes Recebidos %: " + e.getMessage());
            }
            String mensagemSlack = "\n ⚠\uFE0F Alerta de valor de pacotes recebidos no servidor: " + fk_servidor +
                    "\nPico do alerta:" + maxpacketReceived +
                    "\nMínimo do alerta:" + minpacketReceived +
                    "\nDuração do alerta: " + duracao_min + " minutos";
            try {
                SlackNotifier.sendSlackMessage(mensagemSlack);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        }
    }
}


