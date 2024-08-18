package com.example.oliveyoung.service;

import com.example.oliveyoung.dto.JwtResponse;
import com.example.oliveyoung.model.User;
import com.example.oliveyoung.repository.UserRepository;
import com.example.oliveyoung.config.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private JwtUserDetailsService jwtUserDetailsService;

    @Autowired
    private UserRepository userRepository;

    // 로그인 처리
    public JwtResponse login(User user) throws Exception {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword()));
        } catch (Exception e) {
            throw new Exception("INVALID_CREDENTIALS", e);
        }

        final UserDetails userDetails = jwtUserDetailsService.loadUserByUsername(user.getUsername());
        final String accessToken = jwtTokenUtil.generateAccessToken(userDetails);
        final String refreshToken = jwtTokenUtil.generateRefreshToken(userDetails);

        return new JwtResponse(accessToken, refreshToken);
    }

    // 로그아웃 처리
    public void logout(String refreshToken) {
        jwtTokenUtil.invalidateRefreshToken(refreshToken);
    }

    // Refresh Token을 이용한 Access Token 갱신
    public JwtResponse refreshAccessToken(String refreshToken) throws Exception {
        if (jwtTokenUtil.validateRefreshToken(refreshToken)) {
            String username = jwtTokenUtil.getUsernameFromToken(refreshToken);
            UserDetails userDetails = jwtUserDetailsService.loadUserByUsername(username);

            // 새로운 Access Token과 Refresh Token 발급
            final String newAccessToken = jwtTokenUtil.generateAccessToken(userDetails);
            final String newRefreshToken = jwtTokenUtil.generateRefreshToken(userDetails);

            // 기존 Refresh Token 무효화
            jwtTokenUtil.invalidateRefreshToken(refreshToken);

            return new JwtResponse(newAccessToken, newRefreshToken);
        } else {
            throw new Exception("INVALID_REFRESH_TOKEN");
        }
    }
}