package com.nutech.ppob.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;

@Component
public class JwtUtil {

  private final Key key;
  private final int ttlHours;

  public JwtUtil(
    @Value("${app.jwt.secret}") String secret,
    @Value("${app.jwt.ttl-hours}") int ttlHours
  ) {
    byte[] keyBytes = secret.length() >= 32 ? secret.getBytes() : Decoders.BASE64.decode(Base64UrlSafe(secret));
    this.key = Keys.hmacShaKeyFor(keyBytes);
    this.ttlHours = ttlHours;
  }

  public String generate(String subject, Map<String, Object> claims) {
    Instant now = Instant.now();
    return Jwts.builder()
      .setSubject(subject)
      .addClaims(claims)
      .setIssuedAt(Date.from(now))
      .setExpiration(Date.from(now.plus(ttlHours, ChronoUnit.HOURS)))
      .signWith(key, SignatureAlgorithm.HS256)
      .compact();
  }

  public Jws<Claims> parse(String token) {
    return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
  }

  public String getSubject(String token) {
    return parse(token).getBody().getSubject();
  }

  public boolean isValid(String token) {
    try {
      parse(token);
      return true;
    } catch (JwtException e) {
      return false;
    }
  }

  private static String Base64UrlSafe(String s) {
    return java.util.Base64.getEncoder().encodeToString(s.getBytes());
  }
}
