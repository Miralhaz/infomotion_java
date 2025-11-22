package org.example;

import org.apache.commons.dbcp2.BasicDataSource;

import java.util.Scanner;

public class Connection {
//    Scanner scanner = new Scanner(System.in);

    private BasicDataSource dataSource;

    public Connection(){
        dataSource = new BasicDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl("jdbc:mysql://localhost:3306/infomotion");
          dataSource.setUsername("root");
          dataSource.setPassword("1234");
    }

    public BasicDataSource getDataSource() {
        return dataSource;
    }
}

