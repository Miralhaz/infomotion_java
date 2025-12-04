package org.example.classesRegiao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;

public class LogPrevisao {
    private static final Logger log = LoggerFactory.getLogger(LogPrevisao.class);
    private LocalDate data;
    private Integer qtdRequisicao;
    private Double chanceDeChuva;
    private Double chuvaEmMM;
    private Double temperatura;
    private Double umidade;

    public LogPrevisao(Double chanceDeChuva, Double chuvaEmMM, LocalDate data, Integer qtdRequisicao, Double temperatura, Double umidade) {
        this.chanceDeChuva = chanceDeChuva;
        this.chuvaEmMM = chuvaEmMM;
        this.data = data;
        this.qtdRequisicao = qtdRequisicao;
        this.temperatura = temperatura;
        this.umidade = umidade;
    }

    public Double getChanceDeChuva() {
        return chanceDeChuva;
    }

    public void setChanceDeChuva(Double chanceDeChuva) {
        this.chanceDeChuva = chanceDeChuva;
    }

    public Double getChuvaEmMM() {
        return chuvaEmMM;
    }

    public void setChuvaEmMM(Double chuvaEmMM) {
        this.chuvaEmMM = chuvaEmMM;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public Integer getQtdRequisicao() {
        return qtdRequisicao;
    }

    public void setQtdRequisicao(Integer qtdRequisicao) {
        this.qtdRequisicao = qtdRequisicao;
    }

    public Double getTemperatura() {
        return temperatura;
    }

    public void setTemperatura(Double temperatura) {
        this.temperatura = temperatura;
    }

    public Double getUmidade() {
        return umidade;
    }

    public void setUmidade(Double umidade) {
        this.umidade = umidade;
    }

    public Double qtdReqPrevistas(){
        Double qtdReqPrevistas = 0.0;
        qtdReqPrevistas += qtdRequisicao;

        if (chanceDeChuva > 0) {
            if (temperatura > 30 || temperatura < 0) {
                qtdReqPrevistas += (qtdRequisicao * 0.05);
            }

            if (chuvaEmMM > 3.0) {
                qtdReqPrevistas += qtdRequisicao * 0.35;
            } else if (chuvaEmMM > 2.0) {
                qtdReqPrevistas += qtdRequisicao * 0.25;
            } else if (chuvaEmMM > 1.0) {
                qtdReqPrevistas += qtdRequisicao * 0.10;
            }else if (chuvaEmMM < 1.0) {
                qtdReqPrevistas += qtdRequisicao * 0.05;
            }
        }


        return qtdReqPrevistas;
    }

    public Double chanceDeAlteracao(){
        Double chance = chanceDeChuva;

        if (temperatura > 30 || temperatura < 0 ){
            chance += chance * 0.005;
        } else if (temperatura > 20 || temperatura < 10) {
            chance += chance * 0.007;
        }

        if (chance > 100.00){
            chance = 100.00;
        }
        return chance;
    }

    public Double usoRAM(Double req){
        return req * 0.003;
    }

    public Double percentualDeAumento(Integer req,Integer reqPrevista){
        Integer resultado;
        if (req > reqPrevista){
            resultado = req - reqPrevista ;
        }else {
            resultado = reqPrevista - req;
        }


        return (resultado/req.doubleValue()) * 100.00;





    }

    @Override
    public String toString() {
        return "LogPrevisao{" +
                "chanceDeChuva=" + chanceDeChuva +
                ", data=" + data +
                ", qtdRequisicao=" + qtdRequisicao +
                ", chuvaEmMM=" + chuvaEmMM +
                ", temperatura=" + temperatura +
                ", umidade=" + umidade +
                '}';
    }




}

