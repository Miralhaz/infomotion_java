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
        final String arquivoJsonClient = nomeBase + ".json";

        List<LogsMiralhaDisco> listaApenasDisco = transformarParaLogsReduzidos(logsCompletos);

        System.out.println("Verificando se há um arquivo CSV anterior no Trusted para anexar dados...");
        awsConnection.downloadTemperaturaBucket(arquivoCsvTrusted);

        System.out.println("Transformando informações em um arquivo disco_temperaturas_uso.csv... ");
        writeCsvTemperatureCPU(listaApenasDisco, nomeBase);
        System.out.println("Transformação feita com sucesso! Enviando ao bucket trusted...\n");
        awsConnection.uploadTemperaturaBucket(arquivoCsvTrusted);

        System.out.println("Agora passando o tratamento do trusted ao client...");
        writeJsonTemperaturaCpu(listaApenasDisco, nomeBase);
        System.out.println("Transferência ao client feita com sucesso! Enviando ao bucket client...\n");
        awsConnection.uploadBucketClient(arquivoJsonClient);
        System.out.println("Tratamento de Disco feito com sucesso!!\n");
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

    private void writeCsvTemperatureCPU(List<LogsMiralhaDisco> lista, String nomeArq) {
        OutputStreamWriter saida = null;
        String nomeCompletoArq = nomeArq + ".csv";
        File arquivoLocal = new File(nomeCompletoArq);
        boolean append = arquivoLocal.exists();

        try {
            saida = new OutputStreamWriter(new FileOutputStream(nomeCompletoArq, append), StandardCharsets.UTF_8);
            if (!append) {
                saida.append("fk_servidor;timestamp;disco;temperatura_disco\n");
            }

            for (LogsMiralhaDisco log : lista) {
                saida.write(String.format("%d;%s;%.2f;%.2f\n",
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

    private void writeJsonTemperaturaCpu(List<LogsMiralhaDisco> lista, String nomeArq) {
        OutputStreamWriter saida = null;
        String nomeCompletoArq = nomeArq + ".json";

        try {
            saida = new OutputStreamWriter(new FileOutputStream(nomeCompletoArq), StandardCharsets.UTF_8);
            saida.append("[\n");

            for (int i = 0; i < lista.size(); i++) {
                LogsMiralhaDisco log = lista.get(i);

                saida.write(String.format(
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
