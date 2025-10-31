package com.nutech.ppob.service;

import com.nutech.ppob.dto.WalletDtos.*;
import com.nutech.ppob.exception.BusinessException;
import com.nutech.ppob.repo.WalletRepo;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class WalletService {
  private final WalletRepo repo;

  public WalletService(WalletRepo repo) { this.repo = repo; }

  public BalanceResponse getBalance(String email) {
    Long bal = repo.getBalanceByEmail(email);
    if (bal == null) throw new BusinessException("Wallet not found");
    return new BalanceResponse(bal);
  }

  @Transactional
  public TopupResponse topup(String email, int amount) {
    if (amount < 0) {
      throw new BusinessException("Paramter amount hanya boleh angka dan tidak boleh lebih kecil dari 0");
    }

    Long userId = repo.getUserIdByEmail(email);
    if (userId == null) throw new BusinessException("User not found");

    Long before = repo.lockAndGetBalanceByEmail(email);
    if (before == null) throw new BusinessException("Wallet not found");

    long after = before + amount;

    String invoice = genInvoice();
    try {
      repo.insertTopupTx(userId, invoice, amount, before, after);
    } catch (DuplicateKeyException e) {
      invoice = genInvoice();
      repo.insertTopupTx(userId, invoice, amount, before, after);
    }

    repo.updateWalletBalance(userId, after);
    return new TopupResponse(after);
  }

  private String genInvoice() {
    String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    int rnd = ThreadLocalRandom.current().nextInt(100, 999);
    return "INV" + ts + "-" + rnd;
  }
}
