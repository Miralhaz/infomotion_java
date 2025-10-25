package org.example;

import static java.lang.Double.valueOf;

public class Parametro_alerta {
   private Integer id;
   private Integer fk_componente;
   private Integer fk_servidor;
   private String max;
   private String duracao_min;
   private String unidadeMedida;


    public Parametro_alerta(Integer id, Integer fk_componente, Integer fk_servidor, String max, String duracao_min, String unidadeMedida) {
        this.id = id;
        this.fk_componente = fk_componente;
        this.fk_servidor = fk_servidor;
        this.max = max;
        this.duracao_min = duracao_min;
        this.unidadeMedida = unidadeMedida;
    }

    public Parametro_alerta() {
    }

    public Integer getFk_componente() {
        return fk_componente;
    }

    public void setFk_componente(Integer fk_componente) {
        this.fk_componente = fk_componente;
    }

    public Integer getFk_servidor() {
        return fk_servidor;
    }

    public void setFk_servidor(Integer fk_servidor) {
        this.fk_servidor = fk_servidor;
    }

    public Double getMax() {
        return valueOf(max);
    }

    public void setMax(String max) {
        this.max = max;
    }

    public Integer getDuracao_min() {
        return Integer.parseInt(duracao_min);
    }

    public void setDuracao_min(String duracao_min) {
        this.duracao_min = duracao_min;
    }

    public String getUnidadeMedida() {
        return unidadeMedida;
    }

    public void setUnidadeMedida(String unidadeMedida) {
        this.unidadeMedida = unidadeMedida;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "Parametro_alerta{" +
                "id=" + id +
                ", fk_componente=" + fk_componente +
                ", fk_servidor=" + fk_servidor +
                ", max='" + max + '\'' +
                ", duracao_min='" + duracao_min + '\'' +
                ", unidadeMedida='" + unidadeMedida + '\'' +
                '}';
    }
}
