package org.example.classesRede;


import software.amazon.awssdk.services.s3.endpoints.internal.Value;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LogRede {
    private Integer id;
    private Integer fk_servidor;
    private LocalDateTime timeStamp;
    private Long uploadByte;
    private Long downloadByte;
    private Long packetReceived;
    private Long packetSent;
    private Integer packetLossReceived;
    private Integer packetLossSent;
    private String dataHoraString;
    private static final DateTimeFormatter RAW_INPUT_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");



    public LogRede(String dataHoraString, Long uploadByte, Long downloadByte, Long packetReceived, Long packetSent, Integer packetLossReceived, Integer packetLossSent, Integer fk_servidor) {
        this.dataHoraString =dataHoraString;
        this.uploadByte = uploadByte;
        this.downloadByte = downloadByte;
        this.packetReceived = packetReceived;
        this.packetSent = packetSent;
        this.packetLossReceived = packetLossReceived;
        this.packetLossSent = packetLossSent;
        this.fk_servidor = fk_servidor;

    }

    @Override
    public String toString() {
        return "LogRede{" +
                "id=" + id +
                ", fk_servidor=" + fk_servidor +
                ", timeStamp=" + timeStamp +
                ", uploadByte=" + uploadByte +
                ", downloadByte=" + downloadByte +
                ", packetReceived=" + packetReceived +
                ", packetSent=" + packetSent +
                ", packetLossReceived=" + packetLossReceived +
                ", packetLossSent=" + packetLossSent +
                '}';
    }

    public String getDataHoraString() {
        return dataHoraString;
    }

    public  Integer getId() {
        return id;
    }

    public LocalDateTime getTimeStamp() {
        return timeStamp;
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

    public Integer getFk_servidor() {
        return fk_servidor;
    }
}
