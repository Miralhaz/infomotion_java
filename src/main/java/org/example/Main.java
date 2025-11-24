package org.example;

import org.example.classesMiralha.TratamentoProcessos;
import org.example.classesMiralha.TratamentoTemperaturaCpu;
import org.example.classesMiralha.TratamentoTemperaturaDisco;
import org.example.classesRede.TratamentoRede;
import org.example.classesRenan.tratamentoNearRealTime;
import org.example.classesWillian.TratamentoWillian;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.*;


public class Main {

    public static List<Integer> listaIdServidores = new ArrayList<>();
    static AwsConnection aws = new AwsConnection();

    public static void gravaArquivoCsv(List<Logs> lista, String nomeArq, boolean append){
        OutputStreamWriter saida = null;
        Boolean deuRuim = false;
        String nomeCompletoArq = nomeArq + ".csv";

        File arquivoLocal = new File(nomeCompletoArq);
        boolean arquivoExisteAntesDaEscrita = arquivoLocal.exists();

        try {
            saida = new OutputStreamWriter(new FileOutputStream(nomeCompletoArq, append), StandardCharsets.UTF_8);

        }catch (IOException erro){
            System.err.println("Erro ao abrir o arquivo " + nomeCompletoArq + " em gravaArquivoCsv");
            System.exit(1);
        }

        try {
            if (!append || !arquivoExisteAntesDaEscrita) {
                saida.append("fk_servidor;nomeMaquina;timestamp;cpu;ram;disco;temperatura_cpu;temperatura_disco;memoria_swap;quantidade_processos;download_bytes;upload_bytes;pacotes_recebidos;pacotes_enviados;dropin;dropout;numero_leituras;numero_escritas;bytes_lidos;bytes_escritos;tempo_leitura;tempo_escrita\n");
            }

            for (Logs log : lista){
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

                saida.write(String.format("%d;%s;%s;%.2f;%.2f;%.2f;%.2f;%.2f;%.2f;%d;%d;%d;%d;%d;%d;%d;%d;%d;%d;%d;%d;%d\n",
                        log.getFk_servidor(), log.getNomeMaquina(), log.getDataHora().format(formatter), log.getCpu(),log.getRam(),log.getDisco(),log.getTmp_cpu(),log.getTmp_disco(),log.getMemoria_swap(),log.getQtd_processos(), log.getDownload_bytes(), log.getUpload_bytes(), log.getPacotes_recebidos(), log.getPacotes_enviados(), log.getDropin(), log.getDropout(), log.getNumero_leituras(), log.getNumero_escritas(), log.getBytes_lidos(), log.getBytes_escritos(), log.getTempo_leitura(), log.getTempo_escrita()));
            }

        }catch (IOException erro){
            System.err.println("Erro ao gravar o arquivo " + nomeCompletoArq);
            erro.printStackTrace();
            deuRuim = true;
        }finally {
            try {
                if (saida != null) saida.close();
            } catch (IOException erro) {
                System.err.println("Erro ao fechar o arquivo " + nomeCompletoArq);
                deuRuim = true;
            }
            if (deuRuim){
                System.exit(1);
            }
        }
    }

    public static void consolidarArquivosRaw(JdbcTemplate con) {
        final String arquivoConsolidadoTrusted = "logs_consolidados_servidores";

        System.out.println("\nIniciando consolidação...");

        aws.downloadTemperaturaBucket(arquivoConsolidadoTrusted + ".csv");

        List<String> arquivosRawParaProcessar = aws.listarArquivosRaw();
        List<Logs> logsNovosParaConsolidar = new ArrayList<>();

        if (arquivosRawParaProcessar.isEmpty()) {
            System.out.println("Nenhum arquivo RAW novo encontrado para processamento (Padrão: data[n].csv).");
            return;
        }

        System.out.printf("Encontrados %d arquivos RAW para processar.\n", arquivosRawParaProcessar.size());

        for (String chaveRaw : arquivosRawParaProcessar) {
            try {



                aws.downloadBucketRaw(chaveRaw);

                List<Logs> logsDoArquivo = leImportaArquivoCsv(chaveRaw);
                // -------------------------------------------------- DETECTANDO ALERTAS --------------------------------------------------
                // Pegando o id do servidor
                Integer fk_servidor = logsDoArquivo.get(1).getFk_servidor();
                TratamentoAlertas.TratamentoAlertas(con, fk_servidor, logsDoArquivo, aws);

                logsNovosParaConsolidar.addAll(logsDoArquivo);
                System.out.printf("Conteúdo de '%s' lido com sucesso (%d novos logs).\n", chaveRaw, logsDoArquivo.size());

                aws.deleteCsvLocal(chaveRaw);

            } catch (Exception e) {
                System.err.println("ERRO ao processar arquivo RAW " + chaveRaw + ". Pulando para o próximo: " + e.getMessage());
            }
        }

        if (logsNovosParaConsolidar.isEmpty()) {
            System.out.println("Nenhum novo log válido foi lido e consolidado. Upload para Trusted abortado.");
            return;
        }

        gravaArquivoCsv(logsNovosParaConsolidar, arquivoConsolidadoTrusted, true);
        aws.uploadBucketTrusted(arquivoConsolidadoTrusted + ".csv");

        System.out.println("\nConsolidação concluída");
    }

    public static void gravaArquivoCsv(List<Logs> lista, String nomeArq){
        OutputStreamWriter saida = null;
        Boolean deuRuim = false;
        nomeArq += ".csv";

        try {
            saida = new OutputStreamWriter(new FileOutputStream(nomeArq), StandardCharsets.UTF_8);

        }catch (IOException erro){
            System.out.println("Erro ao abrir o arquivo gravaArquivoCsv");
            System.exit(1);
        }

        try {
            saida.append("fk_servidor;nomeMaquina;timestamp;cpu;ram;disco;temperatura_cpu;temperatura_disco;memoria_swap;quantidade_processos;download_bytes;upload_bytes;pacotes_recebidos;pacotes_enviados;dropin;dropout;numero_leituras;numero_escritas;bytes_lidos;bytes_escritos;tempo_leitura;tempo_escrita\n");

            for (Logs log : lista){
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

                saida.write(String.format(Locale.US, "%d;%s;%s;%.2f;%.2f;%.2f;%.2f;%.2f;%.2f;%d;%d;%d;%d;%d;%d;%d;%d;%d;%d;%d;%d;%d\n",
                        log.getFk_servidor(), log.getNomeMaquina(), log.getDataHora().format(formatter), log.getCpu(),log.getRam(),log.getDisco(),log.getTmp_cpu(),log.getTmp_disco(),log.getMemoria_swap(),log.getQtd_processos(), log.getDownload_bytes(), log.getUpload_bytes(), log.getPacotes_recebidos(), log.getPacotes_enviados(), log.getDropin(), log.getDropout(), log.getNumero_leituras(), log.getNumero_escritas(), log.getBytes_lidos(), log.getBytes_escritos(), log.getTempo_leitura(), log.getTempo_escrita()));
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

    public static  List<Logs> leImportaArquivoCsv(String nomeArq){
        Reader arq = null; // objeto arquivo
        BufferedReader entrada = null; // objeto leitor de arquivo
        List<Logs> listaLogs = new ArrayList<>();

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
             System.out.println("Lendo e Importando CSV de dados do bucket-raw");

            linha = entrada.readLine();
            // converte de string para integer
            // caso fosse de string para int usa-se parseint
            while (linha != null) {

                registro = linha.split(";");
                Integer fk_servidor = Integer.valueOf(registro[1]);
                String nomeMaquina = registro[2];
                String dataHoraString = registro[3];
                Double cpu = Double.valueOf(registro[4].replace(",", "."));
                Double ram = Double.valueOf(registro[5].replace(",", "."));
                Double disco = Double.valueOf(registro[6].replace(",", "."));
                Double tmp_cpu = Double.valueOf(registro[7].replace(",", "."));
                Double tmp_disco = Double.valueOf(registro[8].replace(",", "."));
                Double memoria_swap = Double.valueOf(registro[9].replace(",", "."));
                Integer qtd_processos = Integer.valueOf(registro[10]);
                Long download_bytes = Long.valueOf(registro[11]);
                Long upload_bytes = Long.valueOf(registro[12]);
                Long pacotes_recebidos = Long.valueOf(registro[13]);
                Long pacotes_enviados = Long.valueOf(registro[14]);
                Integer dropin = Integer.valueOf(registro[15]);
                Integer dropout = Integer.valueOf(registro[16]);
                Long numero_leituras = Long.valueOf(registro[17]);
                Long numero_escritas = Long.valueOf(registro[18]);
                Long bytes_lidos = Long.valueOf(registro[19]);
                Long bytes_escritos = Long.valueOf(registro[20]);
                Integer tempo_leitura = Integer.valueOf(registro[21]);
                Integer tempo_escrita = Integer.valueOf(registro[22]);
                Logs Log = new Logs(fk_servidor, nomeMaquina, dataHoraString, cpu, ram, disco, tmp_cpu, tmp_disco, memoria_swap, qtd_processos, download_bytes, upload_bytes, pacotes_recebidos, pacotes_enviados, dropin, dropout, numero_leituras, numero_escritas, bytes_lidos, bytes_escritos, tempo_leitura, tempo_escrita);
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

    public static  List<Logs> leImportaArquivoCsvTrusted(String nomeArq){
        Reader arq = null; // objeto aquivo
        BufferedReader entrada = null; // objeto leitor de arquivo
        List<Logs> listaLogs = new ArrayList<>();

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
            System.out.println("Lendo e Importando CSV de dados do bucket-raw");

            linha = entrada.readLine();
            // converte de string para integer
            // caso fosse de string para int usa-se parseint
            while (linha != null) {

                registro = linha.split(";");
                Integer fk_servidor = Integer.valueOf(registro[0]);
                String nomeMaquina = registro[1];
                String dataHoraString = registro[2];
                Double cpu = Double.valueOf(registro[3].replace(",", "."));
                Double ram = Double.valueOf(registro[4].replace(",", "."));
                Double disco = Double.valueOf(registro[5].replace(",", "."));
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
                Logs Log = new Logs(fk_servidor, nomeMaquina, dataHoraString, cpu, ram, disco, tmp_cpu, tmp_disco, memoria_swap, qtd_processos, download_bytes, upload_bytes, pacotes_recebidos, pacotes_enviados, dropin, dropout, numero_leituras, numero_escritas, bytes_lidos, bytes_escritos, tempo_leitura, tempo_escrita);
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

    public static void main(String[] args) throws Exception {

        System.out.println("Estabelecendo conexão ao banco...");
        Connection connection = new Connection();
        JdbcTemplate con = new JdbcTemplate(connection.getDataSource());
        System.out.println("Conexão estabelecida com sucesso!\n");

        consolidarArquivosRaw(con);
        System.out.println("Lendo arquivo consolidado para iniciar tratamentos...");
        List<Logs> logsConsolidados = leImportaArquivoCsvTrusted("logs_consolidados_servidores.csv");


        // AREA TRATAMENTO MIRALHA
        TratamentoTemperaturaCpu tratarTemperatura = new TratamentoTemperaturaCpu(aws, con);
        tratarTemperatura.tratamentoDeTemperaturaCpu(logsConsolidados, "temperaturaUsoCpu");

        TratamentoTemperaturaDisco tratarTemperaturaDisco = new TratamentoTemperaturaDisco(aws, con);
        tratarTemperaturaDisco.tratamentoDeTemperaturaDisco(logsConsolidados, "temperaturaUsoDisco");

        TratamentoProcessos.consolidarArquivosRawProcessos();
        TratamentoProcessos tratarProcessos = new TratamentoProcessos(aws, con);
        tratarProcessos.tratamentoProcessos("processos_consolidados_servidores.csv");


        for (Logs log : logsConsolidados){
            Boolean idJaAdicionado = false;
            Integer idDaVez = log.getFk_servidor();
            for (int i : listaIdServidores){
                if (idDaVez == i ){
                    idJaAdicionado = true;
                }
            }
            if (!idJaAdicionado){
                listaIdServidores.add(idDaVez);
            }
        }

        // --------------- TRATAMENTO REDE ----------------------
        TratamentoRede tratamentoRede = new TratamentoRede(aws, con);
        // Criando Json de rede
        TratamentoRede.gravaArquivoJsonRede(logsConsolidados, listaIdServidores);


        // Criando json de conexao
        TratamentoRede.gravaArquivoJson(listaIdServidores);

        //Criando json Near Real Time
        tratamentoNearRealTime.logsEspecifico(logsConsolidados);

    }

}
