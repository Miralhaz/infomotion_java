package org.example.classesRede;

import org.example.Connection;
import org.example.JiraService;
import org.example.Logs;
import org.example.SlackNotifier;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class TratamentoAlertaRede {

    public static List<LogConexao> csvJsonConexao (String nomeArq) {
        Reader arq = null; // objeto aquivo
        BufferedReader entrada = null; // objeto leitor de arquivo
        nomeArq += ".csv";
        List<LogConexao> listaLogsConexao = new ArrayList<>();

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
            System.out.println("Lendo e Importando CSV de dados do bucket-raw");

            linha = entrada.readLine();
            // converte de string para integer
            // caso fosse de string para int usa-se parseint
            while (linha != null) {

                registro = linha.split(";");
                String nomeConexao = registro[1];
                String laddr = registro[2];
                String raddr = registro[3];
                String status = registro[4];
                Integer idProcessoConexao = Integer.valueOf(registro[5]);
                LogConexao logConexao = new LogConexao(nomeConexao, raddr, laddr, status, idProcessoConexao);
                listaLogsConexao.add(logConexao);
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
        return listaLogsConexao;
    }




    public List<LogRede> filtrandoLogRede(List<Logs> lista) {
        List<LogRede> logsRede = new ArrayList<>();

        for (Logs log : lista) {
            LogRede logRede = new LogRede(log.getDataHora(), log.getUpload_bytes(), log.getDownload_bytes(), log.getPacotes_recebidos(), log.getPacotes_enviados(), log.getDropin(), log.getDropout(), log.getFk_servidor());
            logsRede.add(logRede);
        }
        return logsRede;
    }

    public void detectandoAlertasRede(List<Logs> lista, Double max, Integer duracao_min, Integer fk_parametroAlerta, String unidadeMedida) {

        List<LogRede> listaRede = filtrandoLogRede(lista);
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
            String tituloJira = String.format("Alerta Crítico: Download em %d no Servidor %d",
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
            String tituloJira = String.format("Alerta Crítico: Upload em %d no Servidor %d",
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
            String tituloJira = String.format("Alerta Crítico: Pacotes enviados em %d no Servidor %d",
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
            String tituloJira = String.format("Alerta Crítico: Pacotes Recebidos em %d no Servidor %d",
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
            alertasRede.addAll(listaRede);
        }
    }
}
