package org.example;

import java.time.LocalTime;

public class Metricas {
   private String componente;
   private Double max;
   private LocalTime duracao_min;

    public Metricas(String componente, Double max, LocalTime duracao_min) {
        this.componente = componente;
        this.max = max;
        this.duracao_min = duracao_min;
    }

    public String getComponente() {
        return componente;
    }

    public void setComponente(String componente) {
        this.componente = componente;
    }

    public Double getMax() {
        return max;
    }

    public void setMax(Double max) {
        this.max = max;
    }

    public LocalTime getDuracao_min() {
        return duracao_min;
    }

    public void setDuracao_min(LocalTime duracao_min) {
        this.duracao_min = duracao_min;
    }
}
