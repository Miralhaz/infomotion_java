package org.example.classesRede;

import org.example.*;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.example.AwsConnection;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;



public class TratamentoRede {
    private LocalDateTime dataHoje = LocalDateTime.now(ZoneId.of("America/Sao_Paulo"));

    private static final String nomePasta = "Dashboard_Rede";


    public static List<LogConexao> csvJsonConexao (Integer idServidor, AwsConnection awsConnection) {


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

    public static List<LogRede> filtrandoLogRede(List<Logs> lista, Integer idServidor, Integer tempoHoras, JdbcTemplate banco) {

        List<LogRede> logsRede = new ArrayList<>();

        String selectParametroPorServidor = "SELECT * FROM parametro_alerta where fk_servidor = (?);";
        List<Parametro_alerta> metrica = banco.query(selectParametroPorServidor,
                new BeanPropertyRowMapper<>(Parametro_alerta.class),
                1);

        Double parametroDown = 0.0;
        Double parametroUp = 0.0;
        Double parametroPacotesRecebidos = 0.0;
        Double parametroPacotesEnviados = 0.0;
        LocalDateTime ultimoLogData;
        Long ultimoValorPcktRcvd = 0L;
        Long ultimoValorPcktSnt = 0L;
        Long ultimoValorDown = 0L;
        Long ultimoValorUp = 0L;
        Double valorDiferenca = 1.01;

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

        ultimoLogData = lista.get(0).getDataHora();
        ultimoValorPcktRcvd = lista.get(0).getPacotes_recebidos();
        ultimoValorPcktSnt = lista.get(0).getPacotes_enviados();
        ultimoValorDown = lista.get(0).getDownload_bytes();
        ultimoValorUp = lista.get(0).getUpload_bytes();



        for (Logs log : lista) {

            Boolean vaiPraLista = false;
            String dataString = log.getDataHoraString();
            DateTimeFormatter formatador;
            if (dataString.contains("/")) {
                formatador = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
            } else {
                formatador = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            }
            LocalDateTime dataFinal = LocalDateTime.parse(dataString, formatador);
            LocalDateTime limite = LocalDateTime.now().minusHours(tempoHoras);

            if (dataFinal.isAfter(limite)) {
                DateTimeFormatter formatadorSaida;
                if (tempoHoras == 1 ){
                    if (dataString.contains("/")) {
                        formatadorSaida = DateTimeFormatter.ofPattern("HH:mm");
                    } else {
                        formatadorSaida = DateTimeFormatter.ofPattern("HH:mm");
                    }
                    if (dataFinal.isAfter(ultimoLogData.minusMinutes(12)) || log.getPacotes_enviados() > ultimoValorPcktSnt * valorDiferenca || log.getPacotes_recebidos() > ultimoValorPcktRcvd  * valorDiferenca
                            || log.getDownload_bytes() > ultimoValorDown * valorDiferenca || log.getUpload_bytes() > ultimoValorUp * valorDiferenca) {
                        vaiPraLista = true;
                    }

                } else if (tempoHoras == 24) {
                    if (dataString.contains("/")) {
                        formatadorSaida = DateTimeFormatter.ofPattern("dd HH:mm");
                    } else {
                        formatadorSaida = DateTimeFormatter.ofPattern("dd HH:mm");
                    }

                    if (dataFinal.isAfter(ultimoLogData.minusMinutes(1440)) || log.getPacotes_enviados() > ultimoValorPcktSnt * valorDiferenca || log.getPacotes_recebidos() > ultimoValorPcktRcvd  * valorDiferenca
                            || log.getDownload_bytes() > ultimoValorDown * valorDiferenca || log.getUpload_bytes() > ultimoValorUp * valorDiferenca) {
                        vaiPraLista = true;
                    }

                } else {
                    if (dataString.contains("/")) {
                        formatadorSaida = DateTimeFormatter.ofPattern("dd/MM HH");
                    } else {
                        formatadorSaida = DateTimeFormatter.ofPattern("MM-dd HH");
                    }

                    if (dataFinal.isAfter(ultimoLogData.minusMinutes(10080)) || log.getPacotes_enviados() > ultimoValorPcktSnt * valorDiferenca || log.getPacotes_recebidos() > ultimoValorPcktRcvd  * valorDiferenca
                            || log.getDownload_bytes() > ultimoValorDown * valorDiferenca || log.getUpload_bytes() > ultimoValorUp * valorDiferenca) {
                        vaiPraLista = true;
                    }
                }

                String dataFormatada = dataFinal.format(formatadorSaida);



                if (log.getFk_servidor().equals(idServidor) &&  vaiPraLista) {
                    LogRede logRede = new LogRede(dataFormatada, log.getUpload_bytes(), log.getDownload_bytes(), log.getPacotes_recebidos(), log.getPacotes_enviados(), log.getDropin(), log.getDropout(), log.getFk_servidor(), parametroDown, parametroUp, parametroPacotesRecebidos, parametroPacotesEnviados);
                    logsRede.add(logRede);
                }
            }
        }
        return logsRede;
    }

    public static void gravaArquivoJson(List<Integer> listaServidores, AwsConnection awsConnection) {

        for (Integer i : listaServidores) {
            List<LogConexao> lista = csvJsonConexao(i, awsConnection);
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

    public static void gravaArquivoJsonRede(List<Logs> lista, List<Integer> idServidor, JdbcTemplate banco, AwsConnection awsConnection) {

        // uma hora, um dia. 7 dias
        int[] listaHoras = {1, 24, 168};

        for (Integer i : idServidor) {
            for (Integer tempoHoras : listaHoras) {

                List<LogRede> listaRede = filtrandoLogRede(lista, i, tempoHoras, banco);
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





}


