package org.example;

import java.time.LocalDate;

public class Servidor {
    private Integer id;
    private Integer fk_empresa;
    private Integer fk_regiao;
    private String apelido;
    private String ip;
    private LocalDate dt_cadastro;

    public Servidor(Integer id, Integer fk_empresa, Integer fk_regiao, String apelido, String ip, LocalDate dt_cadastro) {
        this.id = id;
        this.fk_empresa = fk_empresa;
        this.fk_regiao = fk_regiao;
        this.apelido = apelido;
        this.ip = ip;
        this.dt_cadastro = dt_cadastro;
    }

    public Servidor() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getFk_empresa() {
        return fk_empresa;
    }

    public void setFk_empresa(Integer fk_empresa) {
        this.fk_empresa = fk_empresa;
    }

    public Integer getFk_regiao() {
        return fk_regiao;
    }

    public void setFk_regiao(Integer fk_regiao) {
        this.fk_regiao = fk_regiao;
    }

    public String getApelido() {
        return apelido;
    }

    public void setApelido(String apelido) {
        this.apelido = apelido;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public LocalDate getDt_cadastro() {
        return dt_cadastro;
    }

    public void setDt_cadastro(LocalDate dt_cadastro) {
        this.dt_cadastro = dt_cadastro;
    }

    @Override
    public String toString() {
        return "Servidor{" +
                "id=" + id +
                ", fk_empresa=" + fk_empresa +
                ", fk_regiao=" + fk_regiao +
                ", apelido='" + apelido + '\'' +
                ", ip='" + ip + '\'' +
                ", dt_cadastro=" + dt_cadastro +
                '}';
    }
}
