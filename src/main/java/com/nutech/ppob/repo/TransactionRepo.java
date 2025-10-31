package com.nutech.ppob.repo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class TransactionRepo {
  private final JdbcTemplate jdbc;

  public TransactionRepo(JdbcTemplate jdbc){ this.jdbc = jdbc; }

  public Optional<Map<String,Object>> findActiveService(String code){
    String sql = """
      SELECT 
        service_code, 
        service_name, 
        service_icon_url, 
        service_tariff
      FROM services
      WHERE service_code = ? 
        AND is_active = 1
    """;
    var list = jdbc.queryForList(sql, code);
    return list.isEmpty()? Optional.empty(): Optional.of(list.get(0));
  }

  public Long selectBalanceForUpdate(Long userId){
    String sql = """
        SELECT balance FROM wallets WHERE user_id = ? FOR UPDATE
      """;
    return jdbc.queryForObject(sql, Long.class, userId);
  }

  public int updateBalance(Long userId, long newBalance){
    String sql = """
        UPDATE wallets 
        SET 
          balance = ?, 
          updated_at = CURRENT_TIMESTAMP 
        WHERE user_id = ?
      """;
    return jdbc.update(sql, newBalance, userId);
  }

  public int insertPayment(Long userId, String invoice, String serviceCode, String serviceName, long amount){
    String sql = """
        INSERT INTO wallet_transactions (
          user_id, 
          invoice_number, 
          transaction_type, 
          service_code, 
          service_name_snapshot, 
          description, 
          amount, 
          created_on
        ) VALUES (
          ?,
          ?,
          ?,
          ?,
          ?,
          ?,
          ?, 
          CURRENT_TIMESTAMP
        )
      """;
    return jdbc.update(sql, userId, invoice, "PAYMENT", serviceCode, serviceName, serviceName, amount);
  }

  public List<Map<String,Object>> findHistory(Long userId, Integer offset, Integer limit){
    var args = new ArrayList<Object>();
    String base = """
        SELECT 
          invoice_number, 
          transaction_type, 
          description, 
          amount AS total_amount, 
          created_on
        FROM wallet_transactions
        WHERE user_id = ?
        ORDER BY created_on DESC
      """;
    args.add(userId);
    if (limit != null){
      base += " LIMIT ? ";
      args.add(limit);
      if (offset != null){
        base += " OFFSET ? ";
        args.add(offset);
      }
    }
    return jdbc.queryForList(base, args.toArray());
  }
}
