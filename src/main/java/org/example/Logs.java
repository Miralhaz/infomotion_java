package org.example;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Logs {

    @JsonProperty("usuario")
    private String usuario;
    @JsonProperty("ip")
    private String ip;
    @JsonProperty("cpu")
    private Double cpu;
    @JsonProperty("ram")
    private Double ram;
    @JsonProperty("disco")
    private Double disco;
    @JsonProperty("dataHora")
    private String dataHoraString;

    private LocalDate dataHora;

    public Logs() {
    }



    public String getUsuario() {
        return usuario;
    }

    public String getIp() {
        return ip;
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

    public LocalDate getDataHora() {
        return dataHora;
    }


    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public void setIp(String ip) {
        this.ip = ip;
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
        DateTimeFormatter isoLocalDate = DateTimeFormatter.ISO_LOCAL_DATE;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        LocalDate dataUnformatter = LocalDate.parse(dataHora,isoLocalDate);
        String dataTimeFormatterString = dataUnformatter.format(formatter);
        this.dataHora = LocalDate.parse(dataTimeFormatterString,formatter);
    }




    public String toString() {
        return "USER: " + this.usuario + " | " +"IP: " + this.ip + " | " +"CPU: " + this.cpu+"%" + " | " +"RAM: " + this.ram +" | "+ "DISCO: " + this.disco+"%"+ " | " +"DATA: " + this.dataHora+ "\n";
    }
}
