package org.example.classesMiralha;

import org.example.AwsConnection;
import org.example.Logs;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class TratamentoTemperaturaCpu {
    private static final DateTimeFormatter INPUT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final JdbcTemplate jdbcTemplate;
    private final AwsConnection awsConnection;

    public TratamentoTemperaturaCpu(AwsConnection awsConnection, JdbcTemplate jdbcTemplate) {
        this.awsConnection = awsConnection;
        this.jdbcTemplate = jdbcTemplate;
    }

    public void tratamentoDeTemperaturaCpu(List<Logs> logsCompletos, String nomeArqSaida){
        final String nomeBase = "cpu_temperaturas_uso";
        final String arquivoCsvTrusted = nomeBase + ".csv";

        List<LogsMiralhaCpu> listaApenasCpu = transformarParaLogsReduzidos(logsCompletos);

        System.out.println("Transformando informações em um arquivo cpu_temperaturas_uso.csv...\n ");
        writeCsvTemperatureCPU(listaApenasCpu, nomeBase);
        System.out.println("Transformação feita com sucesso! Enviando ao bucket trusted...\n");
        awsConnection.uploadTemperaturaBucket(arquivoCsvTrusted);

        System.out.println("Separando logs por servidor e gerando JSONs filtrados...");
        gerarJsonsPorServidorEPeriodo(listaApenasCpu);

        System.out.println("Tratamento de CPU feito com sucesso!!\n");
    }

    private void gerarJsonsPorServidorEPeriodo(List<LogsMiralhaCpu> logs) {
        LocalDateTime agora = LocalDateTime.now();

        Map<Integer, List<LogsMiralhaCpu>> logsPorServidor = logs.stream()
                .collect(Collectors.groupingBy(LogsMiralhaCpu::getFk_servidor));

        System.out.println("Total de servidores encontrados: " + logsPorServidor.size() + "\n");

        for (Map.Entry<Integer, List<LogsMiralhaCpu>> entry : logsPorServidor.entrySet()) {
            Integer idServidor = entry.getKey();
            List<LogsMiralhaCpu> logsDoServidor = entry.getValue();

            System.out.println("=== Processando Servidor ID: " + idServidor + " ===");

            List<LogsMiralhaCpu> ultimaHora = filtrarPorPeriodo(logsDoServidor, agora.minusHours(1), agora);
            String arquivo1h = "ultima_hora.json";
            writeJsonTemperaturaCpu(ultimaHora, arquivo1h);
            awsConnection.uploadTemperaturaComPasta(idServidor, arquivo1h);
            System.out.println("  ✓ Última hora: " + ultimaHora.size() + " registros");

            // 24 HORAS
            List<LogsMiralhaCpu> ultimas24h = filtrarPorPeriodo(logsDoServidor, agora.minusHours(24), agora);
            String arquivo24h = "24_horas.json";
            writeJsonTemperaturaCpu(ultimas24h, arquivo24h);
            awsConnection.uploadTemperaturaComPasta(idServidor, arquivo24h);
            System.out.println("  ✓ Últimas 24h: " + ultimas24h.size() + " registros");

            // 7 DIAS
            List<LogsMiralhaCpu> ultimos7dias = filtrarPorPeriodo(logsDoServidor, agora.minusDays(7), agora);
            String arquivo7d = "7_dias.json";
            writeJsonTemperaturaCpu(ultimos7dias, arquivo7d);
            awsConnection.uploadTemperaturaComPasta(idServidor, arquivo7d);
            System.out.println("  ✓ Últimos 7 dias: " + ultimos7dias.size() + " registros\n");
        }
    }

    private List<LogsMiralhaCpu> filtrarPorPeriodo(List<LogsMiralhaCpu> logs, LocalDateTime inicio, LocalDateTime fim) {
        return logs.stream()
                .filter(log -> {
                    LocalDateTime dataLog = log.getDataHora();
                    return !dataLog.isBefore(inicio) && !dataLog.isAfter(fim);
                })
                .collect(Collectors.toList());
    }

    private List<LogsMiralhaCpu> transformarParaLogsReduzidos(List<Logs> logs) {
        List<LogsMiralhaCpu> reduzidos = new ArrayList<>();
        System.out.println("\nTransformando para logs de temperatura / uso CPU...");

        for (Logs log : logs) {
            String dataHoraStr = log.getDataHora().format(INPUT_FORMATTER);

            LogsMiralhaCpu logReduzido = new LogsMiralhaCpu(
                    log.getFk_servidor(),
                    dataHoraStr,
                    log.getCpu(),
                    log.getTmp_cpu()
            );
            reduzidos.add(logReduzido);
        }
        System.out.println("Arquivo tratado com sucesso!!\n");
        return reduzidos;
    }

    private void writeCsvTemperatureCPU(List<LogsMiralhaCpu> lista, String nomeArq) {
        OutputStreamWriter saida = null;
        String nomeCompletoArq = nomeArq + ".csv";

        try {
            saida = new OutputStreamWriter(new FileOutputStream(nomeCompletoArq), StandardCharsets.UTF_8);

            for (LogsMiralhaCpu log : lista) {
                saida.write(String.format("%d;%s;%.2f;%.2f\n",
                        log.getFk_servidor(), log.getDataHoraFormatada(), log.getUsoCpu(), log.getTempCpu()));
            }
        } catch (IOException erro) {
            System.err.println("Erro ao gravar o arquivo CSV de saída: " + erro.getMessage());
        } finally {
            try {
                if (saida != null) saida.close();
            } catch (IOException erro) {
                System.err.println("Erro ao fechar o arquivo CSV de saída.");
            }
        }
    }

    private void writeJsonTemperaturaCpu(List<LogsMiralhaCpu> lista, String nomeArq) {
        OutputStreamWriter saida = null;
        String nomeCompletoArq = nomeArq;

        // Adiciona .json apenas se não tiver
        if (!nomeCompletoArq.endsWith(".json")) {
            nomeCompletoArq += ".json";
        }

        try {
            saida = new OutputStreamWriter(new FileOutputStream(nomeCompletoArq), StandardCharsets.UTF_8);
            saida.append("[\n");

            for (int i = 0; i < lista.size(); i++) {
                LogsMiralhaCpu log = lista.get(i);

                saida.write(String.format(
                        """
                        {
                        "fk_servidor": %d,
                        "timestamp": "%s",
                        "cpu_uso": %.2f,
                        "temperatura_cpu": %.2f
                        }""",
                        log.getFk_servidor(),
                        log.getDataHoraFormatada(),
                        log.getUsoCpu(),
                        log.getTempCpu()
                ));

                if (i < lista.size() - 1) {
                    saida.append(",\n");
                } else {
                    saida.append("\n");
                }
            }
            saida.append("]");

        } catch (IOException erro) {
            System.err.println("Erro ao gravar o arquivo JSON de saída: " + erro.getMessage());
        } finally {
            try {
                if (saida != null) saida.close();
            } catch (IOException erro) {
                System.err.println("Erro ao fechar o arquivo JSON de saída.");
            }
        }
    }
}