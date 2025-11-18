package org.example.classesMiralha;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LogsMiralhaDisco {

        private Integer fk_servidor;
        private LocalDateTime dataHora;
        private Double usoDisco;
        private Double tempDisco;

        public LogsMiralhaDisco(Integer fk_servidor, String dataHoraString, Double usoDisco, Double tempDisco) {
            this.fk_servidor = fk_servidor;
            this.dataHora = LocalDateTime.parse(dataHoraString, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            this.usoDisco = usoDisco;
            this.tempDisco = tempDisco;
        }

        public Integer getFk_servidor() {
            return fk_servidor;
        }

        public LocalDateTime getDataHora() {
            return dataHora;
        }

        public String getDataHoraFormatada() {
            return dataHora.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        }

        public Double getUsoDisco() {
            return usoDisco;
        }

        public Double getTempDisco() {
            return tempDisco;
        }
}