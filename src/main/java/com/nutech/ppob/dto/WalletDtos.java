package com.nutech.ppob.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class WalletDtos {
  public record TopupRequest(
      @NotNull(message = "Paramter amount hanya boleh angka dan tidak boleh lebih kecil dari 0")
      @Min(value = 0, message = "Paramter amount hanya boleh angka dan tidak boleh lebih kecil dari 0")
      Integer top_up_amount
  ) {}
  public record BalanceResponse(long balance) {}
  public record TopupResponse(long balance) {}
}
