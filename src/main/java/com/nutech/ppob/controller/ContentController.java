package com.nutech.ppob.controller;

import com.nutech.ppob.dto.ApiResponse;
import com.nutech.ppob.dto.content.BannerItem;
import com.nutech.ppob.dto.content.ServiceItem;
import com.nutech.ppob.service.ContentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ContentController {

  private final ContentService service;

  public ContentController(ContentService service) {
    this.service = service;
  }

  @GetMapping("/banner")
  public ResponseEntity<ApiResponse<List<BannerItem>>> listBanners() {
    return ResponseEntity.ok(ApiResponse.ok("Sukses", service.listBanners()));
  }

  @GetMapping("/services")
  public ResponseEntity<ApiResponse<List<ServiceItem>>> listServices() {
    return ResponseEntity.ok(ApiResponse.ok("Sukses", service.listServices()));
  }
}
