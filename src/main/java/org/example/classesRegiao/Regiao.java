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

    public Double retornarResidual(){
        Integer media = 0;

        for (int i = 0; i < listaLogRegiao.size(); i++) {
            media +=  listaLogRegiao.get(i).getQtdRequisicoes();
        }
        media = media / listaLogRegiao.size();

        Double totalResiduo = 0.0;
        for (int i = 0; i < listaLogRegiao.size(); i++) {
            Integer qtdReq = listaLogRegiao.get(i).getQtdRequisicoes();
            Integer residuo = 0;
            if (media > qtdReq){
                residuo = media - qtdReq;
            }else {
                residuo = qtdReq - media;
            }
            totalResiduo += Math.pow(residuo,2.0);
        }
        return totalResiduo / listaLogRegiao.size();
    }

    public Integer retornarMediana(){
        List<LogRegiao> l = listaLogRegiao;


        for (int i = 0; i < l.size(); i++) {
                for (int j = 1; j < l.size(); j++) {
                    if (l.get(j - 1).getQtdRequisicoes() > l.get(j).getQtdRequisicoes()) {
                        LogRegiao aux = l.get(j);
                        l.set(j, l.get(j - 1));
                        l.set(j - 1, aux);
                    }
                }
            }

        if (l.size() % 2 == 0 ){
            return    l.get(l.size() / 2 - 1).getQtdRequisicoes();
        }else {
            return    l.get(l.size() / 2 - 2).getQtdRequisicoes();
        }



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
