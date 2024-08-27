package com.example.oliveyoung.service;

import com.example.oliveyoung.dto.UserRegistrationRequest;
import com.example.oliveyoung.model.User;
import com.example.oliveyoung.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean login(User user) {
        User existingUser = userRepository.findByUsername(user.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return user.getPassword().equals(existingUser.getPassword());
    }

    public void registerUser(UserRegistrationRequest registrationRequest) throws Exception {

        if (userRepository.existsByUsername(registrationRequest.getUsername())) {
            throw new Exception("이미 존재하는 사용자명입니다.");
        }

        User user = new User();
        user.setUsername(registrationRequest.getUsername());
        user.setPassword(registrationRequest.getPassword());
        user.setEmail(registrationRequest.getEmail());
        user.setAddress(registrationRequest.getAddress());

        // 사용자 저장
        userRepository.save(user);
    }
}