package com.example.oliveyoung.service;

import com.example.oliveyoung.dto.JwtResponse;
import com.example.oliveyoung.dto.UserRegistrationRequest;
import com.example.oliveyoung.model.User;
import com.example.oliveyoung.repository.UserRepository;
import com.example.oliveyoung.config.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
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

    @Autowired
    private PasswordEncoder passwordEncoder;

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

    // 회원가입 처리 메서드
    public void registerUser(UserRegistrationRequest registrationRequest) throws Exception {
        // 중복된 사용자명 확인
        if (userRepository.existsByUsername(registrationRequest.getUsername())) {
            throw new Exception("이미 존재하는 사용자명입니다.");
        }

        // 사용자 엔티티 생성
        User user = new User();
        user.setUsername(registrationRequest.getUsername());
        user.setPassword(passwordEncoder.encode(registrationRequest.getPassword())); // 비밀번호 암호화
        user.setEmail(registrationRequest.getEmail());
        user.setAddress(registrationRequest.getAddress());

        // 사용자 저장
        userRepository.save(user);
    }
}