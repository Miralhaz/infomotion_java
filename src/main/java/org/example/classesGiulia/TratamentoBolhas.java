package org.example.classesGiulia;

import org.example.AwsConnection;
import org.example.Connection;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TratamentoBolhas {
    public static List<Integer> listaIdServidores = new ArrayList<>();
    private AwsConnection awsCon = new AwsConnection();

    public TratamentoBolhas(AwsConnection awsCon) {
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
                if (contador > 0) {
                    saida.append(",");
                }
                saida.write(String.format(Locale.US,""" 
                           {
                           "fk_servidor": %d,
                           "apelido": "%s" ,
                           "percentual": %.2f,
                           "minutos": %d,
                           "classificacao": "%s"
                           }""",
                        log.getFk_servidor(), log.getApelido(), log.getPercentual(), log.getMinutos(), log.getClassificacao()));
                        contador ++;
            }
            saida.append("]");
            System.out.println("Arquivo Json gerado com sucesso!");

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
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            // converte de string para integer
            // caso fosse de string para int usa-se parseint
            while (linha != null) {

                registro = linha.split(";");
                Integer fk_servidor = Integer.valueOf(registro[0]);
                String apelido = registro[1];
                String tsString = registro[2];
                LocalDateTime ts = LocalDateTime.parse(tsString, fmt);
                Double usoCpu = Double.valueOf(registro[3].replace(",", "."));
                Double usoRam = Double.valueOf(registro[4].replace(",", "."));
                Double usoDisco = Double.valueOf(registro[5].replace(",", "."));
                Double tmp_cpu = Double.valueOf(registro[6].replace(",", "."));
                Double tmp_disco = Double.valueOf(registro[7].replace(",", "."));
                Double memoria_swap = Double.valueOf(registro[8].replace(",", "."));
                Integer qtd_processos = Integer.valueOf(registro[9]);
                Long download_bytes = Long.valueOf(registro[10]);
                Long upload_bytes = Long.valueOf(registro[11]);
                Long pacotes_recebidos = Long.valueOf(registro[12]);
                Long pacotes_enviados = Long.valueOf(registro[13]);
                Integer dropin = Integer.valueOf(registro[14]);
                Integer dropout = Integer.valueOf(registro[15]);
                Long numero_leituras = Long.valueOf(registro[16]);
                Long numero_escritas = Long.valueOf(registro[17]);
                Long bytes_lidos = Long.valueOf(registro[18]);
                Long bytes_escritos = Long.valueOf(registro[19]);
                Integer tempo_leitura = Integer.valueOf(registro[20]);
                Integer tempo_escrita = Integer.valueOf(registro[21]);

                Double percentual = 0.0;
                Integer minutos = 0;
                String classificacao = "";

                LogsGiuliaCriticidade Log = new LogsGiuliaCriticidade(fk_servidor, apelido, percentual, minutos, classificacao);
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

    public List<LogsGiuliaCriticidade> gerarBolhasCpu() {

        awsCon.downloadBucketTrusted("logsGiulia.csv");
        List<LogsGiuliaCriticidade> listaLogs = TratamentoDonut.leImportaArquivoCsv("logsGiulia");

        System.out.println("Estabelecendo conexão ao banco...");
        Connection connection = new Connection();
        JdbcTemplate con = new JdbcTemplate(connection.getDataSource());
        System.out.println("Conexão estabelecida com sucesso!\n");

        listaIdServidores.clear();

        for (LogsGiuliaCriticidade log : listaLogs) {
            Integer id = log.getFk_servidor();

            if (!listaIdServidores.contains(id)) {
                listaIdServidores.add(id);
            }
        }

        List<LogsGiuliaCriticidade> listaBolhasCpu = new ArrayList<>();

        for (Integer id : listaIdServidores) {
            List<LogsGiuliaCriticidade> logsServidor = new ArrayList<>();
            for (LogsGiuliaCriticidade log : listaLogs) {
                if (log.getFk_servidor().equals(id)) {
                    logsServidor.add(log);
                }
            }

            String apelidoServidor = logsServidor.get(0).getApelido();

            String selectTipo = ("""
                select pa.max
                from parametro_alerta pa
                inner join componentes c on c.id = pa.fk_componente
                where c.tipo = (?) and pa.fk_servidor = (?);
            """);

            Double limiteCpu = con.queryForObject(selectTipo, Double.class, "CPU", id);

            Integer minutosAcima = 0;
            Double maxPercentual = 0.0;

            for (int i = 0; i < logsServidor.size() - 1; i++) {
                LogsGiuliaCriticidade logAtual = logsServidor.get(i);
                LogsGiuliaCriticidade logProximo  = logsServidor.get(i + 1);

                long minutosEntreLogs = Duration.between(logAtual.getTimestamp(), logProximo.getTimestamp()).toMinutes();

                if (minutosEntreLogs <= 0) {
                    minutosEntreLogs = 1;
                }

                Double uso = logAtual.getUsoCpu();

                if (uso > limiteCpu) {
                    minutosAcima += (int) minutosEntreLogs;
                }

                if (uso > maxPercentual) {
                    maxPercentual = uso;
                }
            }

            String classificacao;

            if (minutosAcima >= 30){
                classificacao = "CRITICO";
            }

            if (minutosAcima >= 5){
                classificacao = "ATENCAO";
            }

            else {
                classificacao = "OK";
            }

                LogsGiuliaCriticidade bolha = new LogsGiuliaCriticidade(id, apelidoServidor, maxPercentual, minutosAcima, classificacao);
                listaBolhasCpu.add(bolha);
        }

        gravaArquivoJson(listaLogs, "criticidadeCpu");
        awsCon.uploadBucketClient("tratamentoGiulia", "criticidadeCpu.json");
        awsCon.limparTemporarios();

        return listaBolhasCpu;
    }

    public List<LogsGiuliaCriticidade> gerarBolhasRam() {

        awsCon.downloadBucketTrusted("logsGiulia.csv");
        List<LogsGiuliaCriticidade> listaLogs = TratamentoDonut.leImportaArquivoCsv("logsGiulia");

        System.out.println("Estabelecendo conexão ao banco...");
        Connection connection = new Connection();
        JdbcTemplate con = new JdbcTemplate(connection.getDataSource());
        System.out.println("Conexão estabelecida com sucesso!\n");

        listaIdServidores.clear();

        for (LogsGiuliaCriticidade log : listaLogs) {
            Integer id = log.getFk_servidor();

            if (!listaIdServidores.contains(id)) {
                listaIdServidores.add(id);
            }
        }

        List<LogsGiuliaCriticidade> listaBolhasRam = new ArrayList<>();

        for (Integer id : listaIdServidores) {
            List<LogsGiuliaCriticidade> logsServidor = new ArrayList<>();
            for (LogsGiuliaCriticidade log : listaLogs) {
                if (log.getFk_servidor().equals(id)) {
                    logsServidor.add(log);
                }
            }

            String apelidoServidor = logsServidor.get(0).getApelido();

            String selectTipo = ("""
                select pa.max
                from parametro_alerta pa
                inner join componentes c on c.id = pa.fk_componente
                where c.tipo = (?) and pa.fk_servidor = (?);
            """);

            Double limiteRam = con.queryForObject(selectTipo, Double.class, "RAM", id);

            Integer minutosAcima = 0;
            Double maxPercentual = 0.0;

            for (int i = 0; i < logsServidor.size() - 1; i++) {
                LogsGiuliaCriticidade logAtual = logsServidor.get(i);
                LogsGiuliaCriticidade logProximo  = logsServidor.get(i + 1);

                long minutosEntreLogs = Duration.between(logAtual.getTimestamp(), logProximo.getTimestamp()).toMinutes();

                if (minutosEntreLogs <= 0) {
                    minutosEntreLogs = 1;
                }

                Double uso = logAtual.getUsoCpu();

                if (uso > limiteRam) {
                    minutosAcima += (int) minutosEntreLogs;
                }

                if (uso > maxPercentual) {
                    maxPercentual = uso;
                }
            }

            String classificacao;

            if (minutosAcima >= 30){
                classificacao = "CRITICO";
            }

            if (minutosAcima >= 5){
                classificacao = "ATENCAO";
            }

            else {
                classificacao = "OK";
            }

            LogsGiuliaCriticidade bolha = new LogsGiuliaCriticidade(id, apelidoServidor, maxPercentual, minutosAcima, classificacao);
            listaBolhasRam.add(bolha);
        }

        gravaArquivoJson(listaLogs, "criticidadeRam");
        awsCon.uploadBucketClient("tratamentoGiulia", "criticidadeRam.json");
        awsCon.limparTemporarios();

        return listaBolhasRam;
    }

    public List<LogsGiuliaCriticidade> gerarBolhasDisco() {

        awsCon.downloadBucketTrusted("logsGiulia.csv");
        List<LogsGiuliaCriticidade> listaLogs = TratamentoDonut.leImportaArquivoCsv("logsGiulia");

        System.out.println("Estabelecendo conexão ao banco...");
        Connection connection = new Connection();
        JdbcTemplate con = new JdbcTemplate(connection.getDataSource());
        System.out.println("Conexão estabelecida com sucesso!\n");

        listaIdServidores.clear();

        for (LogsGiuliaCriticidade log : listaLogs) {
            Integer id = log.getFk_servidor();

            if (!listaIdServidores.contains(id)) {
                listaIdServidores.add(id);
            }
        }

        List<LogsGiuliaCriticidade> listaBolhasDisco = new ArrayList<>();

        for (Integer id : listaIdServidores) {
            List<LogsGiuliaCriticidade> logsServidor = new ArrayList<>();
            for (LogsGiuliaCriticidade log : listaLogs) {
                if (log.getFk_servidor().equals(id)) {
                    logsServidor.add(log);
                }
            }

            String apelidoServidor = logsServidor.get(0).getApelido();

            String selectTipo = ("""
                select pa.max
                from parametro_alerta pa
                inner join componentes c on c.id = pa.fk_componente
                where c.tipo = (?) and pa.fk_servidor = (?);
            """);

            Double limiteDisco = con.queryForObject(selectTipo, Double.class, "DISCO", id);

            Integer minutosAcima = 0;
            Double maxPercentual = 0.0;

            for (int i = 0; i < logsServidor.size() - 1; i++) {
                LogsGiuliaCriticidade logAtual = logsServidor.get(i);
                LogsGiuliaCriticidade logProximo  = logsServidor.get(i + 1);

                long minutosEntreLogs = Duration.between(logAtual.getTimestamp(), logProximo.getTimestamp()).toMinutes();

                if (minutosEntreLogs <= 0) {
                    minutosEntreLogs = 1;
                }

                Double uso = logAtual.getUsoCpu();

                if (uso > limiteDisco) {
                    minutosAcima += (int) minutosEntreLogs;
                }

                if (uso > maxPercentual) {
                    maxPercentual = uso;
                }
            }

            String classificacao;

            if (minutosAcima >= 30){
                classificacao = "CRITICO";
            }

            if (minutosAcima >= 5){
                classificacao = "ATENCAO";
            }

            else {
                classificacao = "OK";
            }

            LogsGiuliaCriticidade bolha = new LogsGiuliaCriticidade(id, apelidoServidor, maxPercentual, minutosAcima, classificacao);
            listaBolhasDisco.add(bolha);
        }

        gravaArquivoJson(listaLogs, "criticidadeDisco");
        awsCon.uploadBucketClient("tratamentoGiulia", "criticidadeDisco.json");
        awsCon.limparTemporarios();

        return listaBolhasDisco;
    }
}
