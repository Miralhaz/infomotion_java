package org.example;

import org.apache.commons.dbcp2.BasicDataSource;

public class Connection {

    private BasicDataSource dataSource;

    public Connection(){
        dataSource = new BasicDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:mysql://localhost:3360//infomotion");
        dataSource.setUsername("");
        dataSource.setPassword("");
    }

    public BasicDataSource getDataSource() {
        return dataSource;
    }
}

