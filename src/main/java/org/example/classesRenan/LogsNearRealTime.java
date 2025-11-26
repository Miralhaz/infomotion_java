package org.example.classesRenan;

import java.time.LocalDateTime;

public class LogsNearRealTime {
    private Integer fk_servidor;
    private LocalDateTime timeStamp;
    private Double ram;
    private Double cpu;
    private Double disco;
    private Double temperatura_cpu;
    private Double temperatura_disco;
    private Long uploadByte;
    private Long downloadByte;

    public LogsNearRealTime(Integer fk_servidor, Long downloadByte, Long uploadByte, Double temperatura_disco, Double temperatura_cpu, Double cpu, Double disco, Double ram, LocalDateTime timeStamp) {
        this.fk_servidor = fk_servidor;
        this.downloadByte = downloadByte;
        this.uploadByte = uploadByte;
        this.temperatura_disco = temperatura_disco;
        this.temperatura_cpu = temperatura_cpu;
        this.cpu = cpu;
        this.disco = disco;
        this.ram = ram;
        this.timeStamp = timeStamp;
    }

    @Override
    public String toString() {
        return "LogsNearRealTime{" +
                "fk_servidor=" + fk_servidor +
                ", timeStamp=" + timeStamp +
                ", ram=" + ram +
                ", cpu=" + cpu +
                ", disco=" + disco +
                ", temperatura_cpu=" + temperatura_cpu +
                ", temperatura_disco=" + temperatura_disco +
                ", uploadByte=" + uploadByte +
                ", downloadByte=" + downloadByte +
                '}';
    }

    public Integer getFk_servidor() {
        return fk_servidor;
    }

    public void setFk_servidor(Integer fk_servidor) {
        this.fk_servidor = fk_servidor;
    }

    public LocalDateTime getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(LocalDateTime timeStamp) {
        this.timeStamp = timeStamp;
    }

    public Double getRam() {
        return ram;
    }

    public void setRam(Double ram) {
        this.ram = ram;
    }

    public Double getCpu() {
        return cpu;
    }

    public void setCpu(Double cpu) {
        this.cpu = cpu;
    }

    public Double getDisco() {
        return disco;
    }

    public void setDisco(Double disco) {
        this.disco = disco;
    }

    public Double getTemperatura_cpu() {
        return temperatura_cpu;
    }

    public void setTemperatura_cpu(Double temperatura_cpu) {
        this.temperatura_cpu = temperatura_cpu;
    }

    public Double getTemperatura_disco() {
        return temperatura_disco;
    }

    public void setTemperatura_disco(Double temperatura_disco) {
        this.temperatura_disco = temperatura_disco;
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

}
