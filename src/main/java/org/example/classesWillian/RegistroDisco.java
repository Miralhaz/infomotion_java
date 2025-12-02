package org.example.classesWillian;

import java.time.LocalDateTime;

public class RegistroDisco {
    public Integer fk_servidor;
    public Integer fk_empresa;
    public String nomeMaquina;
    public LocalDateTime timestamp;
    public Double disco; // uso em %
    public Double temperatura_disco;
    public Integer quantidade_processos;
    public Double numero_leituras;
    public Double numero_escritas;
    public Double bytes_lidos;
    public Double bytes_escritos;
    public Double tempo_leitura;
    public Double tempo_escrita;

    public RegistroDisco(){

    }

    public RegistroDisco(Integer fk_servidor, Integer fk_empresa, String nomeMaquina, LocalDateTime timestamp, Double disco, Double temperatura_disco, Integer quantidade_processos, Double numero_leituras, Double numero_escritas, Double bytes_lidos, Double bytes_escritos, Double tempo_leitura, Double tempo_escrita) {
        this.fk_servidor = fk_servidor;
        this.fk_empresa = fk_empresa;
        this.nomeMaquina = nomeMaquina;
        this.timestamp = timestamp;
        this.disco = disco;
        this.temperatura_disco = temperatura_disco;
        this.quantidade_processos = quantidade_processos;
        this.numero_leituras = numero_leituras;
        this.numero_escritas = numero_escritas;
        this.bytes_lidos = bytes_lidos;
        this.bytes_escritos = bytes_escritos;
        this.tempo_leitura = tempo_leitura;
        this.tempo_escrita = tempo_escrita;
    }

    public void setFk_servidor(Integer fk_servidor) {
        this.fk_servidor = fk_servidor;
    }

    public void setFk_empresa(Integer fk_empresa) {
        this.fk_empresa = fk_empresa;
    }

    public void setNomeMaquina(String nomeMaquina) {
        this.nomeMaquina = nomeMaquina;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public void setDisco(Double disco) {
        this.disco = disco;
    }

    public void setTemperatura_disco(Double temperatura_disco) {
        this.temperatura_disco = temperatura_disco;
    }

    public void setQuantidade_processos(Integer quantidade_processos) {
        this.quantidade_processos = quantidade_processos;
    }

    public void setNumero_leituras(Double numero_leituras) {
        this.numero_leituras = numero_leituras;
    }

    public void setNumero_escritas(Double numero_escritas) {
        this.numero_escritas = numero_escritas;
    }

    public void setBytes_lidos(Double bytes_lidos) {
        this.bytes_lidos = bytes_lidos;
    }

    public void setBytes_escritos(Double bytes_escritos) {
        this.bytes_escritos = bytes_escritos;
    }

    public void setTempo_leitura(Double tempo_leitura) {
        this.tempo_leitura = tempo_leitura;
    }

    public void setTempo_escrita(Double tempo_escrita) {
        this.tempo_escrita = tempo_escrita;
    }
}
