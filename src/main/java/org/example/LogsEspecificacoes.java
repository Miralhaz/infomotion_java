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

    public LogsEspecificacoes(Integer fkServidor, Double swap, Double ram, Integer qtdCpus,
                              Integer qtdNucleos, Double capacidadeDisco, Double qtdParticoes,
                              List<String> listaParticoes, String dataHora) {

        this.fkServidor = fkServidor;
        this.swapTotal = swap;
        this.ramTotal = ram;
        this.quantidadeCpus = qtdCpus;
        this.quantidadeNucleos = qtdNucleos;
        this.capacidadeTotalDisco = capacidadeDisco;
        this.dataHora = dataHora;

        if (listaParticoes != null && !listaParticoes.isEmpty()) {

            this.particoes = new ArrayList<>();

            for (String texto : listaParticoes) {
                this.particoes.addAll(parseParticoes(texto));
            }

        } else {
            this.particoes = new ArrayList<>(); // evita NPE
        }
    }


    public static List<Particao> parseParticoes(String textoParticoes) {
        List<Particao> lista = new ArrayList<>();

        if (textoParticoes == null || textoParticoes.isEmpty()) {
            return lista;
        }

        String[] blocos = textoParticoes.split("\\|");

        for (String bloco : blocos) {
            bloco = bloco.trim();

            String[] partes = bloco.split(":");

            if (partes.length < 3) continue;

            String nome = partes[0].trim();
            String valorStr = partes[2].replace("%","").trim(); // 88.7

            Double uso = Double.valueOf(valorStr);

            lista.add(new Particao(nome, uso));
        }

        return lista;
    }



    public Integer getFkServidor() {
        return fkServidor;
    }

    public void setFkServidor(Integer fkServidor) {
        this.fkServidor = fkServidor;
    }

    public Double getSwapTotal() {
        return swapTotal;
    }

    public void setSwapTotal(Double swapTotal) {
        this.swapTotal = swapTotal;
    }

    public Double getRamTotal() {
        return ramTotal;
    }

    public void setRamTotal(Double ramTotal) {
        this.ramTotal = ramTotal;
    }

    public Integer getQuantidadeCpus() {
        return quantidadeCpus;
    }

    public void setQuantidadeCpus(Integer quantidadeCpus) {
        this.quantidadeCpus = quantidadeCpus;
    }

    public Integer getQuantidadeNucleos() {
        return quantidadeNucleos;
    }

    public void setQuantidadeNucleos(Integer quantidadeNucleos) {
        this.quantidadeNucleos = quantidadeNucleos;
    }

    public Double getCapacidadeTotalDisco() {
        return capacidadeTotalDisco;
    }

    public void setCapacidadeTotalDisco(Double capacidadeTotalDisco) {
        this.capacidadeTotalDisco = capacidadeTotalDisco;
    }

    public List<Particao> getParticoes() {
        return particoes;
    }

    public void setParticoes(List<Particao> particoes) {
        this.particoes = particoes;
    }

    public String getDataHora() {
        return dataHora;
    }

    public void setDataHora(String dataHora) {
        this.dataHora = dataHora;
    }
}

