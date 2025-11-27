package org.example;

import org.apache.commons.dbcp2.BasicDataSource;
import java.sql.ResultSet;
import java.sql.SQLException;


public class Connection {
//    Scanner scanner = new Scanner(System.in);

    public BasicDataSource dataSource;

    public Connection(){
        dataSource = new BasicDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl("jdbc:mysql://localhost:3306/infomotion");
          dataSource.setUsername("root");
          dataSource.setPassword("Churrasco@20");
    }

    public BasicDataSource getDataSource() {
        return dataSource;
    }

    //willian teste
    public java.sql.Connection getRawConnection() throws SQLException {
        return dataSource.getConnection(); // Assumindo que você tem um atributo 'dataSource'
    }
    //final teste willian
}

