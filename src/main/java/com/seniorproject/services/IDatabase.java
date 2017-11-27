package com.seniorproject.services;

import java.sql.Connection;

public interface IDatabase {
    Connection getConnection();
    String getApiKey();
    boolean inputClientSession(int id, String clientSession);
}
