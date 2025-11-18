package org.example;

import org.apache.commons.dbcp2.BasicDataSource;

import java.util.Scanner;

public class Connection {
//    Scanner scanner = new Scanner(System.in);

    private BasicDataSource dataSource;

    public Connection(){
        dataSource = new BasicDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl("jdbc:mysql://44.218.109.94:3306/infomotion");
          dataSource.setUsername("root");
<<<<<<< HEAD
          dataSource.setPassword("1234");
=======
          dataSource.setPassword("041316miralha");
>>>>>>> 7aafb1ee39c8135924478e952adaf071b28f2608
    }

    public BasicDataSource getDataSource() {
        return dataSource;
    }
}

