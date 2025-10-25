package org.example;

import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.*;


public class Main {

    public static void gravaArquivoCsv(List<Logs> lista, String nomeArq){
        OutputStreamWriter saida = null;
        Boolean deuRuim = false;
        nomeArq+=".csv";

        try {
            saida = new OutputStreamWriter(new FileOutputStream(nomeArq), StandardCharsets.UTF_8);

        }catch (IOException erro){
            System.out.println("Erro ao abrir o arquivo gravaArquivoCsv");
            System.exit(1);
        }

        try {
            saida.append("fk_servidor;user;timestamp;cpu;ram;disco;temperatura_cpu;temperatura_disco;memoria_swap;quantidade_processos\n");
            for (Logs log : lista){
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
                saida.write(String.format("%d;%s;%s;%.2f;%.2f;%.2f;%.2f;%.2f;%.2f;%d\n",
                        log.getFk_servidor(), log.getUser(),log.getDataHora().format(formatter), log.getCpu(),log.getRam(),log.getDisco(),log.getTmp_cpu(),log.getTmp_disco(),log.getMemoria_swap(),log.getQtd_processos()));
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
    public static List<Logs> lerJason() {
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream("data.json");
        } catch (FileNotFoundException erro) {
            System.out.println("Arquivo nao encontrado");
            System.exit(1);
        }

        LogMapper logMapper = new LogMapper();
        List<Logs> listaLog = new ArrayList<>();
        try {
            listaLog = logMapper.mapearLogs(inputStream);

        } catch (IOException erro) {
            System.out.println("Erro ao mapear o json lerJason");
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
                           "user": "%s" ,
                           "timestamp": "%s",
                           "cpu": "%.2f",
                           "ram": "%.2f",
                           "disco": "%.2f",
                           "temperatura_cpu": "%.2f",
                           "temperatura_disco": "%.2f",
                           "memoria_swap": "%.2f",
                           "quantidade_processos": "%d"}""",
                            log.getFk_servidor(), log.getUser(),log.getDataHoraString(),log.getCpu(),log.getRam(),log.getDisco(),log.getTmp_cpu(),log.getTmp_disco(),log.getMemoria_swap(),log.getQtd_processos() ));
                }else {
                    saida.write(String.format(Locale.US,"""
                               {
                               "fk_servidor": "%d",
                               "user": "%s" ,
                               "timestamp": "%s",
                               "cpu": "%.2f",
                               "ram": "%.2f",
                               "disco": "%.2f",
                               "temperatura_cpu": "%.2f",
                               "temperatura_disco": "%.2f",
                               "memoria_swap": "%.2f",
                               "quantidade_processos": "%d"},""",
                            log.getFk_servidor(), log.getUser(),log.getDataHoraString(),log.getCpu(),log.getRam(),log.getDisco(),log.getTmp_cpu(),log.getTmp_disco(),log.getMemoria_swap(),log.getQtd_processos()));
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
            System.out.println("Erro a o abrir o arquivo leImportaArquivoCsv");
            System.exit(1);
        }

        try {
            String[] resgistro;
            //readLine é usado para ler uma linha do arquivo
            String linha = entrada.readLine();

            // separa cada  da linha usando o delimitador ";"
            resgistro = linha.split(";");
            // printa os titulos da coluna
            System.out.printf("%16s | %16s | %16s | %16s | %16s | %16s | %16s | %16s | %16s | %16s", resgistro[1],resgistro[2],
                    resgistro[3],resgistro[4],resgistro[5],resgistro[6], resgistro[7],resgistro[8],resgistro[9],resgistro[10]);

            linha = entrada.readLine();
            // converte de string para integer
            //  caso seja de string para int usasse parseint
            while (linha != null) {
                resgistro = linha.split(";");
                Integer fk_servidor = Integer.valueOf(resgistro[1]);
                String user = resgistro[2];
                String dataHoraString = resgistro[3];
                Double cpu = Double.valueOf(resgistro[4]);
                Double ram = Double.valueOf(resgistro[5]);
                Double disco = Double.valueOf(resgistro[6]);
                Double tmp_cpu = Double.valueOf(resgistro[7]);
                Double tmp_disco = Double.valueOf(resgistro[8]);
                Double memoria_swap = Double.valueOf(resgistro[9]);
                Integer qtd_processos = Integer.valueOf(resgistro[10]);
                Logs Log = new Logs(fk_servidor, user, dataHoraString, cpu, ram, disco, tmp_cpu, tmp_disco, memoria_swap, qtd_processos);
                System.out.println("Criando novo log, novo usuario: " + user);
                listaLogs.add(Log);
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
      return listaLogs;
    }


    public static void main(String[] args) throws Exception {
        List<Logs> lista =leImportaArquivoCsv("data");
        gravaArquivoJson(lista,"data");
        List<Logs> listaLogs = lerJason();


//        int indiceMenor;
//        for (int i = 0; i < listaLogs.size() - 1; i++) {
//            indiceMenor = i;
//            for (int j = i + 1; j < listaLogs.size(); j++) {
//                if (listaLogs.get(j).getDisco() > listaLogs.get(indiceMenor).getDisco()) {
//                    indiceMenor = j;
//                }
//            }
//            if (i != indiceMenor) {
//                Logs aux = listaLogs.get(i);
//                listaLogs.set(i, listaLogs.get(indiceMenor));
//                listaLogs.set(indiceMenor, aux);
//            }
//        }

        Connection connection = new Connection();
        JdbcTemplate con = new JdbcTemplate(connection.getDataSource());

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
            Integer contador = 1;

            Integer contadorCPUPorcentagem = 0;
            Integer contadorCPUTemperatura = 0;
            Integer contadorRamPorcentagem = 0;
            Integer contadorDiscoTemperatura = 0;
            Integer contadorSwap = 0;

            for (int l = 0; l < listaLogs.size() - contador; l++) { // For das metricas por servidor

                System.out.println("USER NA LISTA LOGS DO MAIN" + listaLogs.get(l).getUser());

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



                for (int j = 0; j < miniLista.size(); j++) { // For de análise da minilista


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

                    Logs logAtual = miniLista.get(j);

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
                        String mensagemSlack = "Alerta de uso da CPU no servidor: " + fk_servidor +
                                "\nPico do alerta:" + maxCPUPorcentagem +
                                "\nMínimo do alerta:" + minCPUPorcentagem +
                                "\nDuração do alerta: " + duracao_min;
                        SlackNotifier.sendSlackMessage(mensagemSlack);
                        listaAlertas.addAll(miniLista);
                    } else if (contadorCPUTemperatura.equals(duracao_min)) {
                        String insertCPUTemperatura = "INSERT INTO alertas (fk_parametro, max, min)\n" +
                                "VALUES\n" +
                                "((?),(?),(?));";
                        con.update(insertCPUTemperatura, fk_parametroAlerta, maxCPUTemperatura, minCPUTemperatura);
                        String mensagemSlack = "Alerta de temperatura da CPU no servidor: " + fk_servidor +
                                "\nPico do alerta:" + maxCPUTemperatura +
                                "\nMínimo do alerta:" + minCPUTemperatura +
                                "\nDuração do alerta: " + duracao_min;
                        SlackNotifier.sendSlackMessage(mensagemSlack);
                        listaAlertas.add((Logs) miniLista);
                    } else if (contadorDiscoTemperatura.equals(duracao_min)) {
                        String insertDiscoTemperatura = "INSERT INTO alertas (fk_parametro, max, min)\n" +
                                "VALUES\n" +
                                "((?),(?),(?));";
                        con.update(insertDiscoTemperatura, fk_parametroAlerta, maxDiscoTemperatura, minDiscoTemperatura);
                        String mensagemSlack = "Alerta de temperatura do Disco no servidor: " + fk_servidor +
                                "\nPico do alerta:" + maxDiscoTemperatura +
                                "\nMínimo do alerta:" + minDiscoTemperatura +
                                "\nDuração do alerta: " + duracao_min;
                        SlackNotifier.sendSlackMessage(mensagemSlack);
                        listaAlertas.add((Logs) miniLista);
                    } else if (existeAlertaDisco) {
                        String insertDisco = "INSERT INTO alertas (fk_parametro, max, min)\n" +
                                "VALUES\n" +
                                "((?),(?),(?));";
                        con.update(insertDisco, fk_parametroAlerta, maxDiscoPorcentagem, minDiscoPorcentagem);
                        String mensagemSlack = "Alerta de uso do Swap no servidor: " + fk_servidor +
                                "\nPico do alerta:" + maxDiscoPorcentagem +
                                "\nMínimo do alerta:" + minDiscoPorcentagem +
                                "\nDuração do alerta: " + duracao_min;
                        SlackNotifier.sendSlackMessage(mensagemSlack);
                        listaAlertas.add((Logs) miniLista);
                    } else if (contadorRamPorcentagem.equals(duracao_min)) {
                        String insertRamPorcentagem = "INSERT INTO alertas (fk_parametro, max, min)\n" +
                                "VALUES\n" +
                                "((?),(?),(?));";
                        con.update(insertRamPorcentagem, fk_parametroAlerta, maxRamPorcentagem, minRamPorcentagem);
                        String mensagemSlack = "Alerta de uso da RAM no servidor: " + fk_servidor +
                                "\nPico do alerta:" + maxRamPorcentagem +
                                "\nMínimo do alerta:" + minRamPorcentagem +
                                "\nDuração do alerta: " + duracao_min;
                        SlackNotifier.sendSlackMessage(mensagemSlack);
                        listaAlertas.add((Logs) miniLista);
                    } else if (contadorSwap.equals(duracao_min)) {
                        String insertSwap = "INSERT INTO alertas (fk_parametro, max, min)\n" +
                                "VALUES\n" +
                                "((?),(?),(?));";
                        con.update(insertSwap, fk_parametroAlerta, maxSwap, minSwap);
                        String mensagemSlack = "Alerta de uso do Swap no servidor: " + fk_servidor +
                                "\nPico do alerta:" + maxSwap +
                                "\nMínimo do alerta:" + minSwap +
                                "\nDuração do alerta: " + duracao_min;
                        SlackNotifier.sendSlackMessage(mensagemSlack);
                        listaAlertas.add((Logs) miniLista);
                    }
                }


            }
            System.out.println(listaAlertas);
            gravaArquivoCsv(listaAlertas, "alertas");
        }

        }


}
