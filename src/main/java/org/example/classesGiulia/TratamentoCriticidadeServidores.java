package org.example.classesGiulia;

import org.example.AwsConnection;
import org.example.LogMapper;
import org.example.Logs;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TratamentoCriticidadeServidores {

    private AwsConnection awsCon;

    public TratamentoCriticidadeServidores(AwsConnection awsCon) {
        this.awsCon = awsCon;
    }

    public static void gravaArquivoJson(List<LogsGiuliaCriticidade> lista, String nomeArq) {

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
            for (LogsGiuliaCriticidade log : lista) {
                contador ++;
                if (contador == lista.size()){
                    saida.write(String.format(Locale.US,"""
                           {
                           "fk_servidor": "%d",
                           "apelido": "%s" ,
                           "timestamp": "%s",
                           "minutos": "%",
                           "cpu": "%.2f",
                           "ram": "%.2f",
                           "disco": "%.2f",
                           "classificacao": "%s"
                           }""",
                            log.getFk_servidor(), log.getApelido(), log.getDataHoraString(), log.getMinutos(), log.getUsoCpu(), log.getUsoRam(), log.getUsoDisco(), log.getClassificacao()));
                }else {
                    saida.write(String.format(Locale.US,"""
                           {
                           "fk_servidor": "%d",
                           "apelido": "%s" ,
                           "timestamp": "%s",
                           "minutos": "%",
                           "cpu": "%.2f",
                           "ram": "%.2f",
                           "disco": "%.2f",
                           "classificacao": "%s"
                           }""",
                            log.getFk_servidor(), log.getApelido(), log.getDataHoraString(), log.getMinutos(), log.getUsoCpu(), log.getUsoRam(), log.getUsoDisco(), log.getClassificacao()));
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
    public static  List<LogsGiuliaCriticidade> leImportaArquivoCsv(String nomeArq){
        Reader arq = null; // objeto arquivo
        BufferedReader entrada = null; // objeto leitor de arquivo
        nomeArq += ".csv";
        List<LogsGiuliaCriticidade> listaLogs = new ArrayList<>();

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
            System.out.println("Lendo e Importando CSV de dados do bucket-trusted");

            linha = entrada.readLine();
            // converte de string para integer
            // caso fosse de string para int usa-se parseint
            while (linha != null) {

                registro = linha.split(";");
                Integer fk_servidor = Integer.valueOf(registro[0]);
                String apelido = registro[2];
                String timestamp = registro[3];
                Integer minutos = Integer.valueOf(registro[4]);
                Double usoCpu = Double.valueOf(registro[5]);
                Double usoRam = Double.valueOf(registro[6]);
                Double usoDisco = Double.valueOf(registro[7]);
                String classificacao = registro[8];
                LogsGiuliaCriticidade Log = new LogsGiuliaCriticidade(fk_servidor, apelido, timestamp, minutos, usoCpu, usoRam, usoDisco, classificacao);
                listaLogs.add(Log);
                linha = entrada.readLine();
            }
        }catch (IOException e ){
            System.out.println("Erro ao ler o arquivo");
            e.printStackTrace();
            System.exit(1);

        }
        finally {
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

}
