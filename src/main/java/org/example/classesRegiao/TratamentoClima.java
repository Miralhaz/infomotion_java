package org.example.classesRegiao;

import org.example.AwsConnection;
import org.example.Logs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class TratamentoClima {
    private static final DateTimeFormatter INPUT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Logger log = LoggerFactory.getLogger(TratamentoClima.class);
    private static AwsConnection awsConnection;
    private static final String nomePasta = "DashBoard_Regiao/";
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

                System.out.println(lista);


                if (r.getListaLogClima() != null || r.getListaLogRegiao() != null || !r.getListaLogClima().isEmpty() || !r.getListaLogRegiao().isEmpty()){

                    List <LogPrevisao> listaParaJsonPre = criarLogPrevisao(r.getListaLogClima(), r.getListaLogRegiao());
                    criarJsonPrevisao(listaParaJsonPre,r.getId());
                    List <LogHorarioReq> listaParaJsonHora = criarLogHorario(r);
                    criarJson(listaParaJsonHora,r.getId());
                }



            }
        }


        awsConnection.limparTemporarios();
    }

    public static  List <LogPrevisao>  criarLogPrevisao(List<LogClima> logClimaList ,List<LogRegiao> logRegiaoList){
        List <LogPrevisao> logPrevisaos = new ArrayList<>();
        LocalDate dataInicio = logClimaList.get(0).getDataHora().toLocalDate();
        LocalDate dataFinal = logClimaList.get(logClimaList.size() - 1).getDataHora().toLocalDate();
        Double residual = retornarResidual(logRegiaoList);
        Integer mediana = retornarMediana(logRegiaoList);
        Random random = new Random();
        List<LocalDate> datas = new ArrayList<>();
        for (LocalDate i = dataInicio; !i.isAfter(dataFinal); i = i.plusDays(1)) {
            datas.add(i);
        }

        for (int i = 0; i < datas.size(); i++) {
             LocalDate data = datas.get(i);
             Integer qtdRequisicao =  0;
             Double chanceDeChuva = 0.0;
             Double chuvaEmMM = 0.0;
            Double temperatura = 0.0;
            Double umidade = 0.0;
            for (LogClima clima : logClimaList){
                if (clima.getDataHora().toLocalDate().isEqual(data)){
                    if (chanceDeChuva < clima.getProbabilidadeChuva()){
                        chanceDeChuva = clima.getProbabilidadeChuva();
                    }
                    if (chuvaEmMM < clima.getMmChuva()){
                        chuvaEmMM = clima.getMmChuva();
                    }
                    if (temperatura < clima.getTemperatura()){
                        temperatura = clima.getTemperatura();
                    }
                    if (umidade < clima.getUmidade()){
                        umidade = clima.getUmidade();
                    }
                }
            }
            for (LogRegiao log : logRegiaoList) {
                if (log.getDataHora().toLocalDate().isEqual(data)){
                    if (qtdRequisicao < log.getQtdRequisicoes()){
                        qtdRequisicao = log.getQtdRequisicoes();
                    }
                }
            }
            if (qtdRequisicao <= 0){
                System.out.println(mediana + "mediana");
                System.out.println(residual.intValue() + "residual");
                qtdRequisicao = (mediana + (random.nextInt(3) * residual.intValue()));
            }
            LogPrevisao log = new LogPrevisao(chanceDeChuva,chuvaEmMM,data,qtdRequisicao,temperatura,umidade);
            logPrevisaos.add(log);
        }


        for (LogPrevisao L : logPrevisaos){
            System.out.println(L+ "\n");
        }
        return logPrevisaos;
    }

    public static  List <LogHorarioReq>  criarLogHorario(Regiao r){
            List<LogHorarioReq> logHorarioReq = new ArrayList<>();
            LocalTime dataInicio = r.getListaLogRegiao().get(0).getDataHora().toLocalTime();
            LocalTime dataFinal = r.getListaLogRegiao().get(r.getListaLogRegiao().size() - 1).getDataHora().toLocalTime();

            Integer minutosInicio = dataInicio.getMinute();
            Integer minutosFinal = dataFinal.getMinute();
            dataInicio.minusMinutes(minutosInicio);
            dataFinal.minusMinutes(minutosFinal);

            List<LocalTime> listaDeHorarios = new ArrayList<>();


            for (int i = 0; i < listaDeHorarios.size(); i++) {
                LocalTime horario = listaDeHorarios.get(i);
                Integer horaDaVez = listaDeHorarios.get(i).getHour();
                Integer totalDeReq = 0;

                for (LogRegiao log : r.getListaLogRegiao()) {
                    Integer horarioLog = log.getDataHora().getHour();
                    if (horaDaVez == horarioLog) {
                        totalDeReq += log.getQtdRequisicoes();
                    }
                }

                LogHorarioReq lhr = new LogHorarioReq(horario,totalDeReq);
                logHorarioReq.add(lhr);
            }
            return logHorarioReq;
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
                    Double temperatura = Double.valueOf(registro[6]);
                    Double umidade = Double.valueOf(registro[7]);


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

    public static Double retornarResidual(List<LogRegiao> listaLogRegiao){
        Integer media = 0;

        for (LogRegiao log : listaLogRegiao ){
            if(log.getQtdRequisicoes() == 0){
                listaLogRegiao.remove(log);
            }
        }

        for (int i = 0; i < listaLogRegiao.size(); i++) {
            media +=  listaLogRegiao.get(i).getQtdRequisicoes();
        }
        media = media / listaLogRegiao.size();
        System.out.println(media + " media");
        Double totalResiduo = 0.0;

        int contador = 0;
        for (int i = 0; i < listaLogRegiao.size(); i++) {
            Integer qtdReq = listaLogRegiao.get(i).getQtdRequisicoes();
                Integer residuo = 0;
                if (media > qtdReq) {
                    residuo = media - qtdReq;
                } else {
                    residuo = qtdReq - media;
                }
                totalResiduo += Math.pow(residuo,2);
           contador++;
        }
        return Math.sqrt(totalResiduo/contador);
    }

    public static Integer retornarMediana(List<LogRegiao> listaLogRegiao){
        List<LogRegiao> l = listaLogRegiao;

        for (LogRegiao log : l ){
            if(log.getQtdRequisicoes() == 0){
                l.remove(log);
            }
        }

        for (int i = 0; i < l.size(); i++) {
            for (int j = 1; j < l.size(); j++) {
                if (l.get(j - 1).getQtdRequisicoes() > l.get(j).getQtdRequisicoes()) {
                    LogRegiao aux = l.get(j);
                    l.set(j, l.get(j - 1));
                    l.set(j - 1, aux);
                }
            }
        }

        if (l.size() % 2 == 0 ){
            return    l.get(l.size() / 2 - 1).getQtdRequisicoes();
        }else {
            return    l.get(l.size() / 2 - 2).getQtdRequisicoes();
        }
    }

    public static void criarJsonPrevisao(List<LogPrevisao> lista, Integer idRegiao){
        String nomeArq = "RegiaoPrevisao" + idRegiao;
        String nome = nomeArq.endsWith(".json") ? nomeArq : nomeArq + ".json";
        OutputStreamWriter saida = null;
        Boolean deuRuim = false;

        try {
            saida = new OutputStreamWriter(new FileOutputStream(nome), StandardCharsets.UTF_8);
            saida.append("[");
            Integer contador = 0;
            for (LogPrevisao log : lista) {
                if (contador > 0) {
                    saida.append(",");
                }

                String data = log.getData().toString();
                Integer requisicoes = log.qtdReqPrevistas().intValue();
                Double chance = log.chanceDeAlteracao();
                Double percentual = log.percentualDeAumento(log.getQtdRequisicao().intValue(),log.qtdReqPrevistas().intValue());




                saida.write(String.format(Locale.US,""" 
                           {
                           "Data": "%s",
                           "Requsicoes": %d,
                           "ChanceDeAlteracao": %.2f,
                           "PorcentagemDeAumento": %.2f
                           }""",data,requisicoes,chance,percentual

                        ));
                contador ++;
            }
            saida.append("]");
            System.out.println("Arquivo Json de previsão gerado com sucesso!");


            awsConnection.uploadBucketClient(nomePasta,nomeArq);
        }

        catch (IOException erro) {
            System.out.println("Erro ao gravar o arquivo de previsao");
            erro.printStackTrace();
            deuRuim = true;
        }

        finally {

            try {
                if (saida != null){
                    saida.close();
                }
            }

            catch (IOException erro) {
                System.out.println("Erro ao fechar o arquivo");
                deuRuim = true;
            }

            if (deuRuim) {
                System.exit(1);
            }
        }
    }

    public static void criarJson(List<LogHorarioReq> lr, Integer idRegiao){
        String nomeArq = "RegiaoHorario" + idRegiao;
        String nome = nomeArq.endsWith(".json") ? nomeArq : nomeArq + ".json";
        OutputStreamWriter saida = null;
        Boolean deuRuim = false;

        try {
            saida = new OutputStreamWriter(new FileOutputStream(nome), StandardCharsets.UTF_8);
            saida.append("[");
            Integer contador = 0;
            for (LogHorarioReq lhr :lr) {

                if (contador > 0) {
                    saida.append(",");
                }

                String hora = lhr.getHorario().toString();
                Integer qtdReq = lhr.getRequisicao();

                saida.write(String.format(Locale.US,""" 
                           {
                           "Hora": %s,
                           "Requsicoes": %d,
                           }""",hora,qtdReq
                ));
                contador ++;
            }
            saida.append("]");
            System.out.println("Arquivo Json de requisicao gerado com sucesso!");


            awsConnection.uploadBucketClient(nomePasta,nomeArq);
        }

        catch (IOException erro) {
            System.out.println("Erro ao gravar o arquivo de requisicao");
            erro.printStackTrace();
            deuRuim = true;
        }

        finally {

            try {
                if (saida != null){
                    saida.close();
                }
            }

            catch (IOException erro) {
                System.out.println("Erro ao fechar o arquivo");
                deuRuim = true;
            }

            if (deuRuim) {
                System.exit(1);
            }
        }
    }

}
