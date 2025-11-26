package org.example.classesRegiao;

import java.util.ArrayList;
import java.util.List;

public class Regiao {
    private Integer id;
    private List<LogClima> listaLogClima;
    private List<LogRegiao> listaLogRegiao;


    public Regiao() {
    }

    public Regiao(Integer id) {
        this.id = id;
        this.listaLogRegiao = new ArrayList<>();
        this.listaLogClima = new ArrayList<>();
    }


    public Integer getId() {
        return id;
    }


    public List<LogClima> getListaLogClima() {
        return listaLogClima;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setListaLogClima(List<LogClima> listaLogClima) {
        this.listaLogClima = listaLogClima;
    }

    public List<LogRegiao> getListaLogRegiao() {
        return listaLogRegiao;
    }

    public void setListaLogRegiao(List<LogRegiao> listaLogRegiao) {
        this.listaLogRegiao = listaLogRegiao;
    }

    @Override
    public String toString() {
        return "Regiao{" +
                "id=" + id +
                ", listaLogClima=" + listaLogClima +
                ", listaLogRegiao=" + listaLogRegiao +
                '}';
    }
}
