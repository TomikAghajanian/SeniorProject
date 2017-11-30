package com.seniorproject.services;

import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.Arrays;
import java.util.Base64;

@Service
public class DatabaseService implements IDatabase {

    private Connection connection = null;
    private String url = "jdbc:postgresql://localhost:5432/SeniorProject";
    private String username = "postgres";
    private String password = "";
    private static SecretKeySpec secretKey;
    private static byte[] key;

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
                }
                String decryptedKey = decryptApiKey(key, connection);
                connection.close();

                if (decryptedKey != null) {
                    return decryptedKey;
                }
                return null;
            } catch (Exception e) {
                return e.getMessage();
            }
        } else {
            return "Failed to make connection!";
        }
    }

    private String decryptApiKey(String apikeyEncrypted, Connection connection) {
        if (connection != null) {
            try {
                Statement stmt = connection.createStatement();
                ResultSet rs;

                rs = stmt.executeQuery("SELECT decryptkey FROM public.\"DecryptionKey\"");
                String decryptKey = null;
                while (rs.next()) {
                    decryptKey = rs.getString("decryptkey");
                }


                try {
                    setKey(decryptKey);
                    Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
                    cipher.init(Cipher.DECRYPT_MODE, secretKey);
                    connection.close();
                    return new String(cipher.doFinal(Base64.getDecoder().decode(apikeyEncrypted)));
                } catch (Exception e) {
                    logger.error("Error while decrypting: " + e.toString());
                    return null;
                }
            } catch (Exception e) {
                logger.debug(e.getMessage());
                return null;
            }
        } else {
            return null;
        }
    }

    public static void setKey(String myKey) {
        MessageDigest sha = null;
        try {
            key = myKey.getBytes("UTF-8");
            sha = MessageDigest.getInstance("SHA-1");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 16);
            secretKey = new SecretKeySpec(key, "AES");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public boolean storeClientContext(String userID, String clientContext, long creationTime) {
        Connection connection = getConnection();
        if (connection != null) {
            try {
                Statement stmt = connection.createStatement();

                //check if client exists first
                ResultSet rs = stmt.executeQuery("SELECT \"clientID\" FROM public.\"ClientContext\" WHERE \"clientID\"='" + userID +"';");

                boolean clientExists = rs.next();
                logger.debug("client already exists: {}", clientExists);

                if(!clientExists) {

                    logger.info("Client does NOT exist in db. Adding userID and context...");
                    stmt.executeUpdate("INSERT INTO public.\"ClientContext\"(request, \"clientID\" , creationtime) VALUES ('" + clientContext + "','" + userID + "','" + String.valueOf(creationTime) + "');");
                    connection.close();
                    return true;
                }
                else {
                    logger.info("Client does exist in db. Updating context...");
                    stmt.executeUpdate("UPDATE public.\"ClientContext\" SET request='" + clientContext + "', creationtime='" + creationTime + "' WHERE \"clientID\"='" + userID + "';");
                    connection.close();
                    return true;
                }
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
                        "INSERT INTO public.\"Oauth\"(\"userID\", access_token, refresh_token, accesstoken_creationtime, refreshtoken_creationtime) VALUES ('" + userID + "', '" + accessToken + "', '" + refreshToken + "', '" + accessTokenCreationTime + "', '" + refreshTokenCreationTime + "');");
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
                        "SELECT password FROM public.\"ClientLogin\" WHERE username = '" + userInfo[0] + "'");

                String hashedPassword = null;
                while (rs.next()) {
                    hashedPassword = rs.getString("password");
                }

                connection.close();
                logger.debug("hashed password is {} ", hashedPassword);

                if (checkPassword(userInfo[1], hashedPassword)) {
                    return true;
                }
                return false;
            } catch (Exception e) {
                logger.error(e.getMessage());
                return false;
            }
        } else {
            return false;
        }
    }

    private boolean checkPassword(String password_plaintext, String stored_hash) {
        boolean password_verified = false;

        if (null == stored_hash || !stored_hash.startsWith("$2a$"))
            return false;

        password_verified = BCrypt.checkpw(password_plaintext, stored_hash);

        return (password_verified);
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

                long cuurentTimeSubtractedAccessTokenExp = System.currentTimeMillis() - accessTokenExp;
                if (Long.valueOf(accesstoken_creationtime) > cuurentTimeSubtractedAccessTokenExp) {
                    return true;
                }
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
                }
                connection.close();

                long cuurentTimeSubtractedAccessTokenExp = System.currentTimeMillis() - refreshTokenExp;

                if (Long.valueOf(refreshtoken_creationtime) > cuurentTimeSubtractedAccessTokenExp) {
                    return true;
                }
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
