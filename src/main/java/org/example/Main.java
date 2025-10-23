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
            saida.append("user;timestamp;cpu;ram;disco;temperatura_cpu;temperatura_disco;memoria_swap;quantidade_processos\n");
            for (Logs log : lista){
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
                saida.write(String.format("%s;%s;%.2f;%.2f;%.2f;%.2f;%.2f;%.2f;%d\n",log.getUser(),log.getDataHora().format(formatter), log.getCpu(),log.getRam(),log.getDisco(),log.getTmp_cpu(),log.getTmp_disco(),log.getMemoria_swap(),log.getQtd_processos()));
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
                           "user": "%s" ,
                           "timestamp": "%s",
                           "cpu": "%.2f",
                           "ram": "%.2f",
                           "disco": "%.2f",
                           "temperatura_cpu": "%.2f",
                           "temperatura_disco": "%.2f",
                           "memoria_swap": "%.2f",
                           "quantidade_processos": "%d"}""",log.getUser(),log.getDataHoraString(),log.getCpu(),log.getRam(),log.getDisco(),log.getTmp_cpu(),log.getTmp_disco(),log.getMemoria_swap(),log.getQtd_processos()));
                }else {
                    saida.write(String.format(Locale.US,"""
                               {
                               "user": "%s" ,
                               "timestamp": "%s",
                               "cpu": "%.2f",
                               "ram": "%.2f",
                               "disco": "%.2f",
                               "temperatura_cpu": "%.2f",
                               "temperatura_disco": "%.2f",
                               "memoria_swap": "%.2f",
                               "quantidade_processos": "%d"},""",log.getUser(),log.getDataHoraString(),log.getCpu(),log.getRam(),log.getDisco(),log.getTmp_cpu(),log.getTmp_disco(),log.getMemoria_swap(),log.getQtd_processos()));
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
            System.out.printf("%16s | %16s | %16s | %16s | %16s | %16s | %16s | %16s | %16s", resgistro[0],resgistro[1],
                    resgistro[2],resgistro[3],resgistro[4],resgistro[5], resgistro[6],resgistro[7],resgistro[8]);

            linha = entrada.readLine();
            // converte de string para integer
            //  caso seja de string para int usasse parseint
            while (linha != null) {
                resgistro = linha.split(";");
                String user = resgistro[0];
                String dataHoraString = resgistro[1];
                Double cpu = Double.valueOf(resgistro[2]);
                Double ram = Double.valueOf(resgistro[3]);
                Double disco = Double.valueOf(resgistro[4]);
                Double tmp_cpu = Double.valueOf(resgistro[5]);
                Double tmp_disco = Double.valueOf(resgistro[6]);
                Double memoria_swap = Double.valueOf(resgistro[7]);
                Integer qtd_processos = Integer.valueOf(resgistro[8]);
                Logs Log = new Logs(user, dataHoraString, cpu, ram, disco, tmp_cpu, tmp_disco, memoria_swap, qtd_processos);
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
    

    public static void main(String[] args) {
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
//        Integer fk_servidor = listaLogs.getFk_servidor();

        String selectParametroPorServidor = "SELECT * FROM parametro_alerta where fk_servidor = (?);";
        List<Parametro_alerta> metrica = con.queryForList(selectParametroPorServidor, Parametro_alerta.class, /*fk_servidor*/);

        Integer contador = 1;

        Boolean existeAlertaDisco = false;

        for (int i = 0; i < metrica.size(); i++) { // For das metricas por servidor


            Integer fk_componente = metrica.get(i).getFk_componente();
            Double max = metrica.get(i).getMax();
            Double duracao_min = metrica.get(i).getDuracao_min(); // Define a quantidade de capturas em seguida que devem ser maior que o parametro para ser considerado um alerta
            Integer fk_parametroAlerta = metrica.get(i).getId();

            Integer contadorCPUPorcentagem = 0;
            Integer contadorCPUTemperatura = 0;
            Integer contadorRamPorcentagem = 0;
            Integer contadorDiscoTemperatura = 0;
            Integer contadorSwap = 0;

            Double maxCPUPorcentagem = 0.0;
            Double minCPUPorcentagem = 0.0;
            Double maxCPUTemperatura = 0.0;
            Double minCPUTemperatura = 0.0;
            Double maxRamPorcentagem = 0.0;
            Double minRamPorcentagem = 0.0;
            Double maxDiscoPorcentagem = 0.0;
            Double minDiscoPorcentagem = 0.0;
            Double maxDiscoTemperatura = 0.0;
            Double minDiscoTemperatura = 0.0;
            Double maxSwap = 0.0;
            Double minSwap = 0.0;

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
                            if (logAtual.getCpu() > maxCPUPorcentagem){
                                maxCPUPorcentagem = logAtual.getCpu();
                            } else if (logAtual.getCpu() < minCPUPorcentagem){
                                minCPUPorcentagem = logAtual.getCpu();
                            }
                        }
                    } else {
                        if (logAtual.getTmp_cpu() > max) {
                            contadorCPUTemperatura++;
                            if (logAtual.getTmp_cpu() > maxCPUTemperatura){
                                maxCPUTemperatura = logAtual.getTmp_cpu();
                            } else if (logAtual.getTmp_cpu() < minCPUTemperatura){
                                minCPUTemperatura = logAtual.getTmp_cpu();
                            }
                        }
                    }
                } else if (tipo.equalsIgnoreCase("RAM")) {
                    if (logAtual.getRam() > max) {
                        contadorRamPorcentagem++;
                        if (logAtual.getRam() > maxRamPorcentagem){
                            maxRamPorcentagem = logAtual.getRam();
                        } else if (logAtual.getRam() < minRamPorcentagem){
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
                            if (logAtual.getTmp_disco() > maxDiscoTemperatura){
                                maxDiscoTemperatura = logAtual.getTmp_disco();
                            } else if (logAtual.getTmp_disco() < minDiscoTemperatura){
                                minDiscoTemperatura = logAtual.getTmp_disco();
                            }
                        }
                    }
                }
                if (tipo.equalsIgnoreCase("SWAP")) {
                    if (logAtual.getMemoria_swap() > max) {
                        contadorSwap++;
                        if (logAtual.getMemoria_swap() > maxSwap){
                            maxSwap = logAtual.getMemoria_swap();
                        } else if (logAtual.getMemoria_swap() < minSwap){
                            minSwap = logAtual.getMemoria_swap();
                        }
                    }
                }
            }

            if (contadorCPUPorcentagem.equals(duracao_min)) {
                String insertCPUPorcentagem = "INSERT INTO alertas (fk_parametro, max, min)\n" +
                        "VALUES\n" +
                        "((?),(?),(?));";
                con.execute(insertCPUPorcentagem, fk_parametroAlerta,  ,);
            }
                    || contadorCPUTemperatura.equals(duracao_min) || existeAlertaDisco ||
                contadorDiscoTemperatura.equals(duracao_min) || contadorRamPorcentagem.equals(duracao_min) || contadorSwap.equals(duracao_min)) {
                System.out.println("algum passou"); // Válida se atingiu quantidade minima do parametro
                listaAlertas.add((Logs) miniLista);
            }

            System.out.println(listaAlertas);
            gravaArquivoCsv(listaAlertas, "alertas");
        }

    }
}
