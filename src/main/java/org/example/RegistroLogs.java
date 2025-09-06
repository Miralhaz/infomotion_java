package org.example;
import java.util.*;

public class RegistroLogs {
    private String usuario;
    private String ip;
    private Double cpu;
    private Double ram;
    private Double disco;
    private String dataHora;

    public RegistroLogs(String usuario, String ip, Double cpu, Double ram, Double disco, String dataHora) {
        this.usuario = usuario;
        this.ip = ip;
        this.cpu = cpu;
        this.ram = ram;
        this.disco = disco;
        this.dataHora = dataHora;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getIp() {
        return this.ip;
    }

    public Double getCpu() {
        return this.cpu;
    }

    public Double getRam() {
        return this.ram;
    }

    public Double getDisco() {
        return this.disco;
    }

    public String getData() {
        return this.dataHora;
    }

    public String toString() {
        return "USER: " + this.usuario + " | " +"IP: " + this.ip + " | " +"CPU: " + this.cpu+"%" + " | " +"RAM: " + this.ram +" | "+ "DISCO: " + this.disco+"%"+ " | " +"DATA: " + this.dataHora+ "\n";
    }
}
