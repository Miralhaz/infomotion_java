package org.example.classesMiralha;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LogsProcessosMiralha {
    private Integer fk_servidor;
    private LocalDateTime dataHora;
    private String nomeProcesso;
    private Double usoCpuProcesso;
    private Double usoRamProcesso;

    public LogsProcessosMiralha(Integer fk_servidor, String dataHoraString, String nomeProcesso, Double usoCpuProcesso, Double usoRamProcesso) {
        this.fk_servidor = fk_servidor;
        this.dataHora = LocalDateTime.parse(dataHoraString, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));;
        this.nomeProcesso = nomeProcesso;
        this.usoCpuProcesso = usoCpuProcesso;
        this.usoRamProcesso = usoRamProcesso;
    }

    public Integer getFk_servidor() {
        return fk_servidor;
    }

    public String getDataHoraFormatada() {
        return dataHora.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
    }

    public String getNomeProcesso() {
        return nomeProcesso;
    }

    public Double getUsoCpuProcesso() {
        return usoCpuProcesso;
    }

    public Double getUsoRamProcesso() {
        return usoRamProcesso;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }
}
