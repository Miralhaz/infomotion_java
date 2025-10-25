package org.example;

import org.apache.commons.dbcp2.BasicDataSource;

import java.util.Scanner;

public class Connection {
    Scanner scanner = new Scanner(System.in);

//    public String dbUserName(){
//        System.out.println("\nDigite o nome do usuario do banco de dados");
//        return scanner.next();
//    }
//
//    public String dbSenha(){
//        System.out.println("\nDigite a senha do banco de dados");
//        return scanner.next();
//    }



    private BasicDataSource dataSource;

    public Connection(){
        dataSource = new BasicDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl("jdbc:mysql://localhost:3306/infomotion");
//        dataSource.setUsername(dbUserName());
//        dataSource.setPassword(dbSenha());
          dataSource.setUsername("Aluno");
          dataSource.setPassword("1234");
    }

    public BasicDataSource getDataSource() {
        return dataSource;
    }
}

