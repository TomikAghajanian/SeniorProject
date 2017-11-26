package com.seniorproject.security;

import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Base64;

@Component
public class FilterRequest implements Filter {
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        response.setContentType("application/json");
        JSONObject jsonResponse = new JSONObject();

        System.out.println("inside filter");
        String uri = request.getRequestURI().toString();

        System.out.println("URI = " + uri);

        if (uri.equals("/login")) {
            final String authorization = request.getHeader("Authorization");
            if (authorization != null && authorization.startsWith("Basic")) {
                String base64Credentials = authorization.substring("Basic".length()).trim();
                String credentials = new String(Base64.getDecoder().decode(base64Credentials), Charset.forName("UTF-8"));
                final String[] userAndPass = credentials.split(":", 2);

                
            } else {
                response.sendError(HttpStatus.BAD_REQUEST.value(), "no basic authorization provided");
            }
        } else if (uri.equals("/coordinates")) {
            filterChain.doFilter(servletRequest, servletResponse);
        } else if (uri.equals("/distanceandduration")) {
            filterChain.doFilter(servletRequest, servletResponse);
        } else {

        }
    }

    @Override
    public void destroy() {

    }
}
