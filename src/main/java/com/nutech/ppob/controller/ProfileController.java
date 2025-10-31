package com.nutech.ppob.controller;

import com.nutech.ppob.dto.ApiResponse;
import com.nutech.ppob.dto.UserDtos.ProfileResponse;
import com.nutech.ppob.dto.UserDtos.ProfileUpdateRequest;
import com.nutech.ppob.exception.BusinessException;
import com.nutech.ppob.service.UserService;
import jakarta.validation.Valid;
import org.apache.commons.io.FilenameUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.*;
import java.util.UUID;

@RestController
@RequestMapping("/profile")
public class ProfileController {

  private final UserService users;

  public ProfileController(UserService users) {
    this.users = users;
  }

  @GetMapping
  public ResponseEntity<ApiResponse<ProfileResponse>> getProfile(Authentication auth) {
    String email = (String) auth.getPrincipal();
    var profile = users.getProfile(email);
    return ResponseEntity.ok(ApiResponse.ok("Sukses", profile));
  }

  @PutMapping("/update")
  public ResponseEntity<ApiResponse<ProfileResponse>> updateProfile(
    Authentication auth,
    @Valid @RequestBody ProfileUpdateRequest req
  ) {
    String email = (String) auth.getPrincipal();
    var updated = users.updateProfileAndReturn(email, req);
    return ResponseEntity.ok(ApiResponse.ok("Update Pofile berhasil", updated));
  }

  @PutMapping(path = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ApiResponse<ProfileResponse>> uploadProfileImage(
    Authentication auth,
    @RequestParam("file") MultipartFile file
  ) {

    if (file.isEmpty()) throw new BusinessException("Format Image tidak sesuai"); // treat as invalid

    String original = file.getOriginalFilename();
    String ext = original != null ? FilenameUtils.getExtension(original).trim().toLowerCase() : "";

    if (!(ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png"))) {
      throw new BusinessException("Format Image tidak sesuai");
    }

    if (file.getSize() > 5L * 1024 * 1024) {
      throw new BusinessException("Format Image tidak sesuai");
    }

    try {
      Path root = Paths.get("uploads");
      if (!Files.exists(root)) Files.createDirectories(root);

      String filename = "profile_" + UUID.randomUUID() + "." + ext;
      Path dest = root.resolve(filename);
      Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);

      String url = "/uploads/" + filename;
      String email = (String) auth.getPrincipal();
      var updated = users.updateProfileImageAndReturn(email, url);

      return ResponseEntity.ok(ApiResponse.ok("Update Profile Image berhasil", updated));
    } catch (Exception e) {
      throw new BusinessException("Format Image tidak sesuai");
    }
  }
}
