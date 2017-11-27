package com.seniorproject.services;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.sql.*;

@Service
public class Database implements IDatabase {
    public Connection connectionToDb() {
        Connection connection;
        System.out.println("getConnection() called");
        long starttime = 0;
        long endtime = 0;
        try {
            starttime = System.currentTimeMillis();
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println("Where is your PostgreSQL JDBC Driver? "
                    + "Include in your library path!");
            e.printStackTrace();
        }
        try {
            connection = DriverManager.getConnection(
                    "jdbc:postgresql://localhost:5432/SeniorProject", "postgres",
                    "tomik");
            endtime = System.currentTimeMillis();
            System.out.println("execution duration: " + (endtime - starttime));
            return connection;
        } catch (SQLException e) {
            System.out.println("Connection Failed! Check output console");
            e.printStackTrace();
            return null;
        }
    }

    @Cacheable(value = "dbConnection")
    public Connection getConnection() {
        System.out.println("inside getConnection");
        return connectionToDb();
    }

    @Cacheable(value = "dbConnection")
    public String getApiKey() {
        Connection connection = getConnection();
        if (connection != null) {
            try {
                Statement stmt = connection.createStatement();
                ResultSet rs;

                rs = stmt.executeQuery("SELECT api_key FROM public.\"ApiKey\"");
                String key = null;
                while (rs.next()) {
                    key = rs.getString("api_key");
                    System.out.println("here's your api key= " + key);
                }
                connection.close();
                return key;
            } catch (Exception e) {
                return e.getMessage();
            }
        } else {
            return "Failed to make connection!";
        }
    }

    public boolean inputClientSession(int id, String clientSession) {
        Connection connection = getConnection();
        String modifiedClientSession = "'"+clientSession+"'";
        if (connection != null) {
            try {
                Statement stmt = connection.createStatement();
                ResultSet rs;

                rs = stmt.executeQuery("INSERT INTO public.\"ClientSession\"(\"clientID\", \"request\") VALUES (" + id + "," +modifiedClientSession + ");");
                System.out.println("success");
                connection.close();
                return true;
            } catch (Exception e) {
                System.out.println(e);
                return false;
            }
        } else {
            return false;
        }
    }
}
