package ru.func.hungergames;

import com.sun.istack.internal.NotNull;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MySql extends Database {
    //Класс хранящий инструменты для работы с MySql
    private final String user;
    private final String password;
    private final String host;
    private final String database;
    private final int port;

    MySql (@NotNull String user, @NotNull String password, @NotNull String host, @NotNull String database, int port)
    {
        this.user = user;
        this.password = password;
        this.host = host;
        this.database = database;
        this.port = port;
    }

    @Override
    public Connection openConnection() throws SQLException, ClassNotFoundException {
        if (checkConnection()) {
            return connection;
        }
        Class.forName("com.mysql.jdbc.Driver");
        connection = DriverManager.getConnection("jdbc:mysql://" + this.host + ":" + this.port + "/" + this.database + "?characterEncoding=UTF-8&autoReconnect=true", this.user, this.password);
        return connection;
    }
}
