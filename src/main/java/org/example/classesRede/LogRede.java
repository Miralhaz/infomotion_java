package org.example.classesRede;


import software.amazon.awssdk.services.s3.endpoints.internal.Value;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LogRede {
    private Integer fk_servidor;
    private Long uploadByte;
    private Long downloadByte;
    private Long packetReceived;
    private Long packetSent;
    private Integer packetLossReceived;
    private Integer packetLossSent;
    private String dataHoraString;
    private Integer parametroDown;
    private Integer parametroUp;
    private Integer parametroPacotesRecebidos;
    private Integer ParametroPacotesEnviados;


    public LogRede(String dataHoraString, Long uploadByte, Long downloadByte, Long packetReceived, Long packetSent, Integer packetLossReceived, Integer packetLossSent, Integer fk_servidor, Integer parametroDown, Integer parametroUp, Integer parametroPacotesRecebidos, Integer ParametroPacotesEnviados) {
        this.fk_servidor = fk_servidor;
        this.uploadByte = uploadByte;
        this.downloadByte = downloadByte;
        this.packetReceived = packetReceived;
        this.packetSent = packetSent;
        this.packetLossReceived = packetLossReceived;
        this.packetLossSent = packetLossSent;
        this.dataHoraString = dataHoraString;
        this.parametroDown = parametroDown;
        this.parametroUp = parametroUp;
        this.parametroPacotesRecebidos = parametroPacotesRecebidos;
        this.ParametroPacotesEnviados = ParametroPacotesEnviados;
    }

    @Override
    public String toString() {
        return "LogRede{" +
                ", fk_servidor=" + fk_servidor +
                ", uploadByte=" + uploadByte +
                ", downloadByte=" + downloadByte +
                ", packetReceived=" + packetReceived +
                ", packetSent=" + packetSent +
                ", packetLossReceived=" + packetLossReceived +
                ", packetLossSent=" + packetLossSent +
                ", dataHoraString='" + dataHoraString + '\'' +
                ", parametroDown=" + parametroDown +
                ", parametroUp=" + parametroUp +
                ", parametroPacotesRecebidos=" + parametroPacotesRecebidos +
                ", ParametroPacotesEnviados=" + ParametroPacotesEnviados +
                '}';
    }


    public Integer getFk_servidor() {
        return fk_servidor;
    }

    public Long getUploadByte() {
        return uploadByte;
    }

    public Long getDownloadByte() {
        return downloadByte;
    }

    public Long getPacketReceived() {
        return packetReceived;
    }

    public Long getPacketSent() {
        return packetSent;
    }

    public Integer getPacketLossReceived() {
        return packetLossReceived;
    }

    public Integer getPacketLossSent() {
        return packetLossSent;
    }

    public String getDataHoraString() {
        return dataHoraString;
    }

    public Integer getParametroDown() {
        return parametroDown;
    }

    public Integer getParametroUp() {
        return parametroUp;
    }

    public Integer getParametroPacotesRecebidos() {
        return parametroPacotesRecebidos;
    }

    public Integer getParametroPacotesEnviados() {
        return ParametroPacotesEnviados;
    }
}
