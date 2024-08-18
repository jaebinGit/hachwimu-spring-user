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
        // 블랙리스트에 있는지 확인
        if (Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + refreshToken))) {
            return false; // 블랙리스트에 있으면 유효하지 않음
        }
        return redisTemplate.hasKey(refreshToken); // 블랙리스트에 없으면 기존 방식으로 확인
    }

    // 토큰 검증
    public Boolean validateToken(String token, UserDetails userDetails) {
        String username = Jwts.parserBuilder()
                .setSigningKey(publicKey)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
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

    // JWT 만료 여부 확인
    private Boolean isTokenExpired(String token) {
        Date expiration = Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getExpiration();
        return expiration.before(new Date());
    }

    // Refresh Token 무효화 (로그아웃 시 블랙리스트에 추가)
    public void invalidateRefreshToken(String refreshToken) {
        // 블랙리스트에 추가
        redisTemplate.opsForValue().set(BLACKLIST_PREFIX + refreshToken, true, REFRESH_TOKEN_VALIDITY, TimeUnit.MILLISECONDS);
    }
}