package org.example.classesRegiao;

import java.time.LocalTime;

public class LogHorarioReq {
    private LocalTime horario;
    private Integer requisicao;


    public LogHorarioReq(LocalTime horario, Integer requisicao) {
        this.horario = horario;
        this.requisicao = requisicao;
    }

    public LocalTime getHorario() {
        return horario;
    }

    public void setHorario(LocalTime horario) {
        this.horario = horario;
    }

    public Integer getRequisicao() {
        return requisicao;
    }

    public void setRequisicao(Integer requisicao) {
        this.requisicao = requisicao;
    }

    @Override
    public String toString() {
        return "LogHorarioReq{" +
                "horario=" + horario +
                ", requisicao=" + requisicao +
                '}';
    }
}
