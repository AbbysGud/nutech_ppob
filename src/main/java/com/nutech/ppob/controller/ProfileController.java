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
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.*;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/profile")
public class ProfileController {

    private final UserService users;

    public ProfileController(UserService users) {
        this.users = users;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile(Authentication auth){
        String email = (String) auth.getPrincipal();
        var profile = users.getProfile(email);
        return ResponseEntity.ok(ApiResponse.ok("OK", profile));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<Map<String,Object>>> updateProfile(
            Authentication auth,
            @Valid @RequestBody ProfileUpdateRequest req) {
        String email = (String) auth.getPrincipal();
        users.updateProfile(email, req);
        return ResponseEntity.ok(ApiResponse.ok("Profile updated", Map.of(
                "first_name", req.firstName(),
                "last_name", req.lastName()
        )));
    }

    @PutMapping(path = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String,Object>>> uploadProfileImage(
            Authentication auth,
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) throw new BusinessException("Image file required");
        if (file.getSize() > 2 * 1024 * 1024) throw new BusinessException("Image too large (max 2 MB)");

        String ext = StringUtils.trimAllWhitespace(FilenameUtils.getExtension(file.getOriginalFilename())).toLowerCase();
        if (!(ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png") || ext.equals("webp"))) {
            throw new BusinessException("Invalid image type");
        }

        try {
            Path root = Paths.get("uploads");
            if (!Files.exists(root)) Files.createDirectories(root);

            String filename = "profile_" + UUID.randomUUID() + "." + ext;
            Path dest = root.resolve(filename);
            Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);

            String url = "/uploads/" + filename;
            String email = (String) auth.getPrincipal();
            users.updateProfileImage(email, url);

            return ResponseEntity.ok(ApiResponse.ok("Profile image updated", Map.of("profile_image_url", url)));
        } catch (Exception e) {
            throw new BusinessException("Failed to save image");
        }
    }
}
