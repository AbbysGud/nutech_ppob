package com.nutech.ppob.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class PingController {
  @GetMapping("/ping")
  public Map<String, Object> ping() {
    return Map.of(
      "code", "200",
      "message", "pong",
      "data", Map.of("service", "ppob", "status", "OK"));
  }
}
