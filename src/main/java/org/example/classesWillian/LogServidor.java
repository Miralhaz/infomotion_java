package org.example.classesWillian;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
// Apenas as colunas que você decidiu manter:
public class LogServidor {
    public int fk_servidor;
    public String nomeMaquina;
    public double disco;
    public double temperatura_disco;
    public int quantidade_processos;
    public double numero_leituras;
    public double numero_escritas;
    public double bytes_lidos;
    public double bytes_escritos;
    public int tempo_leitura;
    public int tempo_escrita;
    public String timestamp; // Manter como String para a regressão

    public LocalDateTime timestampObj;

    // Construtor e Getters/Setters omitidos por brevidade, mas devem ser adicionados.
    public LogServidor() {
    }

    public LogServidor(int fk_servidor, String nomeMaquina, double disco, double temperatura_disco, int quantidade_processos, double numero_leituras, double numero_escritas, double bytes_lidos, double bytes_escritos, int tempo_leitura, int tempo_escrita, String timestamp) {
        this.fk_servidor = fk_servidor;
        this.nomeMaquina = nomeMaquina;
        this.disco = disco;
        this.temperatura_disco = temperatura_disco;
        this.quantidade_processos = quantidade_processos;
        this.numero_leituras = numero_leituras;
        this.numero_escritas = numero_escritas;
        this.bytes_lidos = bytes_lidos;
        this.bytes_escritos = bytes_escritos;
        this.tempo_leitura = tempo_leitura;
        this.tempo_escrita = tempo_escrita;
        this.timestamp = timestamp;
    }

    public int getFk_servidor() {
        return fk_servidor;
    }

    public void setFk_servidor(int fk_servidor) {
        this.fk_servidor = fk_servidor;
    }

    public String getNomeMaquina() {
        return nomeMaquina;
    }

    public void setNomeMaquina(String nomeMaquina) {
        this.nomeMaquina = nomeMaquina;
    }

    public double getDisco() {
        return disco;
    }

    public void setDisco(double disco) {
        this.disco = disco;
    }

    public double getTemperatura_disco() {
        return temperatura_disco;
    }

    public void setTemperatura_disco(double temperatura_disco) {
        this.temperatura_disco = temperatura_disco;
    }

    public int getQuantidade_processos() {
        return quantidade_processos;
    }

    public void setQuantidade_processos(int quantidade_processos) {
        this.quantidade_processos = quantidade_processos;
    }

    public double getNumero_leituras() {
        return numero_leituras;
    }

    public void setNumero_leituras(double numero_leituras) {
        this.numero_leituras = numero_leituras;
    }

    public double getNumero_escritas() {
        return numero_escritas;
    }

    public void setNumero_escritas(double numero_escritas) {
        this.numero_escritas = numero_escritas;
    }

    public double getBytes_lidos() {
        return bytes_lidos;
    }

    public void setBytes_lidos(double bytes_lidos) {
        this.bytes_lidos = bytes_lidos;
    }

    public double getBytes_escritos() {
        return bytes_escritos;
    }

    public void setBytes_escritos(double bytes_escritos) {
        this.bytes_escritos = bytes_escritos;
    }

    public int getTempo_leitura() {
        return tempo_leitura;
    }

    public void setTempo_leitura(int tempo_leitura) {
        this.tempo_leitura = tempo_leitura;
    }

    public int getTempo_escrita() {
        return tempo_escrita;
    }

    public void setTempo_escrita(int tempo_escrita) {
        this.tempo_escrita = tempo_escrita;
    }

    public LocalDateTime getTimestampObj() {
        return timestampObj;
    }

    public String getTimestamp() {
        return timestamp;
    }

    // Para simplificar a ETL, podemos usar campos públicos e o ObjectMapper.

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
        try {
            // Formato do seu CSV: "20/11/2025 20:22:05"
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss", new Locale("pt", "BR"));
            this.timestampObj = LocalDateTime.parse(timestamp, formatter);
        } catch (Exception e) {
            // Se falhar a conversão (pode acontecer com dados incompletos)
            this.timestampObj = null;
        }
    }
}