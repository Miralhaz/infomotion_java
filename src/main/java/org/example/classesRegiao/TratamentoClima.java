package org.example.classesRegiao;

import org.example.AwsConnection;
import org.example.Logs;
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

    public static void buscarRegioes(JdbcTemplate co, List<Logs> listaLogs){

        List<Regiao> listaRegiaoIdRegiao = co.query("SELECT id FROM regiao;",
                new BeanPropertyRowMapper(Regiao.class));
        System.out.println(listaRegiaoIdRegiao + " lista de regioes");



        for (Regiao r : listaRegiaoIdRegiao){
            System.out.println(r.getId() + "id regiao" +
                    "");
            List<Integer> listaServidoresRegiao = co.queryForList("SELECT id FROM servidor WHERE fk_regiao = (?)",
                    Integer.class, r.getId());
            System.out.println(listaServidoresRegiao);
            List listaLogClima = new ArrayList<>();
            List listaLogRegiao = new ArrayList<>();
            List lista = buscarClimaRegiao(r.getId());
            listaLogClima.addAll(lista);

            for (Integer f : listaServidoresRegiao) {



                List lista2 = buscarRegiaoServidor(f,listaLogs);
                listaLogRegiao.addAll(lista2);
            }


            r.setListaLogRegiao(listaLogRegiao);
            r.setListaLogClima(listaLogClima);
        }

        System.out.println(listaRegiaoIdRegiao);
        criarPrevisaoDeDados(listaRegiaoIdRegiao);
    }







    public static List<LogClima> buscarClimaRegiao(Integer idRegiao){

        String nomeArq = "clima"+ idRegiao;

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
                    Integer id = Integer.valueOf(registro[9]);
                    String dataHora = registro[1];
                    Double probabilidadeChuva = Double.valueOf(registro[3]);
                    Double mmChuva = Double.valueOf(registro[2]);
                    Double temperatura = Double.valueOf(registro[5]);
                    Double umidade = Double.valueOf(registro[6]);

                    if (idRegiao.equals(id)) {


                        String dataHoraFormatado = dataHora.replace('T', ' ');
                        LogClima logClima = new LogClima(dataHoraFormatado, probabilidadeChuva, mmChuva, temperatura, umidade);
                        listaClima.add(logClima);
                    }}

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



    public static List<LogRegiao> buscarRegiaoServidor(Integer idServidor, List<Logs> listaLogs){
        List<LogRegiao> listaLogRegiao = new ArrayList<>();

        for (Logs l : listaLogs){
            if (idServidor.equals(l.getFk_servidor())){
                LogRegiao lr = new LogRegiao(l.getFk_servidor(),l.getQtd_processos(),l.getDisco(),l.getRam(),l.getDataHora());
                listaLogRegiao.add(lr);
            }
        }
        return listaLogRegiao;
    }


    public static void criarPrevisaoDeDados(List<Regiao> lista){



    }
}
