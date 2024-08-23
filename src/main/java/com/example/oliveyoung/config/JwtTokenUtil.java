package com.example.oliveyoung.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Component
public class JwtTokenUtil {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final long ACCESS_TOKEN_VALIDITY = 1000 * 60 * 15; // 15분
    private static final long REFRESH_TOKEN_VALIDITY = 1000 * 60 * 60 * 24 * 7; // 7일
    private static final String BLACKLIST_PREFIX = "BLACKLIST_";

    private final SecretKey secretKey;

    // SecretKey 자동 생성
    public JwtTokenUtil() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("HmacSHA512");
        keyGenerator.init(512);  // HS512는 512비트 키를 사용
        this.secretKey = keyGenerator.generateKey();
    }

    // Access Token 생성
    public String generateAccessToken(UserDetails userDetails) {
        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_VALIDITY)) // 15분
                .signWith(secretKey, SignatureAlgorithm.HS512)
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
                .signWith(secretKey, SignatureAlgorithm.HS512)
                .compact();

        redisTemplate.opsForValue().set(refreshToken, userDetails.getUsername(), REFRESH_TOKEN_VALIDITY, TimeUnit.MILLISECONDS);
        return refreshToken;
    }

    // JWT에서 사용자명 추출
    public String getUsernameFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    // Access Token 검증
    public Boolean validateToken(String token, UserDetails userDetails) {
        String username = getUsernameFromToken(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    // JWT 만료 여부 확인
    private Boolean isTokenExpired(String token) {
        Date expiration = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getExpiration();
        return expiration.before(new Date());
    }

    // Refresh Token 무효화
    public void invalidateRefreshToken(String refreshToken) {
        redisTemplate.delete(refreshToken);
    }

    // Refresh Token 검증
    public Boolean validateRefreshToken(String refreshToken) {
        return redisTemplate.hasKey(refreshToken);
    }

    // Redis에서 사용자 이름으로 refreshToken 찾기
    public String getRefreshTokenFromRedis(String username) {
        return (String) redisTemplate.opsForValue().get(username);
    }
}