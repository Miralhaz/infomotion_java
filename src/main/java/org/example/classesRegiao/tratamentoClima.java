package org.example.classesRegiao;

import org.apache.commons.math3.analysis.function.Log;
import org.example.AwsConnection;
import org.example.classesRede.LogConexao;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class tratamentoClima {
    private static final DateTimeFormatter INPUT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static AwsConnection awsConnection;
    private static final String nomePasta = "Dashboard_Regiao";
    private static JdbcTemplate banco;

    public tratamentoClima(AwsConnection awsConnection, JdbcTemplate banco) {
        this.awsConnection = awsConnection;
        this.banco = banco;
    }


    public List<LogClima> buscarCsvClima(Integer idServidor){

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

            while (linha != null) {

                registro = linha.split(";");
                 Integer fkServidor = Integer.valueOf(registro[10]);
                 String dataHora = registro[1];
                 Double probabilidadeChuva = Double.valueOf(registro[3]) ;
                 Double mmChuva = Double.valueOf(registro[2]);
                 Double temperatura = Double.valueOf(registro[5]);
                 Double umidade = Double.valueOf(registro[6]);

                 LogClima logClima = new LogClima(fkServidor,dataHora,probabilidadeChuva,mmChuva,temperatura,umidade);
                listaClima.add(logClima);
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
}
