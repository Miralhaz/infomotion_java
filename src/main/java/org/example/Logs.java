package org.example;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Logs {
    @JsonProperty("nomeMaquina")
    private String nomeMaquina;
    @JsonProperty("timestamp")
    private String dataHoraString;
    @JsonProperty("cpu")
    private Double cpu;
    @JsonProperty("ram")
    private Double ram;
    @JsonProperty("disco")
    private Double disco;
    @JsonProperty("temperatura_cpu")
    private Double tmp_cpu;
    @JsonProperty("temperatura_disco")
    private Double tmp_disco;
    @JsonProperty("memoria_swap")
    private Double memoria_swap;
    @JsonProperty("quantidade_processos")
    private Integer qtd_processos;
    @JsonProperty("fk_servidor")
    private Integer fk_servidor;
    @JsonProperty("download_bytes")
    private Integer download_bytes;
    @JsonProperty("upload_bytes")
    private Integer upload_bytes;
    @JsonProperty("pacotes_recebidos")
    private Integer pacotes_recebidos;
    @JsonProperty("pacotes_enviados")
    private Integer pacotes_enviados;
    @JsonProperty("dropin")
    private Integer dropin;
    @JsonProperty("dropout")
    private Integer dropout;
    @JsonProperty("numero_leituras")
    private Integer numero_leituras;
    @JsonProperty("numero_escritas")
    private Integer numero_escritas;
    @JsonProperty("bytes_lidos")
    private Long bytes_lidos;
    @JsonProperty("bytes_escritos")
    private Long bytes_escritos;
    @JsonProperty("tempo_leitura")
    private Integer tempo_leitura;
    @JsonProperty("tempo_escrita")
    private Integer tempo_escrita;

    private LocalDateTime dataHora;

    public Logs(Integer fk_servidor,
                String nomeMaquina,
                String dataHoraString,
                Double cpu,
                Double ram,
                Double disco,
                Double tmp_cpu,
                Double tmp_disco,
                Double memoria_swap,
                Integer qtd_processos,
                Integer download_bytes,
                Integer upload_bytes,
                Integer pacotes_recebidos,
                Integer pacotes_enviados,
                Integer dropin,
                Integer dropout,
                Integer numero_leituras,
                Integer numero_escritas,
                Long bytes_lidos,
                Long bytes_escritos,
                Integer tempo_leitura,
                Integer tempo_escrita) {
        this.fk_servidor = fk_servidor;
        this.nomeMaquina = nomeMaquina;
        this.dataHoraString = dataHoraString;
        this.cpu = cpu;
        this.ram = ram;
        this.disco = disco;
        this.tmp_cpu = tmp_cpu;
        this.tmp_disco = tmp_disco;
        this.memoria_swap = memoria_swap;
        this.qtd_processos = qtd_processos;
        this.download_bytes = download_bytes;
        this.upload_bytes = upload_bytes;
        this.pacotes_recebidos = pacotes_recebidos;
        this.pacotes_enviados = pacotes_enviados;
        this.dropin = dropin;
        this.dropout = dropout;
        this.numero_leituras = numero_leituras;
        this.numero_escritas = numero_escritas;
        this.bytes_lidos = bytes_lidos;
        this.bytes_escritos = bytes_escritos;
        this.tempo_leitura = tempo_leitura;
        this.tempo_escrita = tempo_escrita;
        DateTimeFormatter LocalDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        LocalDateTime dataUnformatter = LocalDateTime.parse(dataHoraString,LocalDateTimeFormatter);
        String dataTimeFormatterString = dataUnformatter.format(formatter);
        this.dataHora = LocalDateTime.parse(dataTimeFormatterString,formatter);
    }

    public Logs() {
    }


    public Double getCpu() {
        return cpu;
    }

    public Double getRam() {
        return ram;
    }

    public Double getDisco() {
        return disco;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public String getDataHoraString() {
        return dataHoraString;
    }

    public void setCpu(Double cpu) {
        this.cpu = cpu;
    }

    public void setRam(Double ram) {
        this.ram = ram;
    }

    public void setDisco(Double disco) {
        this.disco = disco;
    }

    public void setDataHoraString(String dataHoraString) {
        this.dataHoraString = dataHoraString;
        dataHoraFormatter(dataHoraString);
    }

    public void dataHoraFormatter(String dataHora) {
        DateTimeFormatter LocalDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        LocalDateTime dataUnformatter = LocalDateTime.parse(dataHora,LocalDateTimeFormatter);
        String dataTimeFormatterString = dataUnformatter.format(formatter);
        this.dataHora = LocalDateTime.parse(dataTimeFormatterString,formatter);
    }

    public Double getTmp_cpu() {
        return tmp_cpu;
    }

    public void setTmp_cpu(Double tmp_cpu) {
        this.tmp_cpu = tmp_cpu;
    }

    public Double getTmp_disco() {
        return tmp_disco;
    }

    public void setTmp_disco(Double tmp_disco) {
        this.tmp_disco = tmp_disco;
    }

    public Double getMemoria_swap() {
        return memoria_swap;
    }

    public void setMemoria_swap(Double memoria_swap) {
        this.memoria_swap = memoria_swap;
    }

    public Integer getQtd_processos() {
        return qtd_processos;
    }

    public void setQtd_processos(Integer qtd_processos) {
        this.qtd_processos = qtd_processos;
    }

    public Integer getFk_servidor() {
        return fk_servidor;
    }

    public void setFk_servidor(Integer fk_servidor) {
        this.fk_servidor = fk_servidor;
    }

    public String getNomeMaquina() {
        return nomeMaquina;
    }

    public void setNomeMaquina(String nomeMaquina) {
        this.nomeMaquina = nomeMaquina;
    }

    public Integer getDownload_bytes() {
        return download_bytes;
    }

    public void setDownload_bytes(Integer download_bytes) {
        this.download_bytes = download_bytes;
    }

    public Integer getUpload_bytes() {
        return upload_bytes;
    }

    public void setUpload_bytes(Integer upload_bytes) {
        this.upload_bytes = upload_bytes;
    }

    public Integer getPacotes_recebidos() {
        return pacotes_recebidos;
    }

    public void setPacotes_recebidos(Integer pacotes_recebidos) {
        this.pacotes_recebidos = pacotes_recebidos;
    }

    public Integer getPacotes_enviados() {
        return pacotes_enviados;
    }

    public void setPacotes_enviados(Integer pacotes_enviados) {
        this.pacotes_enviados = pacotes_enviados;
    }

    public Integer getDropin() {
        return dropin;
    }

    public void setDropin(Integer dropin) {
        this.dropin = dropin;
    }

    public Integer getDropout() {
        return dropout;
    }

    public void setDropout(Integer dropout) {
        this.dropout = dropout;
    }

    public Integer getNumero_leituras() {
        return numero_leituras;
    }

    public void setNumero_leituras(Integer numero_leituras) {
        this.numero_leituras = numero_leituras;
    }

    public Integer getNumero_escritas() {
        return numero_escritas;
    }

    public void setNumero_escritas(Integer numero_escritas) {
        this.numero_escritas = numero_escritas;
    }

    public Long getBytes_lidos() {
        return bytes_lidos;
    }

    public void setBytes_lidos(Long bytes_lidos) {
        this.bytes_lidos = bytes_lidos;
    }

    public Long getBytes_escritos() {
        return bytes_escritos;
    }

    public void setBytes_escritos(Long bytes_escritos) {
        this.bytes_escritos = bytes_escritos;
    }

    public Integer getTempo_leitura() {
        return tempo_leitura;
    }

    public void setTempo_leitura(Integer tempo_leitura) {
        this.tempo_leitura = tempo_leitura;
    }

    public Integer getTempo_escrita() {
        return tempo_escrita;
    }

    public void setTempo_escrita(Integer tempo_escrita) {
        this.tempo_escrita = tempo_escrita;
    }

    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        return "\n Data e Hora:" + dataHora.format(formatter) +
                " | CPU:" + cpu +
                " | RAM:" + ram +
                " | Disco:" + disco +
                " | Temperatura CPU:" + tmp_cpu +
                " | Temperatura Disco:" + tmp_disco +
                " | Memória Swap:" + memoria_swap +
                " | Quantidade Processos:" + qtd_processos +
                " | ID Servidor:" + fk_servidor +
                " | Nome Máquina:" + nomeMaquina +
                " | Download Bytes:" + download_bytes +
                " | Upload Bytes:" + upload_bytes +
                " | Pacotes Recebidos:" + pacotes_recebidos +
                " | Pacotes Enviados:" + pacotes_enviados +
                " | dropin:" + dropin +
                " | dropout:" + dropout +
                " | Número Leituras:" + numero_leituras +
                " | Número Escritas:" + numero_escritas +
                " | Bytes Lidos:" + bytes_lidos +
                " | Bytes Escritos:" + bytes_escritos +
                " | Tempo Leitura:" + tempo_leitura +
                " | Tempo Escrita:" + tempo_escrita;
    }
}