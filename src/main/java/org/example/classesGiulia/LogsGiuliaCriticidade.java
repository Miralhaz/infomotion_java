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
    private Double tempCpu;
    private Double tempDisco;
    private String classificacao;
    private Double captura;

    private Integer alertasCpu;
    private Integer alertasRam;
    private Integer alertasDisco;
    private Integer alertasRede;
    private Integer totalAlertas;

    private Long uploadByte;
    private Long downloadByte;
    private Long packetReceived;
    private Long packetSent;

    // Construtores:
    public LogsGiuliaCriticidade(Integer fk_servidor, String apelido, Integer minutos, LocalDateTime timestamp, Double usoCpu, Double usoRam, Double usoDisco, Double tempCpu, Double tempDisco, Long uploadByte, Long downloadByte, Long packetSent, Long packetReceived, String classificacao) {
        this.fk_servidor = fk_servidor;
        this.apelido = apelido;
        this.minutos = minutos;
        this.timestamp = timestamp;
        this.usoCpu = usoCpu;
        this.usoRam = usoRam;
        this.usoDisco = usoDisco;
        this.tempCpu = tempCpu;
        this.tempDisco = tempDisco;
        this.uploadByte = uploadByte;
        this.downloadByte = downloadByte;
        this.packetReceived = packetReceived;
        this.packetSent = packetSent;
        this.classificacao = classificacao;
    }

    public LogsGiuliaCriticidade(Integer fk_servidor, String apelido, Double captura, Integer minutos, String classificacao) {
        this.fk_servidor = fk_servidor;
        this.apelido = apelido;
        this.captura = captura;
        this.minutos = minutos;
        this.classificacao = classificacao;
    }

    public LogsGiuliaCriticidade(Integer fk_servidor, String apelido, LocalDateTime timestamp, Integer minutos, Double usoCpu, Double usoRam, Double usoDisco, Double tempCpu, Double tempDisco, Long uploadByte, Long downloadByte, Long packetSent, Long packetReceived, Integer alertasCpu, Integer alertasRam, Integer alertasDisco, Integer alertasRede, Integer totalAlertas) {
        this.fk_servidor = fk_servidor;
        this.apelido = apelido;
        this.timestamp = timestamp;
        this.minutos = minutos;
        this.usoCpu = usoCpu;
        this.usoRam = usoRam;
        this.usoDisco = usoDisco;
        this.tempCpu = tempCpu;
        this.tempDisco = tempDisco;
        this.uploadByte = uploadByte;
        this.downloadByte = downloadByte;
        this.packetReceived = packetReceived;
        this.packetSent = packetSent;
        this.alertasCpu = alertasCpu;
        this.alertasRam = alertasRam;
        this.alertasDisco = alertasDisco;
        this.alertasRede = alertasRede;
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

    public Double getCaptura() {
        return captura;
    }

    public void setCaptura(Double captura) {
        this.captura = captura;
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

    public Double getTempCpu() {
        return tempCpu;
    }

    public void setTempCpu(Double tempCpu) {
        this.tempCpu = tempCpu;
    }

    public Double getTempDisco() {
        return tempDisco;
    }

    public void setTempDisco(Double tempDisco) {
        this.tempDisco = tempDisco;
    }

    public Long getUploadByte() {
        return uploadByte;
    }

    public void setUploadByte(Long uploadByte) {
        this.uploadByte = uploadByte;
    }

    public Long getDownloadByte() {
        return downloadByte;
    }

    public void setDownloadByte(Long downloadByte) {
        this.downloadByte = downloadByte;
    }

    public Long getPacketReceived() {
        return packetReceived;
    }

    public void setPacketReceived(Long packetReceived) {
        this.packetReceived = packetReceived;
    }

    public Long getPacketSent() {
        return packetSent;
    }

    public void setPacketSent(Long packetSent) {
        this.packetSent = packetSent;
    }

    public Integer getAlertasRede() {
        return alertasRede;
    }

    public void setAlertasRede(Integer alertasRede) {
        this.alertasRede = alertasRede;
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
                " | tempCpu:" + tempCpu +
                " | tempDisco:" + tempDisco +
                " | classificacao:" + classificacao +
                " | captura:" + captura +
                " | alertasCpu:" + alertasCpu +
                " | alertasRam:" + alertasRam +
                " | alertasDisco:" + alertasDisco +
                " | alertasRede:" + alertasRede +
                " | totalAlertas:" + totalAlertas +
                " | uploadByte:" + uploadByte +
                " | downloadByte:" + downloadByte +
                " | packetReceived:" + packetReceived +
                " | packetSent:" + packetSent +
                '}';
    }
}
