package org.example.classesMiralha;

import org.example.AwsConnection;
import org.example.Logs;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.File;
import java.io.FileOutputStream;
import java.time.format.DateTimeFormatter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

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
        final String arquivoJsonClient = nomeBase + ".json";

        List<LogsMiralhaCpu> listaApenasCpu = transformarParaLogsReduzidos(logsCompletos);

        System.out.println("Verificando se há um arquivo CSV anterior no Trusted para anexar dados...");
        awsConnection.downloadTemperaturaBucket(arquivoCsvTrusted);

        System.out.println("Transformando informações em um arquivo cpu_temperaturas_uso.csv... ");
        writeCsvTemperatureCPU(listaApenasCpu, nomeBase);
        System.out.println("Transformação feita com sucesso! Enviando ao bucket trusted...\n");
        awsConnection.uploadTemperaturaBucket(arquivoCsvTrusted);

        System.out.println("Agora passando o tratamento do trusted ao client...");
        writeJsonTemperaturaCpu(listaApenasCpu, nomeBase);
        System.out.println("Transferência ao client feita com sucesso! Enviando ao bucket client...\n");
        awsConnection.uploadBucketClient(arquivoJsonClient);
        System.out.println("Tratamento de CPU feito com sucesso!!\n");
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
        File arquivoLocal = new File(nomeCompletoArq);
        boolean append = arquivoLocal.exists();

        try {
            saida = new OutputStreamWriter(new FileOutputStream(nomeCompletoArq, append), StandardCharsets.UTF_8);
            if (!append) {
                saida.append("fk_servidor;timestamp;cpu;temperatura_cpu\n");
            }

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
        String nomeCompletoArq = nomeArq + ".json";

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
