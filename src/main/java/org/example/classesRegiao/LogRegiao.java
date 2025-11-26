package org.example.classesRegiao;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

public class LogRegiao {
    private Integer fkServidor;
    private Double usoDisco;
    private Double usoRam;
    private Integer qtdRequisicoes;
    private LocalDateTime dataHora;

    public LogRegiao() {
    }

    public LogRegiao(Integer fkServidor, Integer qtdRequisicoes, Double usoDisco, Double usoRam,String dataHoraString) {
        Random random = new Random();
        this.fkServidor = fkServidor;
        this.qtdRequisicoes = (qtdRequisicoes * 100) + random.nextInt(999);
        this.usoDisco = usoDisco;
        this.usoRam = usoRam;
        this.dataHora = LocalDateTime.parse(dataHoraString, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
    public Integer getFkServidor() {
        return fkServidor;
    }
    public void setFkServidor(Integer fkServidor) {
        this.fkServidor = fkServidor;
    }
    public Integer getQtdRequisicoes() {
        return qtdRequisicoes;
    }
    public void setQtdRequisicoes(Integer qtdRequisicoes) {
        this.qtdRequisicoes = qtdRequisicoes;
    }
    public Double getUsoDisco() {
        return usoDisco;
    }
    public void setUsoDisco(Double usoDisco) {
        this.usoDisco = usoDisco;
    }
    public Double getUsoRam() {
        return usoRam;
    }
    public void setUsoRam(Double usoRam) {
        this.usoRam = usoRam;
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

    @Override
    public String toString() {
        return "LogRegiao{" +
                "dataHora=" + dataHora +
                ", fkServidor=" + fkServidor +
                ", usoDisco=" + usoDisco +
                ", usoRam=" + usoRam +
                ", qtdRequisicoes=" + qtdRequisicoes +
                '}';
    }
}
