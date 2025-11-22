package org.example;

import org.apache.commons.dbcp2.BasicDataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

public class Connection {
//    Scanner scanner = new Scanner(System.in);

    private BasicDataSource dataSource;

    public Connection(){
        dataSource = new BasicDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl("jdbc:mysql://localhost:3306/infomotion");
          dataSource.setUsername("root");
          dataSource.setPassword("20212412Wi@");
    }

    public BasicDataSource getDataSource() {
        return dataSource;
    }

    //willian teste
    /**
     * Busca o parâmetro 'max' de alerta de DISCO para um servidor específico.
     * @param idServidor O ID do servidor que está sendo processado no momento.
     * @return O valor do limite de disco (em percentual), ou um valor default se falhar ou não encontrar.
     */
    public double getParametroLimiteDisco(int idServidor) {
        double limiteDefault = 85.0; // Valor de segurança padrão

        // A query real fornecida, usando '?' para o ID do servidor
        String sql = "SELECT p.max AS parametro " +
                "FROM servidor AS s " +
                "INNER JOIN parametro_alerta AS p ON p.fk_servidor = s.id " +
                "INNER JOIN componentes AS c ON c.id = p.fk_componente " +
                "INNER JOIN empresa AS e ON e.id = s.fk_empresa " +
                "WHERE c.tipo = 'DISCO' AND s.id = ?"; // Usando '?' como placeholder

        try (java.sql.Connection conn = dataSource.getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {

            // Substitui o placeholder '?' pelo ID do servidor
            stmt.setInt(1, idServidor);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    // Retorna o valor da coluna 'parametro'
                    return rs.getDouble("parametro");
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar parâmetro de limite de disco no banco para o servidor " + idServidor + ": " + e.getMessage());
            return limiteDefault;
        }

        // Retorna o default se não encontrar o parâmetro (pode ser um novo servidor)
        return limiteDefault;
    }
    //final teste willian
}

