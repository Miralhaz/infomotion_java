package org.example.classesRede;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;


import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LogConexao {

    @JsonProperty("fk_servidor")
    private Integer fk_servidor;
    @JsonProperty("timestamp")
    private String dataHoraString;
    private LocalDateTime dataHora;
    @JsonProperty("pid")
    private Integer idProcessoConexao;
    @JsonProperty("")
    private String nomeConexao;
    @JsonProperty("raddr")
    private String raddr;
    @JsonProperty("laddr")
    private String laddr;
    @JsonProperty("status")
    private String status;
    private static final DateTimeFormatter INPUT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");



    public LogConexao(Integer fk_servidor, Integer idProcessoConexao, String nomeConexao, String raddr, String laddr, String status, String dataHoraString) {
        this.fk_servidor = fk_servidor;
        this.idProcessoConexao = idProcessoConexao;
        this.nomeConexao = nomeConexao;
        this.raddr = raddr;
        this.laddr = laddr;
        this.status = status;
        this.dataHoraString = dataHoraString;
    }

    @Override
    public String toString() {
        return "LogConexao{" +
                "fk_servidor=" + fk_servidor +
                ", idProcessoConexao=" + idProcessoConexao +
                ", nomeConexao='" + nomeConexao + '\'' +
                ", raddr='" + raddr + '\'' +
                ", laddr='" + laddr + '\'' +
                ", status='" + status + '\'' +
                ", dataHoraString='" + dataHoraString + '\'' +
                '}';
    }

    public void dataHoraFormatter(String dataHora) {
        DateTimeFormatter LocalDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        LocalDateTime dataUnformatter = LocalDateTime.parse(dataHora,LocalDateTimeFormatter);
        String dataTimeFormatterString = dataUnformatter.format(formatter);
        this.dataHora = LocalDateTime.parse(dataTimeFormatterString,formatter);
    }

    public Integer getFk_servidor() {
        return fk_servidor;
    }

    public Integer getIdProcessoConexao() {
        return idProcessoConexao;
    }

    public String getNomeConexao() {
        return nomeConexao;
    }

    public String getRaddr() {
        return raddr;
    }

    public String getLaddr() {
        return laddr;
    }

    public String getStatus() {
        return status;
    }

    public String getDataHoraString() {
        return dataHoraString;
    }
}
