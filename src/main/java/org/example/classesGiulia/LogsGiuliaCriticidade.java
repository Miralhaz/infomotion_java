package org.example.classesGiulia;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LogsGiuliaCriticidade {

    // Atributos:
    private Integer fk_servidor;
    private String apelido;
    private String dataHoraString;
    private Integer minutos;
    private Double usoCpu;
    private Double usoRam;
    private Double usoDisco;
    private String classificacao;

    private LocalDateTime dataHora;

    // Construtor:

    public LogsGiuliaCriticidade(Integer fk_servidor, String apelido, String dataHoraString, Integer minutos, Double usoCpu, Double usoRam, Double usoDisco, String classificacao) {
        this.fk_servidor = fk_servidor;
        this.apelido = apelido;
        this.dataHoraString = dataHoraString;
        this.minutos = minutos;
        this.usoCpu = usoCpu;
        this.usoRam = usoRam;
        this.usoDisco = usoDisco;
        this.classificacao = classificacao;
        DateTimeFormatter LocalDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        LocalDateTime dataUnformatter = LocalDateTime.parse(dataHoraString,LocalDateTimeFormatter);
        String dataTimeFormatterString = dataUnformatter.format(formatter);
        this.dataHora = LocalDateTime.parse(dataTimeFormatterString,formatter);
    }

    public LogsGiuliaCriticidade() {}

    // Métodos:

    // Getters e Setters:

    public Integer getFk_servidor() {
        return fk_servidor;
    }

    public void setFk_servidor(Integer fk_servidor) {
        this.fk_servidor = fk_servidor;
    }

    public String getApelido() {
        return apelido;
    }

    public void setApelido(String apelido) {
        this.apelido = apelido;
    }

    public String getDataHoraString() {
        return dataHoraString;
    }

    public Integer getMinutos() {
        return minutos;
    }

    public void setMinutos(Integer minutos) {
        this.minutos = minutos;
    }

    public Double getUsoCpu() {
        return usoCpu;
    }

    public void setUsoCpu(Double usoCpu) {
        this.usoCpu = usoCpu;
    }

    public Double getUsoRam() {
        return usoRam;
    }

    public void setUsoRam(Double usoRam) {
        this.usoRam = usoRam;
    }

    public Double getUsoDisco() {
        return usoDisco;
    }

    public void setUsoDisco(Double usoDisco) {
        this.usoDisco = usoDisco;
    }

    public String getClassificacao() {
        return classificacao;
    }

    public void setClassificacao(String classificacao) {
        this.classificacao = classificacao;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHoraString(String dataHoraString) {
        this.dataHoraString = dataHoraString;
        dataHoraFormatter(dataHoraString);
    }

    public LocalDateTime dataHoraFormatter(String dataHora) {
        DateTimeFormatter LocalDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        LocalDateTime dataUnformatter = LocalDateTime.parse(dataHora,LocalDateTimeFormatter);
        String dataTimeFormatterString = dataUnformatter.format(formatter);
        return this.dataHora = LocalDateTime.parse(dataTimeFormatterString,formatter);
    }

    // toString():
    @Override
    public String toString() {
        return "LogsGiuliaCriticidade{" +
                " fk_servidor:" + fk_servidor +
                " | apelido:" + apelido +
                " | dataHoraString:" + dataHoraString +
                " | minutos:" + minutos +
                " | usoCpu:" + usoCpu +
                " | usoRam:" + usoRam +
                " | usoDisco:" + usoDisco +
                " | classificacao:" + classificacao +
                " | dataHora:" + dataHora +
                '}';
    }
}
