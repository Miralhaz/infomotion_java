package org.example;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Logs {
    @JsonProperty("user")
    private String user;
    @JsonProperty("timestamp")
    private String dataHoraString;
    @JsonProperty("cpu")
    private Double cpu;
    @JsonProperty("ram")
    private Double ram;
    @JsonProperty("disco")
    private Double disco;
    @JsonProperty("temperatura_cpu")
    private Double tmp_cpu;
    @JsonProperty("temperatura_disco")
    private Double tmp_disco;
    @JsonProperty("memoria_swap")
    private Double memoria_swap;
    @JsonProperty("quantidade_processos")
    private Integer qtd_processos;

    private LocalDateTime dataHora;

    public Logs(String user, String dataHoraString, Double cpu, Double ram, Double disco, Double tmp_cpu, Double tmp_disco, Double memoria_swap, Integer qtd_processos) {
        this.user = user;
        this.dataHoraString = dataHoraString;
        this.cpu = cpu;
        this.ram = ram;
        this.disco = disco;
        this.tmp_cpu = tmp_cpu;
        this.tmp_disco = tmp_disco;
        this.memoria_swap = memoria_swap;
        this.qtd_processos = qtd_processos;
        DateTimeFormatter LocalDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        LocalDateTime dataUnformatter = LocalDateTime.parse(dataHoraString,LocalDateTimeFormatter);
        String dataTimeFormatterString = dataUnformatter.format(formatter);
        this.dataHora = LocalDateTime.parse(dataTimeFormatterString,formatter);
    }

    public Logs() {
    }



    public String getUser() {
        return user;
    }


    public Double getCpu() {
        return cpu;
    }

    public Double getRam() {
        return ram;
    }

    public Double getDisco() {
        return disco;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }


    public void setUser(String user) {
        this.user = user;
    }

    public String getDataHoraString() {
        return dataHoraString;
    }

    public void setCpu(Double cpu) {
        this.cpu = cpu;
    }

    public void setRam(Double ram) {
        this.ram = ram;
    }

    public void setDisco(Double disco) {
        this.disco = disco;
    }

    public void setDataHoraString(String dataHoraString) {
        this.dataHoraString = dataHoraString;
        dataHoraFormatter(dataHoraString);
    }

    public void dataHoraFormatter(String dataHora) {
        DateTimeFormatter LocalDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        LocalDateTime dataUnformatter = LocalDateTime.parse(dataHora,LocalDateTimeFormatter);
        String dataTimeFormatterString = dataUnformatter.format(formatter);
        this.dataHora = LocalDateTime.parse(dataTimeFormatterString,formatter);
    }

    public Double getTmp_cpu() {
        return tmp_cpu;
    }

    public void setTmp_cpu(Double tmp_cpu) {
        this.tmp_cpu = tmp_cpu;
    }

    public Double getTmp_disco() {
        return tmp_disco;
    }

    public void setTmp_disco(Double tmp_disco) {
        this.tmp_disco = tmp_disco;
    }

    public Double getMemoria_swap() {
        return memoria_swap;
    }

    public void setMemoria_swap(Double memoria_swap) {
        this.memoria_swap = memoria_swap;
    }

    public Integer getQtd_processos() {
        return qtd_processos;
    }

    public void setQtd_processos(Integer qtd_processos) {
        this.qtd_processos = qtd_processos;
    }

    @Override
    public String
    toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        return "\n User:" + user +
                " | Data e Hora:" + dataHora.format(formatter) +
                " | cpu:" + cpu +
                " | ram:" + ram +
                " | disco:" + disco +
                " | temperatura de cpu:" + tmp_cpu +
                " | temperatura de disco:" + tmp_disco +
                " | memoria swap:" + memoria_swap +
                " | quantidade de processos:" + qtd_processos;
    }
}
