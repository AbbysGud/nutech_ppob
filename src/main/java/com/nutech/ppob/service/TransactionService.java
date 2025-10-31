package com.nutech.ppob.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.nutech.ppob.exception.BusinessException;
import com.nutech.ppob.repo.TransactionRepo;
import com.nutech.ppob.repo.UserRepo;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

@Service
public class TransactionService {
  private final TransactionRepo txRepo;
  private final UserRepo userRepo;
  private final PlatformTransactionManager txManager;

  public TransactionService(TransactionRepo txRepo, UserRepo userRepo, PlatformTransactionManager txManager){
    this.txRepo = txRepo;
    this.userRepo = userRepo;
    this.txManager = txManager;
  }

  public Map<String,Object> pay(String email, String serviceCode){

    Long userId = userRepo.getUserIdByEmail(email);

    var svcOpt = txRepo.findActiveService(serviceCode);
    if (svcOpt.isEmpty()){
      throw new BusinessException("Service ataus Layanan tidak ditemukan");
    }
    var svc = svcOpt.get();
    long tariff = ((Number)svc.get("service_tariff")).longValue();
    String serviceName = (String) svc.get("service_name");

    TransactionStatus status = txManager.getTransaction(new DefaultTransactionDefinition());
    try {
      long balance = txRepo.selectBalanceForUpdate(userId);
      if (balance < tariff){
        throw new BusinessException("Saldo tidak mencukupi");
      }

      String invoice = generateInvoice();

      txRepo.insertPayment(userId, invoice, serviceCode, serviceName, tariff);

      txRepo.updateBalance(userId, balance - tariff);

      txManager.commit(status);

      var data = new LinkedHashMap<String,Object>();
      data.put("invoice_number", invoice);
      data.put("service_code", serviceCode);
      data.put("service_name", serviceName);
      data.put("transaction_type", "PAYMENT");
      data.put("total_amount", tariff);
      data.put("created_on", Instant.now().toString());
      return data;
    } catch (RuntimeException ex){
      txManager.rollback(status);
      throw ex;
    }
  }

  public Map<String,Object> history(String email, Integer offset, Integer limit){
    Long userId = userRepo.getUserIdByEmail(email);
    List<Map<String,Object>> rows = txRepo.findHistory(userId, offset, limit);
    var data = new LinkedHashMap<String,Object>();
    data.put("offset", offset==null?0:offset);
    data.put("limit", limit==null?0:limit);
    data.put("records", rows);
    return data;
  }

  private String generateInvoice(){
    return "INV" + DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDate.now()) + "-" +
      UUID.randomUUID().toString().substring(0,6).toUpperCase();
  }
}
