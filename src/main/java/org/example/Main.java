package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
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
            System.out.println("Erro ao abrir o arquivo");
            System.exit(1);
        }

        try {
            saida.append("user;timestamp;cpu;ram;disco;temperatura_cpu;temperatura_disco;memoria_swap;quantidade_processos\n");
            for (Logs log : lista){
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
                saida.write(String.format("%s;%s;%.2f;%.2f;%.2f;%.2f;%.2f;%.2f;%d",log.getUser(),log.getDataHora().format(formatter), log.getCpu(),log.getRam(),log.getDisco(),log.getTmp_cpu(),log.getTmp_disco(),log.getMemoria_swap(),log.getQtd_processos()));
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
            inputStream = new FileInputStream("logs.json");
        } catch (FileNotFoundException erro) {
            System.out.println("Arquivo nao encontrado");
            System.exit(1);
        }

        LogMapper logMapper = new LogMapper();
        List<Logs> listaLog = new ArrayList<>();
        try {
            listaLog = logMapper.mapearLogs(inputStream);

        } catch (IOException erro) {
            System.out.println("Erro ao mapear o json");
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
            System.out.println("Erro ao abrir o arquivo");
            System.exit(1);
        }

        try {
            saida.append("[");
            for (Logs log : lista) {
                saida.write(String.format("""
                           {
                           "user": %s ,
                           "timestamp": %s,
                           "cpu": %.2f,
                           "ram": %.2f,
                           "disco": %.2f,
                           "temperatura_cpu": %.2f,
                           "temperatura_disco": %.2f,
                           "memoria_swap": %.2f,
                           "quantidade_processos": %d}""",log.getUser(),log.getDataHoraString(),log.getCpu(),log.getRam(),log.getDisco(),log.getTmp_cpu(),log.getTmp_disco(),log.getMemoria_swap(),log.getQtd_processos()));
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
            System.out.println("Erro a o abrir o arquivo");
            System.exit(1);
        }

        try {
            String[] resgistro;
            //readLine é usado para ler uma linha do arquivo
            String linha = entrada.readLine();

            // separa cada  da linha usando o delimitador ";"
            resgistro = linha.split(";");
            // printa os titulos da coluna
            System.out.printf("%16s | %16s | %16s | %16s | $16s | %16s | %16s | %16s | %16s ", resgistro[0],resgistro[1],
                    resgistro[2],resgistro[3],resgistro[4],resgistro[5], resgistro[6],resgistro[7]);

            linha = entrada.readLine();
            // converte de string para integer
            //  caso seja de string para int usasse parseint
            while (linha != null){
                resgistro = linha.split(";");
                 String user =  resgistro[0];
                 String dataHoraString = resgistro[1];
                 Double cpu =  Double.valueOf(resgistro[2]);
                 Double ram = Double.valueOf(resgistro[3]);
                 Double disco = Double.valueOf(resgistro[4]);
                 Double tmp_cpu = Double.valueOf(resgistro[5]);
                 Double tmp_disco = Double.valueOf(resgistro[6]);
                 Double memoria_swap = Double.valueOf(resgistro[6]);
                 Integer qtd_processos = Integer.valueOf(resgistro[7]);
                 Logs Log = new Logs(user,dataHoraString,cpu,ram,disco,tmp_cpu,tmp_disco,memoria_swap,qtd_processos);
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
        gravaArquivoJson(lista,"Logs");
        List<Logs> listaLogs = lerJason();
        int indiceMenor;
        for (int i = 0; i < listaLogs.size() - 1; i++) {
            indiceMenor = i;
            for (int j = i + 1; j < listaLogs.size(); j++) {
                if (listaLogs.get(j).getDisco() > listaLogs.get(indiceMenor).getDisco()) {
                    indiceMenor = j;
                }
            }
            if (i != indiceMenor) {
                Logs aux = listaLogs.get(i);
                listaLogs.set(i, listaLogs.get(indiceMenor));
                listaLogs.set(indiceMenor, aux);

                System.out.println(listaLogs);
            }
        }
        Connection connection = new Connection();
        JdbcTemplate con = new JdbcTemplate(connection.getDataSource());

        List<Metricas> metrica = con.query("SELECT componente,max,duracao_min FROM parametro_alerta WHERE id = 1;",
                new BeanPropertyRowMapper(Metricas.class));

        String componente = metrica.get(0).getComponente();
        Double max = metrica.get(0).getMax();
        LocalTime duracao_min = metrica.get(0).getDuracao_min();

        List<Logs> listaAlertas = new ArrayList<>();
        for (int i = 0; i < listaLogs.size(); i++) {
            Logs logAtual = listaLogs.get(0);
            if (logAtual.getCpu() > max || logAtual.getDisco() > max || logAtual.getRam() > max || logAtual.getTmp_cpu() > max || logAtual.getTmp_disco() > max){
              listaAlertas.add(logAtual);
            }
        }
        gravaArquivoCsv(listaAlertas,"alertas");
    }
}
