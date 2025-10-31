package com.nutech.ppob.repo;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;

@Repository
public class WalletRepo {
  private final JdbcTemplate jdbc;

  public WalletRepo(JdbcTemplate jdbc) { this.jdbc = jdbc; }

  public Long getBalanceByEmail(String email) {
    String sql = """
      SELECT w.balance
      FROM wallets w
      JOIN users u ON u.id = w.user_id
      WHERE u.email = ?
    """;
    return jdbc.query(sql, ps -> ps.setString(1, email), (ResultSet rs) ->
      rs.next() ? rs.getLong("balance") : null
    );
  }

  public Long lockAndGetBalanceByEmail(String email) {
    String sql = """
      SELECT w.balance
      FROM wallets w
      JOIN users u ON u.id = w.user_id
      WHERE u.email = ?
      FOR UPDATE
    """;
    return jdbc.query(sql, ps -> ps.setString(1, email), (ResultSet rs) ->
      rs.next() ? rs.getLong("balance") : null
    );
  }

  public Long getUserIdByEmail(String email) {
    return jdbc.query("""
      SELECT id FROM users WHERE email = ?
    """, ps -> ps.setString(1, email), rs -> rs.next() ? rs.getLong("id") : null);
  }

  public int insertTopupTx(long userId, String invoice, long amount, long before, long after) {
    String sql = """
      INSERT INTO wallet_transactions (
        user_id, 
        invoice_number, 
        transaction_type, 
        amount,
        balance_before, 
        balance_after, 
        description
      ) VALUES (
        ?,
        ?,
        ?,
        ?,
        ?,
        ?,
        ?
      )
    """;
    return jdbc.update(sql, ps -> {
      ps.setLong(1, userId);
      ps.setString(2, invoice);
      ps.setString(3, "TOPUP");
      ps.setLong(4, amount);
      ps.setLong(5, before);
      ps.setLong(6, after);
      ps.setString(7, "Top Up balance");
    });
  }

  public int updateWalletBalance(long userId, long newBalance) {
    String sql = """
      UPDATE wallets
      SET 
        balance = ?, 
        version = version + 1, 
        updated_at = CURRENT_TIMESTAMP
      WHERE user_id = ?
    """;
    return jdbc.update(sql, ps -> {
      ps.setLong(1, newBalance);
      ps.setLong(2, userId);
    });
  }
}
