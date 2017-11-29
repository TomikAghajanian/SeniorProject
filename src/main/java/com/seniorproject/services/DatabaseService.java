package com.seniorproject.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.sql.*;

@Service
public class DatabaseService implements IDatabase {

    private Connection connection = null;
    private String url = "jdbc:postgresql://localhost:5432/SeniorProject";
    private String username = "postgres";
    private String password = "tomik";


    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public Connection getConnection() {
        try {
            Class.forName("org.postgresql.Driver");
            this.connection = DriverManager.getConnection(url, username, password);
            return this.connection;
        } catch (Exception ex) {
            logger.debug("Database Connection Creation Failed : " + ex.getMessage());
            return null;
        }
    }

    @Cacheable(value = "dbApiKey")
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
                    logger.debug("here's your api key= " + key);
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
        String modifiedClientSession = "'" + clientSession + "'";
        if (connection != null) {
            try {
                Statement stmt = connection.createStatement();
                ResultSet rs;

                rs = stmt.executeQuery("INSERT INTO public.\"ClientSession\"(\"clientID\", \"request\") VALUES (" + id + "," + modifiedClientSession + ");");
                logger.debug("success");
                connection.close();
                return true;
            } catch (Exception e) {
                logger.error(e.getMessage());
                return false;
            }
        } else {
            return false;
        }
    }

    public boolean storeTokens(String userID, String accessToken, String refreshToken, String accessTokenCreationTime, String refreshTokenCreationTime) {
        Connection connection = getConnection();
        if (connection != null) {
            try {
                Statement stmt = connection.createStatement();
                stmt.executeUpdate(
                        "INSERT INTO public.\"Oauth\"(\"userID\", access_token, refresh_token, accesstoken_creationtime, refreshtoken_creationtime) VALUES ('" + userID + "', '" + accessToken + "', '" + refreshToken + "', '" + accessTokenCreationTime + "', '"+ refreshTokenCreationTime+"');");
                connection.close();
                return true;
            } catch (Exception e) {
                logger.error(e.getMessage());
                return false;
            }
        } else {
            return false;
        }
    }

    public boolean validateUser(String[] userInfo) {
        Connection connection = getConnection();
        if (connection != null) {
            try {
                Statement stmt = connection.createStatement();
                ResultSet rs;

                rs = stmt.executeQuery(
                        "SELECT username FROM public.\"ClientLogin\" WHERE username = '" + userInfo[0] + "' AND password='" + userInfo[1] + "'");

                boolean result = rs.next();
                logger.debug("username found = " + result);
                connection.close();
                return result;
            } catch (Exception e) {
                logger.error(e.getMessage());
                return false;
            }
        } else {
            return false;
        }
    }

    public boolean validateAccessToken(String userID, String accessToken, long accessTokenExp) {
        Connection connection = getConnection();
        if (connection != null) {
            try {
                Statement stmt = connection.createStatement();
                ResultSet rs;

                rs = stmt.executeQuery(
                        "SELECT accesstoken_creationtime FROM public.\"Oauth\" WHERE \"userID\" = '" + userID + "' AND access_token='" + accessToken + "'");

                logger.debug("rs = {}", rs);
                String accesstoken_creationtime = null;
                if (rs.next()) {
                    accesstoken_creationtime = rs.getString(1);
                    logger.debug("access token creationtime time is {} ", Long.valueOf(accesstoken_creationtime));
                }
                connection.close();

                logger.debug("current time in mili: {}", System.currentTimeMillis());
                long cuurentTimeSubtractedAccessTokenExp = System.currentTimeMillis() - accessTokenExp;
                logger.debug("cuurentTimeSubtractedAccessTokenExp: {}", cuurentTimeSubtractedAccessTokenExp);

                if (Long.valueOf(accesstoken_creationtime) > cuurentTimeSubtractedAccessTokenExp) {
                    logger.info("access token is valid and not expired yet");
                    return true;
                }
                logger.info("access token is not valid");
                return false;
            } catch (Exception e) {
                logger.error(e.getMessage());
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean validateRefreshToken(String userID, String refreshToken, long refreshTokenExp) {
        Connection connection = getConnection();
        if (connection != null) {
            try {
                Statement stmt = connection.createStatement();
                ResultSet rs;

                rs = stmt.executeQuery(
                        "SELECT refreshtoken_creationtime FROM public.\"Oauth\" WHERE \"userID\" = '" + userID + "' AND refresh_token='" + refreshToken + "'");

                logger.debug("rs = {}", rs);
                String refreshtoken_creationtime = null;
                if (rs.next()) {
                    refreshtoken_creationtime = rs.getString(1);
                    logger.debug("access token creationtime time is {} ", Long.valueOf(refreshtoken_creationtime));
                }
                connection.close();

                logger.debug("current time in mili: {}", System.currentTimeMillis());
                long cuurentTimeSubtractedAccessTokenExp = System.currentTimeMillis() - refreshTokenExp;
                logger.debug("cuurentTimeSubtractedAccessTokenExp: {}", cuurentTimeSubtractedAccessTokenExp);

                if (Long.valueOf(refreshtoken_creationtime) > cuurentTimeSubtractedAccessTokenExp) {
                    logger.info("refresh token is valid and not expired yet");
                    return true;
                }
                logger.info("refresh token is not valid");
                return false;
            } catch (Exception e) {
                logger.error(e.getMessage());
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean storeNewAccessToken(String userID, String newAccessToken, String accessTokenCreationTime) {
        Connection connection = getConnection();
        if (connection != null) {
            try {
                Statement stmt = connection.createStatement();

                stmt.executeUpdate(
                        "UPDATE public.\"Oauth\" SET access_token='" + newAccessToken + "', accesstoken_creationtime= '" + accessTokenCreationTime + "' WHERE \"userID\"='" + userID + "'");


                connection.close();
                logger.info("successfully updated the new access token");
                return true;
            } catch (Exception e) {
                logger.error(e.getMessage());
                return false;
            }
        } else {
            return false;
        }
    }
}
