package com.example.oliveyoung.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

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

    // Access Token 생성
    public String generateAccessToken(UserDetails userDetails) {
        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_VALIDITY)) // 15분
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .claim("roles", userDetails.getAuthorities()) // 권한 정보 추가
                .compact();
    }

    // Refresh Token 생성 및 Redis에 저장
    public String generateRefreshToken(UserDetails userDetails) {
        String refreshToken = Jwts.builder()
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_VALIDITY)) // 7일
                .claim("tokenType", "refresh")
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();

        redisTemplate.opsForValue().set(refreshToken, userDetails.getUsername(), REFRESH_TOKEN_VALIDITY, TimeUnit.MILLISECONDS);
        return refreshToken;
    }

    // Access Token 블랙리스트 추가
    public void invalidateAccessToken(String token) {
        long remainingValidity = getRemainingValidity(token);
        if (remainingValidity > 0) {
            redisTemplate.opsForValue().set(BLACKLIST_PREFIX + token, true, remainingValidity, TimeUnit.MILLISECONDS);
        }
    }

    // Access Token 블랙리스트 확인
    public boolean isAccessTokenBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + token));
    }

    // JWT에서 사용자명 추출
    public String getUsernameFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(publicKey)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    // Access Token 검증
    public Boolean validateToken(String token, UserDetails userDetails) {
        if (isAccessTokenBlacklisted(token)) {
            return false;
        }
        String username = getUsernameFromToken(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    // JWT 만료 여부 확인
    private Boolean isTokenExpired(String token) {
        Date expiration = Jwts.parserBuilder()
                .setSigningKey(publicKey)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getExpiration();
        return expiration.before(new Date());
    }

    // 토큰의 남은 유효 시간 계산
    private long getRemainingValidity(String token) {
        Date expiration = Jwts.parserBuilder()
                .setSigningKey(publicKey)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getExpiration();
        return expiration.getTime() - System.currentTimeMillis();
    }

    // Refresh Token 무효화
    public void invalidateRefreshToken(String refreshToken) {
        redisTemplate.delete(refreshToken);
    }

    // Refresh Token 검증
    public Boolean validateRefreshToken(String refreshToken) {
        return redisTemplate.hasKey(refreshToken);
    }

    // Getter for ACCESS_TOKEN_VALIDITY
    public long getAccessTokenValidity() {
        return ACCESS_TOKEN_VALIDITY;
    }
}