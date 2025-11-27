package org.example.classesRegiao;

import org.example.AwsConnection;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class TratamentoClima {
    private static final DateTimeFormatter INPUT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static AwsConnection awsConnection;
    private static final String nomePasta = "Dashboard_Regiao";
    private static JdbcTemplate banco;

    public TratamentoClima(AwsConnection awsConnection, JdbcTemplate banco) {
        this.awsConnection = awsConnection;
        this.banco = banco;
    }

    public static void buscarRegioes(JdbcTemplate co){

        List<Regiao> listaRegiao = co.query("SELECT id FROM regiao;",
                new BeanPropertyRowMapper(Regiao.class));
        System.out.println(listaRegiao);

        for (Regiao r : listaRegiao){
            System.out.println(r.getId());
            List<Integer> listaServidoresRegiao = co.queryForList("SELECT id FROM servidor WHERE fk_regiao = ?",
                    Integer.class, r.getId());
            System.out.println(listaServidoresRegiao);
            List listaLogClima = new ArrayList<>();
            List listaLogRegiao = new ArrayList<>();

            for (Integer f : listaServidoresRegiao) {
                List lista =  buscarClimaServidor(f);
                listaLogClima.addAll(lista);
                List lista2 = buscarRegiaoServidor(f);
                listaLogRegiao.addAll(lista2);
            }

            Regiao reg = new Regiao(r.getId());
            reg.setListaLogClima(listaLogClima);
            reg.setListaLogRegiao(listaLogRegiao);




        }
    }







    public static List<LogClima> buscarClimaServidor(Integer idServidor){

        String nomeArq = "clima"+ idServidor;

        Reader arq = null;
        BufferedReader entrada = null;
        nomeArq += ".csv";
        List<LogClima> listaClima = new ArrayList<>();


        awsConnection.downloadBucketTrusted(nomeArq);

        try {
            arq = new InputStreamReader(new FileInputStream(nomeArq), StandardCharsets.UTF_8);
            entrada = new BufferedReader(arq);
        } catch (IOException e) {
            System.out.println("Erro ao abrir o arquivo [csvClima]");
            System.exit(1);
        }

        try {
            String[] registro;

            String linha = null;

            linha = entrada.readLine();
            int contador = 0;
            while (linha != null) {

                registro = linha.split(";");

                if (contador > 0) {
                    Integer fkServidor = Integer.valueOf(registro[9]);
                    String dataHora = registro[1];
                    Double probabilidadeChuva = Double.valueOf(registro[3]);
                    Double mmChuva = Double.valueOf(registro[2]);
                    Double temperatura = Double.valueOf(registro[5]);
                    Double umidade = Double.valueOf(registro[6]);

                    String dataHoraFormatado = dataHora.replace('T',' ');
                    LogClima logClima = new LogClima(fkServidor, dataHoraFormatado, probabilidadeChuva, mmChuva, temperatura, umidade);
                    listaClima.add(logClima);
                }

                contador++;

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
        return listaClima;
    }



    public static List<LogRegiao> buscarRegiaoServidor(Integer idServidor){

        String nomeArq = "logs_consolidados_servidores";

        Reader arq = null;
        BufferedReader entrada = null;
        nomeArq += ".csv";
        List<LogRegiao> listaLogRegiao = new ArrayList<>();


        try {
            arq = new InputStreamReader(new FileInputStream(nomeArq), StandardCharsets.UTF_8);
            entrada = new BufferedReader(arq);
        } catch (IOException e) {
            System.out.println("Erro ao abrir o arquivo [csvConsolidado]");
            System.exit(1);
        }

        try {
            String[] registro;

            String linha = null;

            linha = entrada.readLine();

            while (linha != null) {

                registro = linha.split(";");
                Integer fkServidor = Integer.valueOf(registro[0]);
                 Double usoDisco  = Double.valueOf(registro[5]);
                 Double usoRam  = Double.valueOf(registro[4]);
                 Integer qtdRequisicoes  = Integer.valueOf(registro[8]);
                 String dataHora = registro[2];
                String dataHoraFormatado = dataHora.replace('T',' ');

                //LogRegiao logRegiao = new LogRegiao(fkServidor,qtdRequisicoes, usoDisco,usoRam,dataHora.replace('T', ' '));
                //listaLogRegiao.add(logRegiao);
               // linha = entrada.readLine();
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
        return listaLogRegiao;
    }


    public void criarPrevisaoDeDados(){



    }
}
