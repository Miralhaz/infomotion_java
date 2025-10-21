package org.example;

import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.springframework.jdbc.core.JdbcOperationsExtensionsKt.query;

public class testBanco {
    public static void main(String[] args) {

        Connection connection = new Connection();

        JdbcTemplate con = new JdbcTemplate(connection.getDataSource());

        List<Parametro_alerta> metricasList = con.query("SELECT * FROM parametro_alerta;",
                new BeanPropertyRowMapper<>(Parametro_alerta.class));
        System.out.println(metricasList);

    }
}
