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
  public ResponseEntity<ApiResponse<Map<String, Object>>> register(@Valid @RequestBody RegistrationRequest req) {
    users.register(req);
    return ResponseEntity.ok(ApiResponse.ok("Registrasi berhasil silahkan login", null));
  }

  @PostMapping("/login")
  public ResponseEntity<ApiResponse<Map<String, String>>> login(@Valid @RequestBody LoginRequest req) {
    var token = users.login(req).token();
    return ResponseEntity.ok(ApiResponse.ok("Login Sukses", Map.of("token", token)));
  }
}