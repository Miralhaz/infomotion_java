package org.example.classesWillian;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.EmptyResultDataAccessException;

public class BancoParametros {

    private JdbcTemplate jdbc;

    public BancoParametros(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Double buscarLimiteDisco(Integer idServidor) {
        try {
            return jdbc.queryForObject(
                    "SELECT limite_disco FROM parametros WHERE fk_servidor = ?",
                    Double.class,
                    idServidor
            );
        } catch (EmptyResultDataAccessException e) {
            return 80.0; // fallback
        }
    }
}
