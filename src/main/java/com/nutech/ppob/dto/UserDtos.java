package com.nutech.ppob.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UserDtos {

  public record RegistrationRequest(
    @NotBlank @Email String email,
    @NotBlank @Size(min = 6, max = 100) String password,
    @NotBlank @Size(min = 2, max = 100) String firstName,
    @NotBlank @Size(min = 2, max = 100) String lastName
  ) {}

  public record LoginRequest(
    @NotBlank @Email String email,
    @NotBlank String password
  ) {}

  public record LoginResponse(String token, String tokenType) {
    public LoginResponse(String token) {
      this(token, "Bearer");
    }
  }

  public record ProfileResponse(
    String email,
    String first_name,
    String last_name,
    String profile_image
  ) {}

  public record ProfileUpdateRequest(
      @NotBlank @Size(min = 2, max = 100) String firstName,
      @NotBlank @Size(min = 2, max = 100) String lastName
  ) {}
}