package org.example;

import java.util.ArrayList;
import java.util.List;

public class LogsEspecificacoes {

    private Integer fkServidor;
    private Double swapTotal;
    private Double ramTotal;
    private Integer quantidadeCpus;
    private Integer quantidadeNucleos;
    private Double capacidadeTotalDisco;
    private List<Particao> particoes;
    private String dataHora;

    // ✔ Construtor chamado pelo main (lista de Strings)
    public LogsEspecificacoes(Integer fkServidor, Double swap, Double ram, Integer qtdCpus,
                              Integer qtdNucleos, Double capacidadeDisco, Double qtdParticoes,
                              List<String> listaParticoesStr, String dataHora) {

        this.fkServidor = fkServidor;
        this.swapTotal = swap;
        this.ramTotal = ram;
        this.quantidadeCpus = qtdCpus;
        this.quantidadeNucleos = qtdNucleos;
        this.capacidadeTotalDisco = capacidadeDisco;
        this.particoes = parseParticoes(listaParticoesStr);
        this.dataHora = dataHora;
    }

    // ✔ Construtor opcional
    public LogsEspecificacoes(Integer fkServidor) {
        this.fkServidor = fkServidor;
        this.swapTotal = 0.0;
        this.ramTotal = 0.0;
        this.quantidadeCpus = 0;
        this.quantidadeNucleos = 0;
        this.capacidadeTotalDisco = 0.0;
        this.particoes = new ArrayList<>();
        this.dataHora = "";
    }

    private List<Particao> parseParticoes(List<String> lista) {
        List<Particao> listaConvertida = new ArrayList<>();

        if (lista == null) return listaConvertida;

        for (String texto : lista) {

            // Ex: "C: 90.5%" → letra = C | uso = 90.5
            texto = texto.trim();
            if (texto.isEmpty()) continue;

            try {
                String[] partes = texto.split(":");
                String letra = partes[0].trim();

                String percentualStr = partes[1].replace("%", "").trim();
                Double percentual = Double.valueOf(percentualStr);

                listaConvertida.add(new Particao(letra, percentual));

            } catch (Exception e) {
                System.out.println("Erro ao converter partição: " + texto);
            }
        }

        return listaConvertida;
    }


    // Getters/Setters
    public Integer getFkServidor() { return fkServidor; }
    public void setFkServidor(Integer fkServidor) { this.fkServidor = fkServidor; }

    public Double getSwapTotal() { return swapTotal; }
    public void setSwapTotal(Double swapTotal) { this.swapTotal = swapTotal; }

    public Double getRamTotal() { return ramTotal; }
    public void setRamTotal(Double ramTotal) { this.ramTotal = ramTotal; }

    public Integer getQuantidadeCpus() { return quantidadeCpus; }
    public void setQuantidadeCpus(Integer quantidadeCpus) { this.quantidadeCpus = quantidadeCpus; }

    public Integer getQuantidadeNucleos() { return quantidadeNucleos; }
    public void setQuantidadeNucleos(Integer quantidadeNucleos) { this.quantidadeNucleos = quantidadeNucleos; }

    public Double getCapacidadeTotalDisco() { return capacidadeTotalDisco; }
    public void setCapacidadeTotalDisco(Double capacidadeTotalDisco) { this.capacidadeTotalDisco = capacidadeTotalDisco; }

    public List<Particao> getParticoes() { return particoes; }
    public void setParticoes(List<Particao> particoes) { this.particoes = particoes; }

    public String getDataHora() { return dataHora; }
    public void setDataHora(String dataHora) { this.dataHora = dataHora; }

    @Override
    public String toString() {
        return "LogsEspecificacoes{fk=" + fkServidor + ", particoes=" + particoes + "}";
    }
}
