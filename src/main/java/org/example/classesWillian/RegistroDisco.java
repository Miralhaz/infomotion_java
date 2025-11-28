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
}
