//package org.example.classesWillian;
//
//import org.example.AwsConnection;
//
//import java.util.List;
//
//public class TratamentoCentralDisco {
//
//    private AwsConnection aws;
//    private BancoParametros repo;
//    private TratamentoKpi tratamentoKpi;
//    private LeitorCsvTrusted leitor;
//
//    public TratamentoCentralDisco(AwsConnection aws, BancoParametros repo) {
//        this.aws = aws;
//        this.repo = repo;
//
//        this.tratamentoKpi = new TratamentoKpi(repo, aws);
//        this.leitor = new LeitorCsvTrusted();
//    }
//
//    public void executar() {
//
//        // 1) download do trusted
//        String csvLocal = aws.("logs_consolidados_servidores.csv");
//
//        // 2) leitura + redução
//        List<LogsWillianDisco> logs = leitor.lerCsvDisco(csvLocal);
//
//        // 3) primeira tarefa → ALERTA DISCO
//        tratamentoKpi.tratarAlertaDisco(logs);
//
//        System.out.println("Tratamento finalizado.");
//    }
//}

