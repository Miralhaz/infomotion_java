package org.example.classesMiralha;

import org.example.AwsConnection;
import org.example.Logs;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class TratamentoProcessos {
    private static final DateTimeFormatter INPUT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final JdbcTemplate jdbcTemplate;
    private final AwsConnection awsConnection;

    public TratamentoProcessos(AwsConnection awsConnection, JdbcTemplate jdbcTemplate) {
        this.awsConnection = awsConnection;
        this.jdbcTemplate = jdbcTemplate;
    }

    public void tratamentoProcessos(String nomeArqConsolidado){
        final String nomeBase = "processosTop5";
        final String arquivoCsvTrusted = nomeBase + ".csv";
        final String arquivoJsonClient = nomeBase + ".json";

        System.out.println("Iniciando tratamento de Processos...");
        awsConnection.deleteCsvLocal(nomeArqConsolidado);
        awsConnection.downloadBucketProcessosTrusted(nomeArqConsolidado);

        List<LogsProcessosMiralha> processosCompletos = leImportaArquivoCsvProcessos(nomeArqConsolidado);
        List<LogsProcessosMiralha> listaTop5Processos = transformarParaLogsTop5PorServidor(processosCompletos);

        writeCsvProcessos(listaTop5Processos, nomeBase);
        awsConnection.uploadProcessosBucket(arquivoCsvTrusted);
        writeJsonProcessos(listaTop5Processos, nomeBase);
        awsConnection.uploadProcessosBucket(arquivoJsonClient);
        System.out.println("Tratamento de Processos feito com sucesso!!");
    }

    public static List<LogsProcessosMiralha> leImportaArquivoCsvProcessos(String nomeArq) {
        Reader arq = null;
        BufferedReader entrada = null;
        List<LogsProcessosMiralha> listaProcessos = new ArrayList<>();

        try {
            arq = new InputStreamReader(new FileInputStream("/tmp/" + nomeArq), StandardCharsets.UTF_8);
            entrada = new BufferedReader(arq);
        } catch (IOException e) {
            System.out.println("Erro ao abrir o arquivo leImportaArquivoCsvProcessos: " + nomeArq);
            System.exit(1);
        }

        try {
            String cabecalho = entrada.readLine();
            System.out.println("Cabeçalho detectado: " + cabecalho);

            String linha = entrada.readLine();
            int linhaNumero = 2;

            while (linha != null) {
                if (linha.trim().isEmpty()) {
                    linha = entrada.readLine();
                    linhaNumero++;
                    continue;
                }

                String[] registro = linha.split(";");

                try {
                    Integer fkServidor;
                    String dataHoraStr;
                    String nomeProcesso;
                    Double usoCpu;
                    Double usoRam;

                    if (registro.length >= 7) {
                        fkServidor = Integer.valueOf(registro[1].trim());
                        dataHoraStr = registro[2].trim();
                        nomeProcesso = registro[3].trim();
                        usoCpu = Double.valueOf(registro[5].trim().replace(",", "."));
                        usoRam = Double.valueOf(registro[6].trim().replace(",", "."));

                    } else if (registro.length >= 5) {
                        fkServidor = Integer.valueOf(registro[0].trim());
                        dataHoraStr = registro[1].trim();
                        nomeProcesso = registro[2].trim();
                        usoCpu = Double.valueOf(registro[3].trim().replace(",", "."));
                        usoRam = Double.valueOf(registro[4].trim().replace(",", "."));

                    } else {
                        System.err.println("Linha " + linhaNumero + " ignorada por ter menos que 5 campos (" + registro.length + " campos): " + linha);
                        linha = entrada.readLine();
                        linhaNumero++;
                        continue;
                    }

                    LogsProcessosMiralha logProcesso = new LogsProcessosMiralha(
                            fkServidor,
                            dataHoraStr,
                            nomeProcesso,
                            usoCpu,
                            usoRam
                    );
                    listaProcessos.add(logProcesso);

                } catch (NumberFormatException e) {
                    System.err.println("Erro de formato numérico na linha " + linhaNumero + ": " + linha);
                    System.err.println("Detalhes: " + e.getMessage());
                } catch (Exception e) {
                    System.err.println("Erro de parsing na linha " + linhaNumero + ": " + linha);
                    System.err.println("Detalhes: " + e.getMessage());
                }

                linha = entrada.readLine();
                linhaNumero++;
            }

            System.out.println("Total de registros importados: " + listaProcessos.size());

        } catch (IOException e) {
            System.out.println("Erro ao ler o arquivo: " + nomeArq);
            e.printStackTrace();
            System.exit(1);

        } finally {
            try {
                if (entrada != null) entrada.close();
                if (arq != null) arq.close();
            } catch (IOException e) {
                System.out.println("Erro ao fechar o arquivo " + nomeArq);
            }
        }
        return listaProcessos;
    }

    public static void consolidarArquivosRawProcessos() {

        AwsConnection aws = new AwsConnection();

        final String arquivoProcessosConsolidadoTrusted = "processos_consolidados_servidores";
        final String nomeArquivoConsolidadoComExtensao = arquivoProcessosConsolidadoTrusted + ".csv";

        List<String> arquivosRawParaProcessar = aws.listarArquivosRawProcessos();
        List<LogsProcessosMiralha> logsNovosParaConsolidar = new ArrayList<>();

        if (arquivosRawParaProcessar.isEmpty()) {
            System.out.println("Nenhum arquivo RAW novo encontrado para processamento (Padrão: processos[n].csv).");
            return;
        }

        for (String chaveRaw : arquivosRawParaProcessar) {
            try {
                aws.downloadBucketRaw(chaveRaw);

                List<LogsProcessosMiralha> logsDoArquivo = leImportaArquivoCsvProcessos(chaveRaw);
                logsNovosParaConsolidar.addAll(logsDoArquivo);

                aws.deleteCsvLocal(chaveRaw);

            } catch (Exception e) {
                System.err.println("ERRO ao processar arquivo RAW " + chaveRaw + ". Pulando para o próximo: " + e.getMessage());
            }
        }

        if (logsNovosParaConsolidar.isEmpty()) {
            System.out.println("Nenhum novo log válido foi lido e consolidado. Upload para Trusted abortado.");
            return;
        }

        gravaArquivoCsvProcessos(logsNovosParaConsolidar, arquivoProcessosConsolidadoTrusted);
        aws.uploadBucketTrusted(nomeArquivoConsolidadoComExtensao);
    }

    private List<LogsProcessosMiralha> transformarParaLogsTop5PorServidor(List<LogsProcessosMiralha> logs) {
        List<LogsProcessosMiralha> todosOsTop5 = new ArrayList<>();

        Map<Integer, List<LogsProcessosMiralha>> logsPorServidor = logs.stream()
                .collect(Collectors.groupingBy(LogsProcessosMiralha::getFk_servidor));

        for (Map.Entry<Integer, List<LogsProcessosMiralha>> entry : logsPorServidor.entrySet()) {
            Integer idServidor = entry.getKey();
            List<LogsProcessosMiralha> logsDoServidor = entry.getValue();

            List<LogsProcessosMiralha> processosFiltrados = logsDoServidor.stream()
                    .filter(log -> !log.getNomeProcesso().equalsIgnoreCase("System Idle Process"))
                    .collect(Collectors.toList());

            List<LogsProcessosMiralha> picosPorProcesso = processosFiltrados.stream()
                    .collect(Collectors.groupingBy(LogsProcessosMiralha::getNomeProcesso))
                    .values().stream()
                    .map(listaLogsProcesso -> listaLogsProcesso.stream()
                            .max(Comparator.comparingDouble(LogsProcessosMiralha::getUsoCpuProcesso))
                            .orElse(null)
                    )
                    .filter(log -> log != null)
                    .collect(Collectors.toList());

            List<LogsProcessosMiralha> top5DoServidor = picosPorProcesso.stream()
                    .sorted(Comparator.comparingDouble(LogsProcessosMiralha::getUsoCpuProcesso).reversed())
                    .limit(5)
                    .collect(Collectors.toList());

            todosOsTop5.addAll(top5DoServidor);
        }

        return todosOsTop5;
    }

    private void writeCsvProcessos(List<LogsProcessosMiralha> lista, String nomeArq) {
        OutputStreamWriter saida = null;
        String nomeCompletoArq = nomeArq + ".csv";

        try {
            saida = new OutputStreamWriter(new FileOutputStream("/tmp/" + nomeCompletoArq), StandardCharsets.UTF_8);
            saida.append("fk_servidor;timestamp;nome_processo;uso_cpu;uso_ram\n");

            for (LogsProcessosMiralha log : lista) {
                saida.write(String.format(Locale.US, "%d;%s;%s;%.2f;%.2f\n",
                        log.getFk_servidor(),
                        log.getDataHoraFormatada(),
                        log.getNomeProcesso(),
                        log.getUsoCpuProcesso(),
                        log.getUsoRamProcesso()));
            }
        } catch (IOException erro) {
            System.err.println("Erro ao gravar o arquivo CSV de processos: " + erro.getMessage());
        } finally {
            try {
                if (saida != null) saida.close();
            } catch (IOException erro) {
                System.err.println("Erro ao fechar o arquivo CSV de processos.");
            }
        }
    }

    private void writeJsonProcessos(List<LogsProcessosMiralha> lista, String nomeArq) {
        OutputStreamWriter saida = null;
        String nomeCompletoArq = nomeArq + ".json";

        try {
            saida = new OutputStreamWriter(new FileOutputStream("/tmp/" + nomeCompletoArq), StandardCharsets.UTF_8);
            saida.append("[\n");

            for (int i = 0; i < lista.size(); i++) {
                LogsProcessosMiralha log = lista.get(i);

                saida.write(String.format(Locale.US,
                        """
                        {
                        "fk_servidor": %d,
                        "timestamp": "%s",
                        "nome_processo": "%s",
                        "uso_cpu": %.2f,
                        "uso_ram": %.2f
                        }""",
                        log.getFk_servidor(),
                        log.getDataHoraFormatada(),
                        log.getNomeProcesso(),
                        log.getUsoCpuProcesso(),
                        log.getUsoRamProcesso()
                ));

                if (i < lista.size() - 1) {
                    saida.append(",\n");
                } else {
                    saida.append("\n");
                }
            }
            saida.append("]");

        } catch (IOException erro) {
            System.err.println("Erro ao gravar o arquivo JSON de processos: " + erro.getMessage());
        } finally {
            try {
                if (saida != null) saida.close();
            } catch (IOException erro) {
                System.err.println("Erro ao fechar o arquivo JSON de processos.");
            }
        }
    }

    public static void gravaArquivoCsvProcessos(List<LogsProcessosMiralha> lista, String nomeArq){
        OutputStreamWriter saida = null;
        Boolean deuRuim = false;
        nomeArq += ".csv";

        try {
            saida = new OutputStreamWriter(new FileOutputStream("/tmp/" + nomeArq), StandardCharsets.UTF_8);

        }catch (IOException erro){
            System.out.println("Erro ao abrir o arquivo gravaArquivoCsv");
            System.exit(1);
        }

        try {
            saida.append("fk_servidor;timestamp;nome_processo;uso_cpu;uso_ram\n");

            for (LogsProcessosMiralha log : lista){
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

                saida.write(String.format(Locale.US, "%d;%s;%s;%.2f;%.2f\n",
                        log.getFk_servidor(),
                        log.getDataHora().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                        log.getNomeProcesso(),
                        log.getUsoCpuProcesso(),
                        log.getUsoRamProcesso()));
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
}