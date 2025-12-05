package org.example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.*;

public class LambdaHandler implements RequestHandler<Map<String, Object>, Map<String, Object>>{
    private static final Logger log = LoggerFactory.getLogger(LambdaHandler.class);
    private static Connection con;
    private static AwsConnection aws;
    private static JdbcTemplate jdbcTemplate;

    static {
        try {
            System.out.println("Conectando informações");
            con = new Connection();
            jdbcTemplate = new JdbcTemplate(con.getDataSource());
            aws = new AwsConnection();
            System.out.println("Conexões feitas");
        } catch (Exception e){
            System.err.println("Erro nas conexões: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void executarETL(LambdaLogger logger) throws Exception {
        logger.log("Processo de alertas...\n");

        List<String> arquivosRaw = aws.listarArquivosRaw();

        for (String nomeArq : arquivosRaw){
            logger.log("Processando arquivo " + nomeArq + "\n");
            aws.downloadBucketRaw(nomeArq);

            List<Logs> logsArquivoNovo = Main.leImportaArquivoCsv(nomeArq);

            if (logsArquivoNovo.isEmpty()){
                logger.log("Arquivo " + nomeArq + "sem registros\n");
                continue;
            }

            Integer fk_servidor = logsArquivoNovo.get(1).getFk_servidor();
            TratamentoCardsServidores.atualizarStatusServidor(
                    fk_servidor, logsArquivoNovo, aws, jdbcTemplate
            );
        }
        aws.limparTemporarios();

        logger.log("Iniciando consolidação");
        Main.consolidarArquivosRaw(jdbcTemplate);
        Main.consolidarEspecificacoesRaw(jdbcTemplate);

        logger.log("Lendo arquivo consolidado");
        List<Logs> logsArqConsolidado = Main.leImportaArquivoCsvTrusted("logs_consolidados_servidores.csv");

        logger.log("Iniciando Tratamento Temperatura");
        TratamentoTemperaturaCpu tratamentoTemperaturaCpu = new TratamentoTemperaturaCpu(aws, jdbcTemplate);
        tratamentoTemperaturaCpu.tratamentoDeTemperaturaCpu(logsArqConsolidado, "temperaturaUsoCpu");

        TratamentoTemperaturaDisco tratarTemperaturaDisco = new TratamentoTemperaturaDisco(aws, jdbcTemplate);
        tratarTemperaturaDisco.tratamentoDeTemperaturaDisco(logsArqConsolidado, "temperaturaUsoDisco");

        TratamentoProcessos.consolidarArquivosRawProcessos();
        TratamentoProcessos tratarProcessos = new TratamentoProcessos(aws, jdbcTemplate);
        tratarProcessos.tratamentoProcessos("processos_consolidados_servidores.csv");

        aws.limparTemporarios();

        // TRATAMENTO WILLIAN
        logger.log("Iniciando tratamento Willian...\n");
        ProcessadorDiscoWillian tratamentoDisco = new ProcessadorDiscoWillian(aws, con);
        tratamentoDisco.executarTratamento();

        aws.limparTemporarios();

        // LISTA DE IDS DE SERVIDORES
        List<Integer> listaIdServidores = new ArrayList<>();
        for (Logs log : logsArqConsolidado) {
            Integer idDaVez = log.getFk_servidor();
            if (!listaIdServidores.contains(idDaVez)) {
                listaIdServidores.add(idDaVez);
            }
        }

        // TRATAMENTO DERECK (REGIÃO)
        logger.log("Iniciando tratamento região...\n");
        TratamentoClima tratamentoClima = new TratamentoClima(aws, jdbcTemplate);
        tratamentoClima.buscarRegioes(jdbcTemplate, logsArqConsolidado);

        // TRATAMENTO REDE
        logger.log("Iniciando tratamento rede...\n");
        TratamentoRede.gravaArquivoJsonRede(logsArqConsolidado, listaIdServidores, jdbcTemplate, aws);
        TratamentoRede.gravaArquivoJson(listaIdServidores, aws);

        // TRATAMENTO GIULIA
        logger.log("Iniciando tratamento Giulia...\n");
        TratamentoDonut tratamentoDonut = new TratamentoDonut(aws, jdbcTemplate);
        tratamentoDonut.classificarCriticidade();

        TratamentoBolhas tratamentoBolhas = new TratamentoBolhas(aws, jdbcTemplate);
        tratamentoBolhas.gerarBolhas("CPU", "%");
        tratamentoBolhas.gerarBolhas("CPU", "C");
        tratamentoBolhas.gerarBolhas("RAM", "%");
        tratamentoBolhas.gerarBolhas("DISCO", "%");
        tratamentoBolhas.gerarBolhas("DISCO", "C");
        tratamentoBolhas.gerarBolhas("REDE", "UPLOAD");
        tratamentoBolhas.gerarBolhas("REDE", "DOWNLOAD");
        tratamentoBolhas.gerarBolhas("REDE", "PCKT_RCVD");
        tratamentoBolhas.gerarBolhas("REDE", "PCKT_SNT");

        TratamentoHistorico tratamentoHistorico = new TratamentoHistorico(aws, jdbcTemplate);
        tratamentoHistorico.classificarAlertas(1);
        tratamentoHistorico.classificarAlertas(7);
        tratamentoHistorico.classificarAlertas(30);

        TratamentoHistoricoServidor tratamentoHistoricoServidor = new TratamentoHistoricoServidor(aws, jdbcTemplate);
        tratamentoHistoricoServidor.classificarAlertas(1);
        tratamentoHistoricoServidor.classificarAlertas(7);
        tratamentoHistoricoServidor.classificarAlertas(30);

        // TRATAMENTO NEAR REAL TIME
        logger.log("Iniciando tratamento Near Real Time...\n");
        tratamentoNearRealTime.logsEspecifico(logsArqConsolidado);

        aws.limparTemporarios();

        logger.log("Todos os tratamentos concluídos!\n");
    }

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        LambdaLogger logger = context.getLogger();
        Map<String, Object> resposta = new HashMap<>();

        try {
            logger.log("Execução ETL iniciada");
            Long tempoInicial = System.currentTimeMillis();

            executarETL(logger);

            Long tempoFinal = System.currentTimeMillis();
            Long duracao = (tempoFinal - tempoInicial) / 1000;

            logger.log("ETL Concluída, Tempo: " + duracao + " segundos");
        } catch (Exception e) {
            logger.log("ERRO na execução da ETL: " + e.getMessage());
            e.printStackTrace();

            resposta.put("statusCode", 500);
            resposta.put("message", "Erro na execução da ETL");
            resposta.put("error", e.getMessage());
        }

        return resposta;
    }
}