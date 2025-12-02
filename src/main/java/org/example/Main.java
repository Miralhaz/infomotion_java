package org.example;

import org.example.classesGiulia.TratamentoBolhas;
import org.example.classesGiulia.TratamentoDonut;
import org.example.classesGiulia.TratamentoHistorico;
import org.example.classesGiulia.TratamentoHistoricoServidor;
import org.example.classesMiralha.TratamentoProcessos;
import org.example.classesMiralha.TratamentoTemperaturaCpu;
import org.example.classesMiralha.TratamentoTemperaturaDisco;
import org.example.classesRede.TratamentoRede;
import org.example.classesRegiao.TratamentoClima;
import org.example.classesRenan.tratamentoNearRealTime;
import org.example.classesWillian.ProcessadorDiscoWillian;
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

        aws.downloadBucketTrusted(arquivoConsolidadoTrusted + ".csv");

        List<Logs> logsConsolidadosExistentes = new ArrayList<>();
        File arquivoLocal = new File(arquivoConsolidadoTrusted + ".csv");

        if (arquivoLocal.exists()) {
            try {
                logsConsolidadosExistentes = leImportaArquivoCsv(arquivoConsolidadoTrusted + ".csv");
                System.out.printf("Arquivo consolidado existente possui %d logs.\n", logsConsolidadosExistentes.size());
            } catch (Exception e) {
                System.err.println("Erro ao ler arquivo consolidado existente: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("Nenhum arquivo consolidado anterior encontrado. Iniciando novo arquivo.");
        }

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

                Integer fk_servidor = logsDoArquivo.get(1).getFk_servidor();
                try {
                    TratamentoAlertas.TratamentoAlertas(con, fk_servidor, logsDoArquivo, aws);
                    logsNovosParaConsolidar.addAll(logsDoArquivo);
                } catch (Exception e) {
                    System.out.println("Erro no tratamento de alertas");
                    e.printStackTrace();
                }
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

        logsConsolidadosExistentes.addAll(logsNovosParaConsolidar);
        System.out.printf("Total de logs após consolidação: %d\n", logsConsolidadosExistentes.size());

        gravaArquivoCsv(logsConsolidadosExistentes, arquivoConsolidadoTrusted, false);

        aws.uploadBucketTrusted(arquivoConsolidadoTrusted + ".csv");

        System.out.println("\nConsolidação concluída");
    }

    public static void consolidarEspecificacoesRaw(JdbcTemplate con) {
        final String arquivoEspecificadosConsolidadoTrusted = "logs_especificados_consolidados_servidores";

        System.out.println("\nIniciando consolidação...");

        aws.downloadTemperaturaBucket(arquivoEspecificadosConsolidadoTrusted + ".csv");

        List<String> arquivosRawParaProcessar = aws.listarEspecificacoesRaw();
        List<LogsEspecificacoes> logsNovosParaConsolidar = new ArrayList<>();

        if (arquivosRawParaProcessar.isEmpty()) {
            System.out.println("Nenhum arquivo RAW novo encontrado para processamento (Padrão: data[n].csv).");
            return;
        }

        System.out.printf("Encontrados %d arquivos RAW para processar.\n", arquivosRawParaProcessar.size());

        for (String chaveRaw : arquivosRawParaProcessar) {
            try {
                aws.downloadBucketRaw(chaveRaw);
                List<LogsEspecificacoes> logsDoArquivo = leEspecificacoesCsv(chaveRaw);

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

        gravaArquivoCsvEspecificacoes(logsNovosParaConsolidar, arquivoEspecificadosConsolidadoTrusted);
        aws.uploadBucketTrusted(arquivoEspecificadosConsolidadoTrusted + ".csv");


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

            String[] header = linha.split(";");
            int offset = (header[0].isEmpty() || header[0].matches("\\d+")) ? 1 : 0;

            System.out.println("Lendo e Importando CSV de dados (offset detectado: " + offset + ")");

            linha = entrada.readLine();

            while (linha != null) {
                registro = linha.split(";");

                // USA O OFFSET DETECTADO
                Integer fk_servidor = Integer.valueOf(registro[offset + 0]);
                String nomeMaquina = registro[offset + 1];
                String dataHoraString = registro[offset + 2];
                Double cpu = Double.valueOf(registro[offset + 3].replace(",", "."));
                Double ram = Double.valueOf(registro[offset + 4].replace(",", "."));
                Double disco = Double.valueOf(registro[offset + 5].replace(",", "."));
                Double tmp_cpu = Double.valueOf(registro[offset + 6].replace(",", "."));
                Double tmp_disco = Double.valueOf(registro[offset + 7].replace(",", "."));
                Double memoria_swap = Double.valueOf(registro[offset + 8].replace(",", "."));
                Integer qtd_processos = Integer.valueOf(registro[offset + 9]);
                Long download_bytes = Long.valueOf(registro[offset + 10]);
                Long upload_bytes = Long.valueOf(registro[offset + 11]);
                Long pacotes_recebidos = Long.valueOf(registro[offset + 12]);
                Long pacotes_enviados = Long.valueOf(registro[offset + 13]);
                Integer dropin = Integer.valueOf(registro[offset + 14]);
                Integer dropout = Integer.valueOf(registro[offset + 15]);
                Long numero_leituras = Long.valueOf(registro[offset + 16]);
                Long numero_escritas = Long.valueOf(registro[offset + 17]);
                Long bytes_lidos = Long.valueOf(registro[offset + 18]);
                Long bytes_escritos = Long.valueOf(registro[offset + 19]);
                Integer tempo_leitura = Integer.valueOf(registro[offset + 20]);
                Integer tempo_escrita = Integer.valueOf(registro[offset + 21]);

                Logs Log = new Logs(fk_servidor, nomeMaquina, dataHoraString, cpu, ram, disco,
                        tmp_cpu, tmp_disco, memoria_swap, qtd_processos,
                        download_bytes, upload_bytes, pacotes_recebidos, pacotes_enviados,
                        dropin, dropout, numero_leituras, numero_escritas,
                        bytes_lidos, bytes_escritos, tempo_leitura, tempo_escrita);
                listaLogs.add(Log);
                linha = entrada.readLine();
            }
        } catch (IOException e) {
            System.out.println("Erro ao ler o arquivo");
            e.printStackTrace();
            System.exit(1);
        } finally {
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

    public static void gravaArquivoCsvEspecificacoes(List<LogsEspecificacoes> lista, String nomeArq) {
        OutputStreamWriter saida = null;
        boolean deuRuim = false;
        nomeArq += ".csv";

        try {
            saida = new OutputStreamWriter(new FileOutputStream(nomeArq), StandardCharsets.UTF_8);
        } catch (IOException erro) {
            System.out.println("Erro ao abrir o arquivo gravaArquivoCsvEspecificacoes");
            System.exit(1);
        }

        try {
            // Cabeçalho
            saida.append("fk_servidor;swap_total;ram_total;quantidade_cpus;quantidade_nucleos;capacidade_total_disco;qtd_particoes;particoes;data_hora\n");

            // Conteúdo
            for (LogsEspecificacoes log : lista) {

                // Montar concatenação das partições (ex: "C:\\: 88.7% | D:\\: 34.3% | E:\\: 12.4%")
                StringBuilder sbParticoes = new StringBuilder();

                for (int i = 0; i < log.getParticoes().size(); i++) {
                    Particao p = log.getParticoes().get(i);

                    sbParticoes.append(p.getNome())
                            .append(": ")
                            .append(p.getUso())
                            .append("%");

                    if (i < log.getParticoes().size() - 1) {
                        sbParticoes.append(" | ");
                    }
                }

                saida.write(String.format(Locale.US,
                        "%d;%.2f;%.2f;%d;%d;%.2f;%d;%s;%s\n",
                        log.getFkServidor(),
                        log.getSwapTotal(),
                        log.getRamTotal(),
                        log.getQuantidadeCpus(),
                        log.getQuantidadeNucleos(),
                        log.getCapacidadeTotalDisco(),
                        log.getParticoes().size(),
                        sbParticoes.toString(),
                        log.getDataHora()
                ));
            }

        } catch (IOException erro) {
            System.out.println("Erro ao gravar o arquivo");
            erro.printStackTrace();
            deuRuim = true;
        } finally {
            try {
                if (saida != null) saida.close();
            } catch (IOException erro) {
                System.out.println("Erro ao fechar o arquivo");
                deuRuim = true;
            }

            if (deuRuim) System.exit(1);
        }
    }



    public static List<LogsEspecificacoes> leEspecificacoesCsv(String nomeArq) {
        Reader arq = null;
        BufferedReader entrada = null;
        List<LogsEspecificacoes> lista = new ArrayList<>();

        try {
            arq = new InputStreamReader(new FileInputStream(nomeArq), StandardCharsets.UTF_8);
            entrada = new BufferedReader(arq);
        } catch (IOException e) {
            System.out.println("Erro ao abrir arquivo de especificações");
            System.exit(1);
        }

        try {
            String linha = entrada.readLine(); // cabeçalho

            linha = entrada.readLine(); // primeira linha útil

            while (linha != null) {

                String[] registro = linha.split(";");

                Integer fkServidor = Integer.valueOf(registro[1]);
                Double swap = Double.valueOf(registro[2].replace(",", "."));
                Double ram = Double.valueOf(registro[3].replace(",", "."));
                Integer qtdCpus = Integer.valueOf(registro[4]);
                Integer qtdNucleos = Integer.valueOf(registro[5]);
                Double capacidadeDisco = Double.valueOf(registro[6].replace(",", "."));
                Double qtdParticoes = Double.valueOf(registro[7].replace(",", "."));

                String campoParticoes = registro[8];
                String dataHora = registro[9];

                List<Particao> listaParticoes = new ArrayList<>();

                String[] particoes = campoParticoes.split("\\|");

                for (String p : particoes) {

                    if (p == null || p.isBlank()) continue;

                    String[] partes = p.split(":", 2);
                    if (partes.length < 2) continue;

                    String nome = partes[0].trim();
                    String valorStr = partes[1].replace("%", "").trim();

                    try {
                        // remover \: e qualquer lixo
                        valorStr = valorStr.replaceAll("[^0-9.,]", "");

                        double uso = Double.parseDouble(valorStr.replace(",", "."));
                        listaParticoes.add(new Particao(nome, uso));

                    } catch (Exception e) {
                        System.err.println("Partição inválida: '" + nome + "' (valor='" + valorStr + "')");
                    }
                }
                List<String> listaStr = listaParticoes.stream()
                        .map(p -> p.getNome() + ": " + p.getUso() + "%")
                        .toList();

                LogsEspecificacoes log = new LogsEspecificacoes(
                        fkServidor, swap, ram, qtdCpus, qtdNucleos,
                        capacidadeDisco, qtdParticoes, listaStr, dataHora
                );


                lista.add(log);

                linha = entrada.readLine();
            }

        } catch (Exception e) {
            System.out.println("Erro ao ler especificações: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                entrada.close();
                arq.close();
            } catch (IOException e) {
                System.out.println("Erro ao fechar arquivo");
            }
        }

        return lista;
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

        // Tratamento de alertas
        List<String> arquivosRaw = aws.listarArquivosRaw();

        for (String nomeArquivo : arquivosRaw) {
            System.out.println("\nProcessando: " + nomeArquivo);
            aws.downloadBucketRaw(nomeArquivo);

            List<Logs> logsDoArquivo = leImportaArquivoCsv(nomeArquivo);

            if (logsDoArquivo.isEmpty()) {
                System.out.println("Arquivo sem registros, continuando...");
                continue;
            }

            Integer fk_servidor_arquivo = logsDoArquivo.get(0).getFk_servidor();
            TratamentoCardsServidores.atualizarStatusServidor(fk_servidor_arquivo, logsDoArquivo, aws, con);
        }
        aws.limparTemporarios();
        // Fim da área tratamento alertas


        consolidarArquivosRaw(con);
        consolidarEspecificacoesRaw(con);
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
        // FIM DA ÁREA TRATAMENTO MIRALHA

        aws.limparTemporarios();
        // comeco tratamento willian
        // Na main da equipe, apó  s instanciar aws e db:
        ProcessadorDiscoWillian tratamentoDisco = new ProcessadorDiscoWillian(aws, connection);
        tratamentoDisco.executarTratamento();
        // final tratamento willian

            for (Logs log : logsConsolidados) {
                Boolean idJaAdicionado = false;
                Integer idDaVez = log.getFk_servidor();
                for (int i : listaIdServidores) {
                    if (idDaVez == i) {
                        idJaAdicionado = true;
                    }
                }
                if (!idJaAdicionado) {
                    listaIdServidores.add(idDaVez);
                }
            }
            //AREA TRATAMENTO DERECK
        System.out.println("--------------------iniciando tratamento regiao------------------");
            TratamentoClima tratamentoClima = new TratamentoClima(aws,con);
            tratamentoClima.buscarRegioes(con,logsConsolidados);

        //FIM AREA TRATAMENTO DERECK
            // Pegando o id do servidor
            Integer fk_servidor = logsConsolidados.get(1).getFk_servidor();

            // TRATAMENTO REDE
            // Criando Json de rede
            TratamentoRede.gravaArquivoJsonRede(logsConsolidados, listaIdServidores, con, aws);
            // Criando json de conexao
            TratamentoRede.gravaArquivoJson(listaIdServidores, aws);

            // TRATAMENTO - GIULIA
            TratamentoDonut tratamentoDonut = new TratamentoDonut(aws, con);
            tratamentoDonut.classificarCriticidade(logsConsolidados);

            TratamentoBolhas tratamentoBolhas = new TratamentoBolhas(aws, con);
            tratamentoBolhas.gerarBolhasCpu(logsConsolidados);
            tratamentoBolhas.gerarBolhasRam(logsConsolidados);
            tratamentoBolhas.gerarBolhasDisco(logsConsolidados);
            tratamentoBolhas.gerarBolhasTempCpu(logsConsolidados);
            tratamentoBolhas.gerarBolhasTempDisco(logsConsolidados);

            TratamentoHistorico tratamentoHistorico = new TratamentoHistorico(aws, con);
            tratamentoHistorico.classificarAlertas(logsConsolidados, 1);
            tratamentoHistorico.classificarAlertas(logsConsolidados, 7);
            tratamentoHistorico.classificarAlertas(logsConsolidados, 30);

            TratamentoHistoricoServidor tratamentoHistoricoServidor = new TratamentoHistoricoServidor(aws, con);
            tratamentoHistoricoServidor.classificarAlertas(logsConsolidados, 1);
            tratamentoHistoricoServidor.classificarAlertas(logsConsolidados, 7);
            tratamentoHistoricoServidor.classificarAlertas(logsConsolidados, 30);

            //Criando json Near Real Time
            tratamentoNearRealTime.logsEspecifico(logsConsolidados);

            aws.limparTemporarios();
        }
    }