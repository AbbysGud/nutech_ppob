package com.nutech.ppob.repo;

import com.nutech.ppob.dto.UserDtos.ProfileResponse;
import com.nutech.ppob.exception.ResourceNotFoundException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

@Repository
public class UserRepo {

  private final JdbcTemplate jdbc;

  public UserRepo(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  private static class ProfileRowMapper implements RowMapper<ProfileResponse> {
    @Override
    public ProfileResponse mapRow(ResultSet rs, int rowNum) throws SQLException {
      return new ProfileResponse(
        rs.getString("first_name"),
        rs.getString("last_name"),
        rs.getString("email"),
        rs.getString("profile_image")
      );
    }
  }

  public boolean emailExists(String email) {
    Integer cnt = jdbc.queryForObject(
      "SELECT COUNT(*) FROM users WHERE email = ?",
      Integer.class, 
      email
    );
    return cnt != null && cnt > 0;
  }

  public void insertUser(String email, String passwordHash, String firstName, String lastName)
    throws DuplicateKeyException {
    jdbc.update("""
        INSERT INTO users (
          email,
          password_hash,
          first_name,
          last_name,
          created_at,
          updated_at
        ) VALUES (
          ?,
          ?,
          ?,
          ?,
          CURRENT_TIMESTAMP,
          CURRENT_TIMESTAMP
        )
      """, email, passwordHash, firstName, lastName
    );
  }

  public Long getUserIdByEmail(String email) {
    String sql = "SELECT id FROM users WHERE email = ?";
    return jdbc.queryForObject(sql, Long.class, email);
  }

  public void insertWalletZero(String email) {
    Long userId = jdbc.queryForObject(
      "SELECT id FROM users WHERE email = ?",
      Long.class, email
    );

    if (userId == null) throw new ResourceNotFoundException("User not found");

    jdbc.update("""
        INSERT INTO wallets (
          user_id, 
          balance, 
          version, 
          created_at, 
          updated_at
        ) VALUES (
          ?, 
          0, 
          0, 
          CURRENT_TIMESTAMP, 
          CURRENT_TIMESTAMP
        )
      """, userId
    );
  }

  public Optional<String> getPasswordHashByEmail(String email) {
    try {
      String ph = jdbc.queryForObject(
        "SELECT password_hash FROM users WHERE email = ?",
        String.class, email
      );
      return Optional.ofNullable(ph);
    } catch (EmptyResultDataAccessException e) {
      return Optional.empty();
    }
  }

  public void updateLastLogin(String email) {
    jdbc.update("UPDATE users SET last_login_at = CURRENT_TIMESTAMP WHERE email = ?", email);
  }

  public ProfileResponse getProfileByEmail(String email) {
    try {
      return jdbc.queryForObject("""
          SELECT
            email, 
            first_name, 
            last_name, 
            COALESCE(profile_image_url,'') AS profile_image
          FROM users 
          WHERE email = ?
        """, new ProfileRowMapper(), email);
    } catch (EmptyResultDataAccessException e) {
      throw new ResourceNotFoundException("User not found");
    }
  }

  public int updateProfile(String email, String firstName, String lastName) {
    return jdbc.update("""
        UPDATE users 
        SET 
          first_name = ?, 
          last_name = ?, 
          updated_at = CURRENT_TIMESTAMP
        WHERE email = ?
      """, firstName, lastName, email);
  }

  public int updateProfileImage(String email, String imageUrl) {
    return jdbc.update("""
        UPDATE users 
        SET 
          profile_image_url = ?, 
          updated_at = CURRENT_TIMESTAMP
        WHERE email = ?
      """, imageUrl, email);
  }
}
