package com.lou.freegpt.utils;


import cn.hutool.core.date.DateTime;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@Slf4j
public class JwtUtil {
    private static final String SECRET_KEY = "mhchat-2000314";
    private static final long EXPIRE_TOKEN = 1000 * 60 * 60 * 24 * 7;
    private static final String USER_NAME = "username";
    private static final String ISSUER = "admin";

    public static String token(String username) {
        Date date = new Date();
        Algorithm algorithm = Algorithm.HMAC256(SECRET_KEY);
        String token = JWT.create()
                .withIssuer(ISSUER)
                .withIssuedAt(date)
                .withExpiresAt(new Date(date.getTime() + EXPIRE_TOKEN))
                .withClaim(USER_NAME, username)
                .sign(algorithm);
        log.info("jwt generated user={}", username);
        return token;
    }

    public static Date getExpirationDate(String token) {
        DecodedJWT decodedJWT = JWT.decode(token);
        return decodedJWT.getExpiresAt();
    }

    public static boolean verify(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(SECRET_KEY);
            JWTVerifier jwtVerifier = JWT.require(algorithm)
                    .withIssuer(ISSUER)
                    .build();
            jwtVerifier.verify(token);
            return true;
        } catch (Exception e) {
            log.error("jwt校验失败-{}", e);
            return false;
        }
    }


    public static String getUsernameFromToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(SECRET_KEY);
            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer(ISSUER)
                    .build();
            DecodedJWT jwt = verifier.verify(token);
            String username = jwt.getClaim(USER_NAME).asString();
            return username;
        } catch (JWTVerificationException exception){
            log.error("Token verification failed.", exception);
            return null;
        }
    }

//    public static void main(String[] args) {
//        String token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJhZG1pbiIsImV4cCI6MTcxNzMwODE3OSwiaWF0IjoxNzE3MjAwMTc5LCJ1c2VybmFtZSI6ImZpcmUifQ.Ks6K0kTUkCedxbeIHnIqpTt14C7_-hkaTHZzgwTI6M8";
//        //String token = token("muhuo");
//        Date expirationDate = getExpirationDate(token);
//        long time = System.currentTimeMillis() - expirationDate.getTime();
//        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//        String format = simpleDateFormat.format(expirationDate);
//        System.out.println(time);
//    }

    public static void main(String[] args) {
        String token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJhZG1pbiIsImV4cCI6MTcxNzgyMjQ3NiwiaWF0IjoxNzE3MjE3Njc2LCJ1c2VybmFtZSI6ImZpcmUifQ.eFfEOgGYtPJwLWVJ0rImVjyv-bFGeMl9bTofjptRqjk";

        System.out.println(getUsernameFromToken(token));
    }

}
