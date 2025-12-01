package org.example.classesRegiao;

import org.example.AwsConnection;
import org.example.Logs;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class TratamentoClima {
    private static final DateTimeFormatter INPUT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static AwsConnection awsConnection;
    private static final String nomePasta = "Dashboard_Regiao";
    private static JdbcTemplate banco;

    public TratamentoClima(AwsConnection awsConnection, JdbcTemplate banco) {
        this.awsConnection = awsConnection;
        this.banco = banco;
    }

    public static void buscarRegioes(JdbcTemplate co, List<Logs> listaLogs){

        List<Regiao> listaRegiaoIdRegiao = co.query("SELECT id FROM regiao;",
                new BeanPropertyRowMapper(Regiao.class));

        for (Regiao r : listaRegiaoIdRegiao){
            List<Integer> listaServidoresRegiao = co.queryForList("SELECT id FROM servidor WHERE fk_regiao = (?)",
                    Integer.class, r.getId());

            List listaLogRegiao = new ArrayList<>();


            int idServer = 0;
            for (Integer f : listaServidoresRegiao) {
                idServer = f;
                if (idServer > 0) {
                    List lista2 = buscarRegiaoServidor(f, listaLogs);

                    for (int i = 0; i < lista2.size(); i++) {
                        listaLogRegiao.add(lista2.get(i));
                    }
                }
            }

            if (idServer > 0) {


                List lista = buscarClimaRegiao(r.getId(), idServer);

                r.setListaLogRegiao(listaLogRegiao);
                r.setListaLogClima(lista);


                if (r.getListaLogClima() != null || r.getListaLogRegiao() != null || !r.getListaLogClima().isEmpty() || !r.getListaLogRegiao().isEmpty()){
                    criarLogPrevisao(r.getListaLogClima(), r.getListaLogRegiao());
                }



            }
        }



    }

    public static void criarLogPrevisao(List<LogClima> logClimaList ,List<LogRegiao> logRegiaoList){
        List <LogPrevisao> logPrevisaos = new ArrayList<>();
        LocalDate Datainicio = logClimaList.get(0).getDataHora().toLocalDate();
        LocalDate Datafinal = logClimaList.get(logClimaList.size() - 1).getDataHora().toLocalDate();
        List<LocalDate> datas = new ArrayList<>();

        for (LocalDate i = Datainicio ; i.equals(Datafinal) ; i.plusDays(1)) {
                datas.add(i);
        }

        for (int i = 0; i < datas.size(); i++) {
             LocalDate data = datas.get(i);
             Integer qtdRequisicao = 1;
             Double chanceDeChuva = 0.0;
             Double chuvaEmMM = 1.0;
            Double temperatura = 1.0;
            Double umidade = 1.0;


        for (LogClima l : logClimaList){
            if (l.getDataHora().toLocalDate().equals(data)){
                if (chanceDeChuva < l .getProbabilidadeChuva()){
                    chanceDeChuva =  l .getProbabilidadeChuva();
                }
                if (chuvaEmMM < l.getMmChuva()){
                    chuvaEmMM = l.getMmChuva();
                }
                if (temperatura < l.getTemperatura()){
                    temperatura = l.getTemperatura();
                }
                if (umidade < l.getUmidade()){
                    umidade = l.getUmidade();
                }
            }

            for (LogRegiao lr : logRegiaoList) {
                if (qtdRequisicao < lr.getQtdRequisicoes()){
                    qtdRequisicao =  lr.getQtdRequisicoes();
                }
            }

            LogPrevisao log = new LogPrevisao(chanceDeChuva,chuvaEmMM,data,qtdRequisicao,temperatura,umidade);
            logPrevisaos.add(log);
        }
        List<LogPrevisao> listaNoPassado = new ArrayList<>();

        for (LogPrevisao lp : logPrevisaos){
            if (lp.getData().isBefore(LocalDate.now())){
             listaNoPassado.add(lp);
            }
        }
            Double residual = retornarResidual(listaNoPassado);
            Integer mediana = retornarMediana(listaNoPassado);
            Random random = new Random();


        for (LogPrevisao lp : logPrevisaos){
            if (lp.getData().isAfter(LocalDate.now())){
                lp.setQtdRequisicao(mediana * random.nextInt(3) * residual.intValue());
            }
        }











        }

    }

    public static List<LogClima> buscarClimaRegiao(Integer idRegiao , Integer argumentoArquivo){

        String nomeArq = "clima"+ argumentoArquivo;

        Reader arq = null;
        BufferedReader entrada = null;
        nomeArq += ".csv";
        List<LogClima> listaClima = new ArrayList<>();


        awsConnection.downloadBucketTrusted(nomeArq);

        try {
            arq = new InputStreamReader(new FileInputStream(nomeArq), StandardCharsets.UTF_8);
            entrada = new BufferedReader(arq);
        } catch (IOException e) {
            System.out.println("Erro ao abrir o arquivo [csvClima]");
            System.exit(1);
        }

        try {
            String[] registro;

            String linha = null;

            linha = entrada.readLine();
            int contador = 0;

            while (linha != null) {

                registro = linha.split(";");

                if (contador > 0) {

                    Integer id = Integer.valueOf(registro[9]);
                    String dataHora = registro[1];
                    Double probabilidadeChuva = Double.valueOf(registro[3]);
                    Double mmChuva = Double.valueOf(registro[2]);
                    Double temperatura = Double.valueOf(registro[5]);
                    Double umidade = Double.valueOf(registro[6]);


                    if (idRegiao.equals(id)) {
                        String dataHoraFormatado = dataHora.replace('T', ' ');
                        LogClima logClima = new LogClima(dataHoraFormatado, probabilidadeChuva, mmChuva, temperatura, umidade);
                        listaClima.add(logClima);
                    }}

                contador++;

                linha = entrada.readLine();
            }
        }catch (IOException e ){
            System.out.println("Erro ao ler o arquivo");
            e.printStackTrace();
            System.exit(1);

        }finally {
            try {
                entrada.close();
                arq.close();
            } catch (IOException e) {
                System.out.println("Erro ao fechar o arquivo");
            }
        }
        awsConnection.deleteCsvLocal(nomeArq);
        return listaClima;
    }

    public static List<LogRegiao> buscarRegiaoServidor(Integer idServidor, List<Logs> listaLogs){
        List<LogRegiao> listaLogRegiao = new ArrayList<>();

        for (Logs l : listaLogs){
            if (idServidor.equals(l.getFk_servidor())){
                LogRegiao lr = new LogRegiao(l.getFk_servidor(),l.getQtd_processos(),l.getDisco(),l.getRam(),l.getDataHora());
                listaLogRegiao.add(lr);
            }
        }
        return listaLogRegiao;
    }

    public static Double retornarResidual(List<LogPrevisao> listaLogRegiao){
        Integer media = 0;

        for (int i = 0; i < listaLogRegiao.size(); i++) {
            media +=  listaLogRegiao.get(i).getQtdRequisicao();
        }
        media = media / listaLogRegiao.size();

        Double totalResiduo = 0.0;
        for (int i = 0; i < listaLogRegiao.size(); i++) {
            Integer qtdReq = listaLogRegiao.get(i).getQtdRequisicao();
            Integer residuo = 0;
            if (media > qtdReq){
                residuo = media - qtdReq;
            }else {
                residuo = qtdReq - media;
            }
            totalResiduo += Math.pow(residuo,2.0);
        }
        return totalResiduo / listaLogRegiao.size();
    }

    public static Integer retornarMediana(List<LogPrevisao> listaLogRegiao){
        List<LogPrevisao> l = listaLogRegiao;

        for (int i = 0; i < l.size(); i++) {
            for (int j = 1; j < l.size(); j++) {
                if (l.get(j - 1).getQtdRequisicao() > l.get(j).getQtdRequisicao()) {
                    LogPrevisao aux = l.get(j);
                    l.set(j, l.get(j - 1));
                    l.set(j - 1, aux);
                }
            }
        }

        if (l.size() % 2 == 0 ){
            return    l.get(l.size() / 2 - 1).getQtdRequisicao();
        }else {
            return    l.get(l.size() / 2 - 2).getQtdRequisicao();
        }
    }



}
