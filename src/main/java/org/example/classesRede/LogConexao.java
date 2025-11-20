package org.example.classesRede;

public class LogConexao {

    private String nomeConexao;
    private String raddr;
    private String laddr;
    private String status;
    private Integer idProcessoConexao;

    public LogConexao(String nomeConexao, String raddr, String laddr, String status, Integer idProcessoConexao) {
        this.nomeConexao = nomeConexao;
        this.raddr = raddr;
        this.laddr = laddr;
        this.status = status;
        this.idProcessoConexao = idProcessoConexao;
    }

    @Override
    public String toString() {
        return "LogConexao{" +
                "nomeConexao='" + nomeConexao + '\'' +
                ", raddr='" + raddr + '\'' +
                ", laddr='" + laddr + '\'' +
                ", status='" + status + '\'' +
                ", idProcessoConexao=" + idProcessoConexao +
                '}';
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

    public Integer getIdProcessoConexao() {
        return idProcessoConexao;
    }
}
