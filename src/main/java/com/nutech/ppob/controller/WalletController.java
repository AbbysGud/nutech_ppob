package com.nutech.ppob.controller;

import com.nutech.ppob.dto.ApiResponse;
import com.nutech.ppob.dto.WalletDtos.*;
import com.nutech.ppob.exception.BusinessException;
import com.nutech.ppob.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
public class WalletController {
  private final WalletService service;
  public WalletController(WalletService service) { this.service = service; }

  @GetMapping("/balance")
  public ResponseEntity<ApiResponse<BalanceResponse>> getBalance(Authentication auth) {
    String email = (String) auth.getPrincipal();
    var res = service.getBalance(email);
    return ResponseEntity.ok(ApiResponse.ok("Get Balance Berhasil", res));
  }

  @PostMapping("/topup")
  public ResponseEntity<ApiResponse<TopupResponse>> topup(
      Authentication auth,
      @Valid @RequestBody TopupRequest req) {
    String email = (String) auth.getPrincipal();

    if (req.top_up_amount() == null || req.top_up_amount() < 0) {
      throw new BusinessException("Paramter amount hanya boleh angka dan tidak boleh lebih kecil dari 0");
    }

    var res = service.topup(email, req.top_up_amount());
    return ResponseEntity.ok(ApiResponse.ok("Top Up Balance berhasil", res));
  }
}
