package org.example.classesMiralha;

import org.example.AwsConnection;
import org.example.Logs;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class TratamentoProcessos {
    private static final DateTimeFormatter INPUT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final JdbcTemplate jdbcTemplate;
    private final AwsConnection awsConnection;

    public TratamentoProcessos(AwsConnection awsConnection, JdbcTemplate jdbcTemplate) {
        this.awsConnection = awsConnection;
        this.jdbcTemplate = jdbcTemplate;
    }

    public void tratamentoProcessos(String nomeArq){
        final String nomeBase = "processosTop5";
        final String arquivoCsvTrusted = nomeBase + ".csv";
        final String arquivoJsonClient = nomeBase + ".json";

        List<LogsProcessosMiralha> processosCompletos = lerCsvProcessosDoBucket(nomeArq);
        List<LogsProcessosMiralha> listaTop5Processos = transformarParaLogsTop5(processosCompletos);

        writeCsvProcessos(listaTop5Processos, nomeBase);
        awsConnection.uploadProcessosBucket(arquivoCsvTrusted);
        writeJsonProcessos(listaTop5Processos, nomeBase);
        awsConnection.uploadProcessosBucket(arquivoJsonClient);
    }

    private List<LogsProcessosMiralha> lerCsvProcessosDoBucket(String nomeArqProcessosEntrada) {

        List<LogsProcessosMiralha> listaCompleta = new ArrayList<>();

        System.out.println("Baixando arquivo de processos: " + nomeArqProcessosEntrada);
        awsConnection.downloadBucket(nomeArqProcessosEntrada);

        try (BufferedReader br = new BufferedReader(new FileReader(nomeArqProcessosEntrada, StandardCharsets.UTF_8))) {
            String linha;
            br.readLine();

            while ((linha = br.readLine()) != null) {

                String[] dados = linha.split(";");

                if (dados.length >= 5) {
                    try {
                        Integer fkServidor = Integer.parseInt(dados[1].trim());
                        String dataHoraStr = dados[2].trim();
                        String nomeProcesso = dados[3].trim();
                        Double usoCpu = Double.parseDouble(dados[5].trim());
                        Double usoRam = Double.parseDouble(dados[6].trim());

                        LogsProcessosMiralha logProcesso = new LogsProcessosMiralha(
                                fkServidor,
                                dataHoraStr,
                                nomeProcesso,
                                usoCpu,
                                usoRam
                        );
                        listaCompleta.add(logProcesso);

                    } catch (NumberFormatException e) {
                        System.err.println("Erro de formato de número na linha: " + linha + " -> " + e.getMessage());
                    }
                }
            }
        } catch (IOException erro) {
            System.err.println("Erro ao ler o arquivo CSV de processos: " + erro.getMessage());
        }

        System.out.println("Leitura do arquivo de processos concluída.");
        return listaCompleta;
    }

    private List<LogsProcessosMiralha> transformarParaLogsTop5(List<LogsProcessosMiralha> logs) {

        // 1. Filtrar o 'System Idle Process'
        List<LogsProcessosMiralha> processosFiltrados = logs.stream()
                .filter(log -> !log.getNomeProcesso().equalsIgnoreCase("System Idle Process"))
                .collect(Collectors.toList());

        // 2. Agrupar por Nome do Processo e encontrar o LOG que representa o PICO de consumo de CPU.
        // O critério de pico agora é APENAS o maior uso de CPU.
        List<LogsProcessosMiralha> picosPorProcesso = processosFiltrados.stream()
                .collect(Collectors.groupingBy(LogsProcessosMiralha::getNomeProcesso))
                .values().stream()
                .map(listaLogsProcesso -> listaLogsProcesso.stream()
                        // Acha o MÁXIMO (PICO) usando o consumo de CPU
                        .max(Comparator.comparingDouble(LogsProcessosMiralha::getUsoCpuProcesso))
                        .orElse(null)
                )
                .filter(log -> log != null)
                .collect(Collectors.toList());

        // 3. Aplicar a ordenação estrita por CPU e limitar ao Top 5
        List<LogsProcessosMiralha> reduzidosTop5 = picosPorProcesso.stream()
                .sorted(
                        // Critério ÚNICO: CPU (Decrescente) - O maior CPU vem primeiro
                        Comparator.comparingDouble(LogsProcessosMiralha::getUsoCpuProcesso).reversed()
                )
                .limit(5)
                .collect(Collectors.toList());

        System.out.println("Filtro Top 5 (Apenas CPU) aplicado com sucesso!\n");
        return reduzidosTop5;
    }

    private void writeCsvProcessos(List<LogsProcessosMiralha> lista, String nomeArq) {
        OutputStreamWriter saida = null;
        String nomeCompletoArq = nomeArq + ".csv";

        try {
            saida = new OutputStreamWriter(new FileOutputStream(nomeCompletoArq), StandardCharsets.UTF_8);
            saida.append("fk_servidor;timestamp;nome_processo;uso_cpu;uso_ram\n");

            for (LogsProcessosMiralha log : lista) {
                saida.write(String.format("%d;%s;%s;%.2f;%.2f\n",
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
            saida = new OutputStreamWriter(new FileOutputStream(nomeCompletoArq), StandardCharsets.UTF_8);
            saida.append("[\n");

            for (int i = 0; i < lista.size(); i++) {
                LogsProcessosMiralha log = lista.get(i);

                saida.write(String.format(
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
}
