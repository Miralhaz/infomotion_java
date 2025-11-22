package org.example.classesGiulia;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LogsGiuliaCriticidade {

    // Atributos:
    private Integer fk_servidor;
    private String apelido;
    private LocalDateTime timestamp;
    private Integer minutos;
    private Double usoCpu;
    private Double usoRam;
    private Double usoDisco;
    private String classificacao;
    private Double percentual;

    // Construtor:
    public LogsGiuliaCriticidade(Integer fk_servidor, String apelido, LocalDateTime timestamp, Integer minutos, Double usoCpu, Double usoRam, Double usoDisco, String classificacao) {
        this.fk_servidor = fk_servidor;
        this.apelido = apelido;
        this.timestamp = timestamp;
        this.minutos = minutos;
        this.usoCpu = usoCpu;
        this.usoRam = usoRam;
        this.usoDisco = usoDisco;
        this.classificacao = classificacao;
    }

    public LogsGiuliaCriticidade(Integer fk_servidor, Double percentual, Integer minutos) {
        this.fk_servidor = fk_servidor;
        this.percentual = percentual;
        this.minutos = minutos;
    }

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

    public LocalDateTime getTimestamp() {
        return timestamp;
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

    public Double getPercentual() {
        return percentual;
    }

    public void setPercentual(Double percentual) {
        this.percentual = percentual;
    }

    // toString():
    @Override
    public String toString() {
        return "LogsGiuliaCriticidade{" +
                " fk_servidor:" + fk_servidor +
                " | apelido:" + apelido +
                " | timestamp:" + timestamp +
                " | minutos:" + minutos +
                " | usoCpu:" + usoCpu +
                " | usoRam:" + usoRam +
                " | usoDisco:" + usoDisco +
                " | classificacao:" + classificacao +
                '}';
    }
}
