package com.nutech.ppob.controller;

import com.nutech.ppob.repo.SmokeRepo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class DbCheckController {
  private final SmokeRepo repo;

  public DbCheckController(SmokeRepo repo) {
    this.repo = repo;
  }

  @GetMapping("/db/check")
  public Map<String, Object> check() {
    int services = repo.countServices();
    return Map.of(
      "code", "200",
      "message", "DB OK",
      "data", Map.of("services_count", services));
  }
}
