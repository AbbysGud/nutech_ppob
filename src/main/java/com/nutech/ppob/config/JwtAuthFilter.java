package com.nutech.ppob.config;

import com.nutech.ppob.util.JwtUtil;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

  private final JwtUtil jwt;

  @Override
  protected void doFilterInternal(
    HttpServletRequest req, 
    HttpServletResponse res, 
    FilterChain fc
  ) throws ServletException, IOException {

    String header = req.getHeader(HttpHeaders.AUTHORIZATION);
    if (header != null && header.startsWith("Bearer ")) {
      String token = header.substring(7);
      try {
        if (jwt.isValid(token)) {
          String subject = jwt.getSubject(token); // email atau userId
          Authentication authn = new UsernamePasswordAuthenticationToken(subject, null, List.of());
          SecurityContextHolder.getContext().setAuthentication(authn);
        }
      } catch (JwtException ex) {
        // Token invalid/expired → biarkan tanpa auth; EntryPoint akan handle 401 jika
        // endpoint butuh auth
        log.warn("Invalid JWT: {}", ex.getMessage());
      } catch (Exception ex) {
        log.error("JWT filter error", ex);
      }
    }
    fc.doFilter(req, res);
  }
}
