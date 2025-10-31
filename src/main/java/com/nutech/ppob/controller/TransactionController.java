package com.nutech.ppob.controller;

import com.nutech.ppob.exception.BusinessException;
import com.nutech.ppob.dto.ApiResponse;
import com.nutech.ppob.service.TransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
public class TransactionController {

  private final TransactionService txService;

  public TransactionController(TransactionService txService){
    this.txService = txService;
  }

  @PostMapping("/transaction")
  public ResponseEntity<ApiResponse<Map<String,Object>>> pay(
    Authentication auth,
    @RequestBody Map<String,String> body
  ){
    String email = (String) auth.getPrincipal();
    String serviceCode = body.get("service_code");
    if (serviceCode == null || serviceCode.isBlank()){
      throw new BusinessException("Paramter service_code tidak boleh kosong");
    }
    var data = txService.pay(email, serviceCode);
    return ResponseEntity.ok(ApiResponse.of(0, "Transaksi berhasil", data));
  }

  @GetMapping("/transaction/history")
  public ResponseEntity<ApiResponse<Map<String,Object>>> history(
    Authentication auth,
    @RequestParam(value="offset", required=false) Integer offset,
    @RequestParam(value="limit", required=false) Integer limit
  ){
    String email = (String) auth.getPrincipal();
    var data = txService.history(email, offset, limit);
    return ResponseEntity.ok(ApiResponse.of(0, "Get History Berhasil", data));
  }
}
