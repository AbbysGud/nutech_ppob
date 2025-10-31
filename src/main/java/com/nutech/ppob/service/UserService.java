package com.nutech.ppob.service;

import com.nutech.ppob.dto.UserDtos.*;
import com.nutech.ppob.exception.BusinessException;
import com.nutech.ppob.repo.UserRepo;
import com.nutech.ppob.util.JwtUtil;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class UserService {

    private final UserRepo userRepo;
    private final PasswordEncoder encoder;
    private final JwtUtil jwt;

    public UserService(UserRepo userRepo, PasswordEncoder encoder, JwtUtil jwt) {
        this.userRepo = userRepo;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    @Transactional
    public void register(RegistrationRequest req) {
        if (userRepo.emailExists(req.email())) {
            throw new BusinessException("Email already registered");
        }
        String hash = encoder.encode(req.password());
        try {
            userRepo.insertUser(req.email(), hash, req.firstName(), req.lastName());
            userRepo.insertWalletZero(req.email());
        } catch (DuplicateKeyException e) {
            throw new BusinessException("Email already registered");
        }
    }

    public LoginResponse login(LoginRequest req) {
        var opt = userRepo.getPasswordHashByEmail(req.email());
        if (opt.isEmpty()) throw new BusinessException("Invalid credentials");
        String hash = opt.get();
        if (!encoder.matches(req.password(), hash)) {
            throw new BusinessException("Invalid credentials");
        }
        userRepo.updateLastLogin(req.email());
        String token = jwt.generate(req.email(), Map.of("email", req.email()));
        return new LoginResponse(token);
    }

    public ProfileResponse getProfile(String email) {
        return userRepo.getProfileByEmail(email);
    }

    public void updateProfile(String email, ProfileUpdateRequest req) {
        int n = userRepo.updateProfile(email, req.firstName(), req.lastName());
        if (n == 0) throw new BusinessException("Failed to update profile");
    }

    public String updateProfileImage(String email, String imageUrl) {
        int n = userRepo.updateProfileImage(email, imageUrl);
        if (n == 0) throw new BusinessException("Failed to update profile image");
        return imageUrl;
    }
}
