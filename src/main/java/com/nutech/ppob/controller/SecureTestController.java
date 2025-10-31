package com.nutech.ppob.controller;

import com.nutech.ppob.dto.ApiResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/secure")
public class SecureTestController {

    @GetMapping("/ping")
    public ApiResponse<Map<String, Object>> securePing(Authentication auth) {
        String subject = auth != null ? String.valueOf(auth.getPrincipal()) : "anonymous";
        return ApiResponse.ok("secure pong", Map.of(
                "subject", subject,
                "status", "OK"
        ));
    }
}