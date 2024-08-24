package com.example.oliveyoung.service;

import com.example.oliveyoung.dto.UserRegistrationRequest;
import com.example.oliveyoung.model.User;
import com.example.oliveyoung.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // 로그인 처리
    public boolean login(User user) {
        // 데이터베이스에서 사용자를 조회
        User existingUser = userRepository.findByUsername(user.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 비밀번호 확인
        return passwordEncoder.matches(user.getPassword(), existingUser.getPassword());
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
        user.setPassword(passwordEncoder.encode(registrationRequest.getPassword()));
        user.setEmail(registrationRequest.getEmail());
        user.setAddress(registrationRequest.getAddress());

        // 사용자 저장
        userRepository.save(user);
    }
}