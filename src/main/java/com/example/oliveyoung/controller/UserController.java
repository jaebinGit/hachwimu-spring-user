package com.example.oliveyoung.controller;

import com.example.oliveyoung.dto.JwtResponse;
import com.example.oliveyoung.dto.UserRegistrationRequest;
import com.example.oliveyoung.dto.UserResponse;
import com.example.oliveyoung.model.User;
import com.example.oliveyoung.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserRegistrationRequest registrationRequest) {
        try {
            userService.registerUser(registrationRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body("User registered successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User user, HttpServletResponse response) {
        try {
            JwtResponse jwtResponse = userService.login(user, response);

            // Access token을 쿠키에 저장
            Cookie accessTokenCookie = new Cookie("accessToken", jwtResponse.getAccessToken());
            accessTokenCookie.setHttpOnly(true);  // JavaScript에서 접근 불가
            accessTokenCookie.setPath("/");
            accessTokenCookie.setMaxAge(15 * 60);  // 15분
            response.addCookie(accessTokenCookie);

            // Refresh token을 쿠키에 저장
            Cookie refreshTokenCookie = new Cookie("refreshToken", jwtResponse.getRefreshToken());
            refreshTokenCookie.setHttpOnly(true);
            refreshTokenCookie.setPath("/");
            refreshTokenCookie.setMaxAge(7 * 24 * 60 * 60);  // 7일
            response.addCookie(refreshTokenCookie);

            return ResponseEntity.ok("Logged in successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@CookieValue("refreshToken") String refreshToken, String accessToken, HttpServletResponse response) {
        userService.logout(refreshToken, accessToken);

        // 쿠키에서 토큰 제거
        Cookie accessTokenCookie = new Cookie("accessToken", null);
        accessTokenCookie.setMaxAge(0);  // 쿠키 삭제
        accessTokenCookie.setPath("/");
        response.addCookie(accessTokenCookie);

        Cookie refreshTokenCookie = new Cookie("refreshToken", null);
        refreshTokenCookie.setMaxAge(0);
        refreshTokenCookie.setPath("/");
        response.addCookie(refreshTokenCookie);

        return ResponseEntity.ok("Logged out successfully");
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@CookieValue("refreshToken") String refreshToken, HttpServletResponse response) {
        try {
            JwtResponse jwtResponse = userService.refreshAccessToken(refreshToken);

            // 새롭게 발급된 Access Token을 쿠키에 저장
            Cookie accessTokenCookie = new Cookie("accessToken", jwtResponse.getAccessToken());
            accessTokenCookie.setHttpOnly(true);  // JavaScript에서 접근 불가
            accessTokenCookie.setPath("/");
            accessTokenCookie.setMaxAge(15 * 60);  // 15분
            response.addCookie(accessTokenCookie);

            return ResponseEntity.ok("Token refreshed successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication != null) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            return ResponseEntity.ok(new UserResponse(userDetails.getUsername()));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
    }
}