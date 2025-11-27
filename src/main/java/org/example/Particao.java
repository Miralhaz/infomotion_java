package org.example;

public class Particao {
    private String nome;
    private Double uso;

    public Particao(String nome, Double uso) {
        this.nome = nome;
        this.uso = uso;
    }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public Double getUso() { return uso; }
    public void setUso(Double uso) { this.uso = uso; }

    public boolean isEmpty() {
        return (nome == null || nome.isBlank()) && (uso == null);
    }

    @Override
    public String toString() {
        return "Particao{name='" + nome + "', uso=" + uso + "}";
    }
}
