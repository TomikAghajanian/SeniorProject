package com.seniorproject.security;

import com.seniorproject.services.IDatabase;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
public class FilterRequest implements Filter {
    private final static long ACCESS_TOKEN_EXPTIME = 300000;
    private final static long REFRESH_TOKEN_EXPTIME = 600000;

    @Autowired
    private IDatabase database;


    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        response.setContentType("application/json");

        logger.debug("Filtering Request...");
        String uri = request.getRequestURI().toString();

        logger.debug("Client trying to access {} endpoint", uri);
        if (uri.equals("/login")) {
            final String authorization = request.getHeader("Authorization");
            if (authorization != null && authorization.startsWith("Basic")) {
                String base64Credentials = authorization.substring("Basic".length()).trim();
                String credentials = new String(Base64.getDecoder().decode(base64Credentials), Charset.forName("UTF-8"));
                logger.debug("client provided basic auth");
                final String[] userAndPass = credentials.split(":", 2);

                boolean isValid = database.validateUser(userAndPass);
                if (isValid) {
                    logger.debug("User is Authorized.");
                    JSONObject jsonResponse = null;
                    try {
                        jsonResponse = new JSONObject(providedTokens());
                    } catch (Exception e) {
                        logger.error("unexpected internal error");
                        response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "unexpected internal error");
                    }
                    PrintWriter out = response.getWriter();
                    out.print(jsonResponse);
                    out.flush();

                } else {
                    logger.error("Unauthorized user!");
                    response.sendError(HttpStatus.BAD_REQUEST.value(), "user not valid");
                }
            } else {
                logger.error("no basic authorization provided");
                response.sendError(HttpStatus.BAD_REQUEST.value(), "no basic authorization provided");
            }
        } else if (uri.equals("/coordinates") || uri.equals("/distanceandduration") || uri.equals("/latlong")) {
            if (request.getHeader("AccessToken") != null && request.getHeader("UserID") != null) {

                //grab userId and accesstoken from header
                String accessToken = request.getHeader("AccessToken");
                String userID = request.getHeader("UserID");

                boolean isValid = database.validateAccessToken(userID, accessToken, ACCESS_TOKEN_EXPTIME);
                if (isValid) {
                    logger.info("access token is VALID");
                    filterChain.doFilter(servletRequest, servletResponse);
                } else {
                    logger.error("access token is EXPIRED");
                    response.sendError(HttpStatus.BAD_REQUEST.value(), "access token is expired");
                }
            } else {
                logger.error("please provide userID and access token header");
                response.sendError(HttpStatus.BAD_REQUEST.value(), "no userID and access token provided in the header");
            }
        } else if (uri.equals("/refresh")) {
            if (request.getHeader("RefreshToken") != null && request.getHeader("UserID") != null) {
                //grab userId and accesstoken from headeer
                String refreshToken = request.getHeader("RefreshToken");
                String userID = request.getHeader("UserID");

                boolean isValid = database.validateRefreshToken(userID, refreshToken, REFRESH_TOKEN_EXPTIME);
                if (isValid) {
                    logger.info("refresh token is VALID");
                    String newAccessToken = providedNewAccessToken(userID);
                    if (newAccessToken != null) {
                        JSONObject jsonResponse = new JSONObject();
                        jsonResponse.put("accesstoken", newAccessToken);
                        jsonResponse.put("userID", userID);

                        PrintWriter out = response.getWriter();
                        out.print(jsonResponse);
                        out.flush();
                    } else {
                        logger.error("unexpected internal error");
                        response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "unexpected internal error");
                    }
                } else {
                    logger.error("refresh token is EXPIRED");
                    response.sendError(HttpStatus.BAD_REQUEST.value(), "refresh token is expired");
                }
            }else {
                logger.error("please provide refresh token and userID in header");
                response.sendError(HttpStatus.BAD_REQUEST.value(), "no userID and refresh token provided in the header");
            }
        }else{
            logger.error("INVALID ENDPOINT");
            response.sendError(HttpStatus.BAD_REQUEST.value(), "INVALID ENDPOINT");
        }

    }

    public String providedTokens() {
        String accessToken = UUID.randomUUID().toString();
        String refreshToken = UUID.randomUUID().toString();

        Random r = new Random();
        int Low = 1;
        int High = 10000;
        int tempUserID = r.nextInt(High - Low) + Low;

        String userID = String.valueOf(tempUserID);
        String accessTokenAndRefreshTokenCreationTime = String.valueOf(System.currentTimeMillis());
        boolean isStored = database.storeTokens(userID, accessToken, refreshToken, accessTokenAndRefreshTokenCreationTime, accessTokenAndRefreshTokenCreationTime);

        if (isStored) {
            JSONObject returningJSON = new JSONObject();

            returningJSON.put("accesstoken", accessToken);
            returningJSON.put("refreshtoken", refreshToken);
            returningJSON.put("userID", userID);
            returningJSON.put("refresh token expires in:", TimeUnit.MILLISECONDS.toMinutes(REFRESH_TOKEN_EXPTIME) + " minutes");
            returningJSON.put("access token expires in:", TimeUnit.MILLISECONDS.toMinutes(ACCESS_TOKEN_EXPTIME) + " minutes");

            logger.debug("JOSN Obj from provide token {}", returningJSON.toString());
            return returningJSON.toString();
        }
        return null;
    }

    public String providedNewAccessToken(String userID) {
        String newAccessToken = UUID.randomUUID().toString();

        //store in db
        boolean isStored = database.storeNewAccessToken(userID, newAccessToken, String.valueOf(System.currentTimeMillis()));

        if (isStored) {
            return newAccessToken;
        } else {
            return null;
        }
    }

    @Override
    public void destroy() {

    }
}
