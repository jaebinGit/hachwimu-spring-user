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
    public ResponseEntity<?> login(@RequestBody User user) {
        try {
            JwtResponse jwtResponse = userService.login(user);

            // 클라이언트가 access token을 직접 처리하도록 변경
            return ResponseEntity.ok(jwtResponse);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


    @PostMapping("/logout")
    public ResponseEntity<?> logout(@CookieValue("refreshToken") String refreshToken, String accessToken, HttpServletResponse response) {
        userService.logout(refreshToken, accessToken);

        return ResponseEntity.ok("Logged out successfully");
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@CookieValue("refreshToken") String refreshToken) {
        try {
            // Refresh Token을 이용하여 Access Token 갱신
            JwtResponse jwtResponse = userService.refreshAccessToken(refreshToken);

            // 새롭게 발급된 Access Token을 클라이언트로 반환
            return ResponseEntity.ok(jwtResponse);
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