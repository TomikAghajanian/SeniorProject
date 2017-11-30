package com.seniorproject.services;

import java.sql.Connection;

public interface IDatabase {
    Connection getConnection();
    String getApiKey();
    boolean storeClientContext(String userID, String clientContext, long creationTime);
    boolean storeTokens(String userID, String accessToken, String refreshToken, String accessTokenCreationTime, String refreshTokenCreationTime);
    boolean validateUser(String[] userInfo);
    boolean validateAccessToken(String userID, String accessToken, long accessTokenExp);
    boolean validateRefreshToken(String userID, String refreshToken, long refreshTokenExp);
    boolean storeNewAccessToken(String userID, String newAccessToken, String accessTokenCreationTime);
}
