package org.example;

public class Particao {

    private String nome;
    private Double uso;

    public Particao(String nome, Double uso) {
        this.nome = nome;
        this.uso = uso;
    }

    public String getNome() {
        return nome;
    }

    public Double getUso() {
        return uso;
    }
}