package com.nutech.ppob.controller;

import com.nutech.ppob.dto.ApiResponse;
import com.nutech.ppob.dto.UserDtos.*;
import com.nutech.ppob.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class AuthController {

    private final UserService users;

    public AuthController(UserService users) {
        this.users = users;
    }

    @PostMapping("/registration")
    public ResponseEntity<ApiResponse<Map<String,Object>>> register(@Valid @RequestBody RegistrationRequest req){
        users.register(req);
        return ResponseEntity.ok(ApiResponse.ok("Register success", Map.of(
                "email", req.email(),
                "first_name", req.firstName(),
                "last_name", req.lastName()
        )));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest req){
        var resp = users.login(req);
        return ResponseEntity.ok(ApiResponse.ok("Login success", resp));
    }
}