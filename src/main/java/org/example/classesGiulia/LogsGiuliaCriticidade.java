package org.example.classesGiulia;

import java.time.LocalDateTime;

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

    private Integer alertasCpu;
    private Integer alertasRam;
    private Integer alertasDisco;
    private Integer totalAlertas;

    // Construtores:
    public LogsGiuliaCriticidade(Integer fk_servidor, String apelido, Integer minutos, LocalDateTime timestamp, Double usoCpu, Double usoRam, Double usoDisco, String classificacao) {
        this.fk_servidor = fk_servidor;
        this.apelido = apelido;
        this.minutos = minutos;
        this.timestamp = timestamp;
        this.usoCpu = usoCpu;
        this.usoRam = usoRam;
        this.usoDisco = usoDisco;
        this.classificacao = classificacao;
    }

    public LogsGiuliaCriticidade(Integer fk_servidor, String apelido, Double percentual, Integer minutos, String classificacao) {
        this.fk_servidor = fk_servidor;
        this.apelido = apelido;
        this.percentual = percentual;
        this.minutos = minutos;
        this.classificacao = classificacao;
    }

    public LogsGiuliaCriticidade(Integer fk_servidor, String apelido, LocalDateTime timestamp, Integer minutos, Double usoCpu, Double usoRam, Double usoDisco, Integer alertasCpu, Integer alertasRam, Integer alertasDisco, Integer totalAlertas) {
        this.fk_servidor = fk_servidor;
        this.apelido = apelido;
        this.timestamp = timestamp;
        this.minutos = minutos;
        this.usoCpu = usoCpu;
        this.usoRam = usoRam;
        this.usoDisco = usoDisco;
        this.alertasCpu = alertasCpu;
        this.alertasRam = alertasRam;
        this.alertasDisco = alertasDisco;
        this.totalAlertas = totalAlertas;
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

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
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

    public Integer getAlertasCpu() {
        return alertasCpu;
    }

    public void setAlertasCpu(Integer alertasCpu) {
        this.alertasCpu = alertasCpu;
    }

    public Integer getAlertasRam() {
        return alertasRam;
    }

    public void setAlertasRam(Integer alertasRam) {
        this.alertasRam = alertasRam;
    }

    public Integer getAlertasDisco() {
        return alertasDisco;
    }

    public void setAlertasDisco(Integer alertasDisco) {
        this.alertasDisco = alertasDisco;
    }

    public Integer getTotalAlertas() {
        return totalAlertas;
    }

    public void setTotalAlertas(Integer totalAlertas) {
        this.totalAlertas = totalAlertas;
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
                " | percentual:" + percentual +
                " | alertasCpu:" + alertasCpu +
                " | alertasRam:" + alertasRam +
                " | alertasDisco:" + alertasDisco +
                " | totalAlertas:" + totalAlertas +
                '}';
    }
}
