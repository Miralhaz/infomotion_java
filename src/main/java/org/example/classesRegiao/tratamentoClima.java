package org.example.classesRegiao;

import org.example.AwsConnection;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.format.DateTimeFormatter;

public class tratamentoClima {    private static final DateTimeFormatter INPUT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final JdbcTemplate jdbcTemplate;
    private final AwsConnection awsConnection;

    public tratamentoClima(AwsConnection awsConnection, JdbcTemplate jdbcTemplate) {
        this.awsConnection = awsConnection;
        this.jdbcTemplate = jdbcTemplate;
    }


    public void tratamentoQtdReqPrev(){



    }
}
