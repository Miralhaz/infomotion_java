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

public class TratamentoTemperaturaDisco {
    private static final DateTimeFormatter INPUT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final JdbcTemplate jdbcTemplate;
    private final AwsConnection awsConnection;

    public TratamentoTemperaturaDisco(AwsConnection awsConnection, JdbcTemplate jdbcTemplate) {
        this.awsConnection = awsConnection;
        this.jdbcTemplate = jdbcTemplate;
    }

    public void tratamentoDeTemperaturaDisco(List<Logs> logsCompletos, String nomeArqSaida){
        final String nomeBase = "disco_temperaturas_uso";
        final String arquivoCsvTrusted = nomeBase + ".csv";

        List<LogsMiralhaDisco> listaApenasDisco = transformarParaLogsReduzidos(logsCompletos);

        System.out.println("Transformando informações em um arquivo disco_temperaturas_uso.csv... ");
        writeCsvTemperaturaDisco(listaApenasDisco, nomeBase);
        System.out.println("Transformação feita com sucesso! Enviando ao bucket trusted...\n");
        awsConnection.uploadDiscoBucket(arquivoCsvTrusted);

        System.out.println("Separando logs por servidor e gerando JSONs filtrados...");
        gerarJsonsPorServidorEPeriodo(listaApenasDisco);

        System.out.println("Tratamento de Disco feito com sucesso!!\n");
    }

    private void gerarJsonsPorServidorEPeriodo(List<LogsMiralhaDisco> logs) {
        LocalDateTime agora = LocalDateTime.now();

        Map<Integer, List<LogsMiralhaDisco>> logsPorServidor = logs.stream()
                .collect(Collectors.groupingBy(LogsMiralhaDisco::getFk_servidor));

        System.out.println("Total de servidores encontrados: " + logsPorServidor.size() + "\n");

        for (Map.Entry<Integer, List<LogsMiralhaDisco>> entry : logsPorServidor.entrySet()) {
            Integer idServidor = entry.getKey();
            List<LogsMiralhaDisco> logsDoServidor = entry.getValue();

            System.out.println("=== Processando Servidor ID: " + idServidor + " ===");

            List<LogsMiralhaDisco> ultimaHora = filtrarPorPeriodo(logsDoServidor, agora.minusHours(1), agora);
            String arquivo1h = "ultima_hora.json";
            writeJsonTemperaturaDisco(ultimaHora, arquivo1h);
            awsConnection.uploadDiscoComPasta(idServidor, arquivo1h);
            System.out.println("  ✓ Última hora: " + ultimaHora.size() + " registros");

            List<LogsMiralhaDisco> ultimas24h = filtrarPorPeriodo(logsDoServidor, agora.minusHours(24), agora);
            String arquivo24h = "24_horas.json";
            writeJsonTemperaturaDisco(ultimas24h, arquivo24h);
            awsConnection.uploadDiscoComPasta(idServidor, arquivo24h);
            System.out.println("  ✓ Últimas 24h: " + ultimas24h.size() + " registros");

            List<LogsMiralhaDisco> ultimos7dias = filtrarPorPeriodo(logsDoServidor, agora.minusDays(7), agora);
            String arquivo7d = "7_dias.json";
            writeJsonTemperaturaDisco(ultimos7dias, arquivo7d);
            awsConnection.uploadDiscoComPasta(idServidor, arquivo7d);
            System.out.println("  ✓ Últimos 7 dias: " + ultimos7dias.size() + " registros\n");
        }
    }

    private List<LogsMiralhaDisco> filtrarPorPeriodo(List<LogsMiralhaDisco> logs, LocalDateTime inicio, LocalDateTime fim) {
        return logs.stream()
                .filter(log -> {
                    LocalDateTime dataLog = log.getDataHora();
                    return !dataLog.isBefore(inicio) && !dataLog.isAfter(fim);
                })
                .collect(Collectors.toList());
    }

    private List<LogsMiralhaDisco> transformarParaLogsReduzidos(List<Logs> logs) {
        List<LogsMiralhaDisco> reduzidos = new ArrayList<>();
        System.out.println("\nTransformando para logs de temperatura / uso Disco...");

        for (Logs log : logs) {
            String dataHoraStr = log.getDataHora().format(INPUT_FORMATTER);

            LogsMiralhaDisco logReduzido = new LogsMiralhaDisco(
                    log.getFk_servidor(),
                    dataHoraStr,
                    log.getDisco(),
                    log.getTmp_disco()
            );
            reduzidos.add(logReduzido);
        }
        System.out.println("Arquivo tratado com sucesso!!\n");
        return reduzidos;
    }

    private void writeCsvTemperaturaDisco(List<LogsMiralhaDisco> lista, String nomeArq) {
        OutputStreamWriter saida = null;
        String nomeCompletoArq = nomeArq + ".csv";

        try {
            saida = new OutputStreamWriter(new FileOutputStream(nomeCompletoArq), StandardCharsets.UTF_8);

            for (LogsMiralhaDisco log : lista) {
                saida.write(String.format(Locale.US, "%d;%s;%.2f;%.2f\n",
                        log.getFk_servidor(), log.getDataHoraFormatada(), log.getUsoDisco(), log.getTempDisco()));
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

    private void writeJsonTemperaturaDisco(List<LogsMiralhaDisco> lista, String nomeArq) {
        OutputStreamWriter saida = null;
        String nomeCompletoArq = nomeArq;

        if (!nomeCompletoArq.endsWith(".json")) {
            nomeCompletoArq += ".json";
        }

        try {
            saida = new OutputStreamWriter(new FileOutputStream(nomeCompletoArq), StandardCharsets.UTF_8);
            saida.append("[\n");

            for (int i = 0; i < lista.size(); i++) {
                LogsMiralhaDisco log = lista.get(i);

                saida.write(String.format(Locale.US,
                        """
                        {
                        "fk_servidor": %d,
                        "timestamp": "%s",
                        "disco_uso": %.2f,
                        "temperatura_disco": %.2f
                        }""",
                        log.getFk_servidor(),
                        log.getDataHoraFormatada(),
                        log.getUsoDisco(),
                        log.getTempDisco()
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