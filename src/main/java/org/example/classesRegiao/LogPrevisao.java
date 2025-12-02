package org.example.classesRegiao;

import java.time.LocalDate;

public class LogPrevisao {
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
        return qtdRequisicao * chuvaEmMM * 0.25 + temperatura * 0.5;
    }

    public Double chanceDeAlteracao(){
        Double chance = chanceDeChuva;

        if (temperatura > 30 || temperatura < 5 ){
            chance += 5.0;
        } else if (temperatura > 20 || temperatura < 10) {
            chance += 2.0;
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
        Double umPorcento = req.doubleValue() / 100;
        Integer multiplicador;

        if (req > reqPrevista){
            multiplicador = req - reqPrevista ;
        }else {
            multiplicador = reqPrevista - req;
        }

        return umPorcento * multiplicador;
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

