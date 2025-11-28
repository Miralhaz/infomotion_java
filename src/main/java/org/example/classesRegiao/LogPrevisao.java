package org.example.classesRegiao;

import java.time.LocalDate;

public class LogPrevisao {
    private LocalDate data;
    private Integer qtdRequisicao;
    private Double chanceDeChuva;
    private Double chuvaEmMM;
    private Integer temperatura;
    private Integer umidade;

    public LogPrevisao(Double chanceDeChuva, Double chuvaEmMM, LocalDate data, Integer qtdRequisicao, Integer temperatura, Integer umidade) {
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

    public Integer getTemperatura() {
        return temperatura;
    }

    public void setTemperatura(Integer temperatura) {
        this.temperatura = temperatura;
    }

    public Integer getUmidade() {
        return umidade;
    }

    public void setUmidade(Integer umidade) {
        this.umidade = umidade;
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

