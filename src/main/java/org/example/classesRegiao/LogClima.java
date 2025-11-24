package org.example.classesRegiao;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LogClima {
    private LocalDateTime dataHora;
    private Double probabilidadeChuva;
    private Double mmChuva;
    private Double temperatura;
    private Double umidade;


    public LogClima() {
    }

    public LogClima(String dataHoraString, Double humidade, Double mmChuva, Double probabilidadeChuva, Double temperatura) {
        this.dataHora = LocalDateTime.parse(dataHoraString, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        this.umidade = humidade;
        this.mmChuva = mmChuva;
        this.probabilidadeChuva = probabilidadeChuva;
        this.temperatura = temperatura;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public String getDataHoraFormatada() {
        return dataHora.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }

    public Double getUmidade() {
        return umidade;
    }

    public void setUmidade(Double umidade) {
        this.umidade = umidade;
    }

    public Double getMmChuva() {
        return mmChuva;
    }

    public void setMmChuva(Double mmChuva) {
        this.mmChuva = mmChuva;
    }

    public Double getProbabilidadeChuva() {
        return probabilidadeChuva;
    }

    public void setProbabilidadeChuva(Double probabilidadeChuva) {
        this.probabilidadeChuva = probabilidadeChuva;
    }

    public Double getTemperatura() {
        return temperatura;
    }

    public void setTemperatura(Double temperatura) {
        this.temperatura = temperatura;
    }
}
