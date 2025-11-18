package org.example.classesMiralha;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LogsMiralhaCpu {
    private Integer fk_servidor;
    private LocalDateTime dataHora;
    private Double usoCpu;
    private Double tempCpu;

    public LogsMiralhaCpu(Integer fk_servidor, String dataHoraString, Double usoCpu, Double tempCpu) {
        this.fk_servidor = fk_servidor;
        this.dataHora = LocalDateTime.parse(dataHoraString, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        this.usoCpu = usoCpu;
        this.tempCpu = tempCpu;
    }

    public Integer getFk_servidor() {
        return fk_servidor;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public String getDataHoraFormatada() {
        return dataHora.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
    }

    public Double getUsoCpu() {
        return usoCpu;
    }

    public Double getTempCpu() {
        return tempCpu;
    }
}
