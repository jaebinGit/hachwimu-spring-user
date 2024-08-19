package com.example.oliveyoung.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Component
public class JwtTokenUtil {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final long ACCESS_TOKEN_VALIDITY = 1000 * 60 * 15; // 15분
    private static final long REFRESH_TOKEN_VALIDITY = 1000 * 60 * 60 * 24 * 7; // 7일
    private static final SecretKey SECRET_KEY = io.jsonwebtoken.security.Keys.secretKeyFor(SignatureAlgorithm.HS512);
    private static final String BLACKLIST_PREFIX = "BLACKLIST_";

    private final PrivateKey privateKey;
    private final PublicKey publicKey;

    public JwtTokenUtil() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        this.privateKey = keyPair.getPrivate();
        this.publicKey = keyPair.getPublic();
    }


    // Access Token 생성 (RS256 알고리즘 사용)
    public String generateAccessToken(UserDetails userDetails) {
        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_VALIDITY))
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }

    // Refresh Token 생성 및 Redis에 저장
    public String generateRefreshToken(UserDetails userDetails) {
        String refreshToken = Jwts.builder()
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_VALIDITY))
                .signWith(SECRET_KEY)
                .compact();

        // Redis에 Refresh Token 저장
        redisTemplate.opsForValue().set(refreshToken, userDetails.getUsername(), REFRESH_TOKEN_VALIDITY, TimeUnit.MILLISECONDS);
        return refreshToken;
    }

    // Refresh Token 유효성 확인 (블랙리스트 확인)
    public Boolean validateRefreshToken(String refreshToken) {
        return redisTemplate.hasKey(refreshToken);
    }

    // JWT에서 사용자명 추출
    public String getUsernameFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        // JWT에서 사용자명 추출
        String username = getUsernameFromToken(token);

        // 토큰이 만료되지 않았는지 확인하고, 토큰에 있는 사용자명과 UserDetails의 사용자명이 일치하는지 확인
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    // JWT에서 토큰의 만료 여부 확인
    private Boolean isTokenExpired(String token) {
        Date expiration = Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getExpiration();
        return expiration.before(new Date());
    }

    public void invalidateRefreshToken(String refreshToken) {
        // Redis에서 Refresh Token 삭제 (무효화)
        redisTemplate.delete(refreshToken);
    }
}