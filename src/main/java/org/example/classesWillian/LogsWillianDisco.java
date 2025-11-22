package org.example.classesWillian;

import java.time.LocalDateTime;
import org.example.AwsConnection;
import org.example.Logs;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.File;
import java.io.FileOutputStream;
import java.time.format.DateTimeFormatter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class LogsWillianDisco {
    private Integer fkServidor;
    private String nomeMaquina;
    private LocalDateTime timestamp;
    private Double usoDisco;
    private Double temperaturaDisco;

    public LogsWillianDisco(Integer fkServidor, String nomeMaquina, LocalDateTime timestamp,
                            Double usoDisco, Double temperaturaDisco) {
        this.fkServidor = fkServidor;
        this.nomeMaquina = nomeMaquina;
        this.timestamp = timestamp;
        this.usoDisco = usoDisco;
        this.temperaturaDisco = temperaturaDisco;
    }

    public Integer getFkServidor() { return fkServidor; }
    public String getNomeMaquina() { return nomeMaquina; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public Double getUsoDisco() { return usoDisco; }
    public Double getTemperaturaDisco() { return temperaturaDisco; }
}



