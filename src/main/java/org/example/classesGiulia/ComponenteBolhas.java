package org.example.classesGiulia;

public class ComponenteBolhas {

    // Atributos:
    private Integer minutosAcima;
    private Double max;

    // Construtor:
    public ComponenteBolhas(Integer minutosAcima, Double max) {
        this.minutosAcima = minutosAcima;
        this.max = max;
    }

    // Métodos:
    // Getters e setters:
    public Integer getMinutosAcima() {
        return minutosAcima;
    }

    public void setMinutosAcima(Integer minutosAcima) {
        this.minutosAcima = minutosAcima;
    }

    public Double getMax() {
        return max;
    }

    public void setMax(Double max) {
        this.max = max;
    }

    // toString():
    @Override
    public String toString() {
        return " | minutosAcima:" + minutosAcima +
                " | max:" + max;
    }
}
