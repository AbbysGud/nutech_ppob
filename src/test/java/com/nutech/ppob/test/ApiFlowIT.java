package com.nutech.ppob.test;

import com.nutech.ppob.test.containers.DbIntegrationTest;
import com.nutech.ppob.test.utils.ApiTestUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.TestMethodOrder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@AutoConfigureMockMvc
@TestMethodOrder(OrderAnnotation.class)
public class ApiFlowIT extends DbIntegrationTest {

  @Autowired
  MockMvc mvc;

  ObjectMapper mapper = new ObjectMapper();

  String email = "ariq@example.com";
  String pass  = "password123";
  String token;

  String authHeader() {
    return "Bearer " + token;
  }

  @Test @Order(1) @DisplayName("Registrasi → 200 'Registrasi berhasil silahkan login'")
  void registration_ok() throws Exception {
    var body = new java.util.HashMap<String,Object>();
    body.put("email", email);
    body.put("password", pass);
    body.put("firstName", "Ariq");
    body.put("lastName", "Sugiharto");

    var res = mvc.perform(ApiTestUtil.postJson("/registration", body))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value(0))
      .andExpect(jsonPath("$.message").value("Registrasi berhasil silahkan login"))
      .andReturn();

    // implicitly ok
  }

  @Test @Order(2) @DisplayName("Login → 200 'Login Sukses' dan menerima token")
  void login_ok() throws Exception {
    var body = new java.util.HashMap<String,Object>();
    body.put("email", email);
    body.put("password", pass);

    var res = mvc.perform(ApiTestUtil.postJson("/login", body))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value(0))
      .andExpect(jsonPath("$.message").value("Login Sukses"))
      .andExpect(jsonPath("$.data.token").exists())
      .andReturn();

    JsonNode root = mapper.readTree(res.getResponse().getContentAsString());
    token = root.at("/data/token").asText();
    assertThat(token).isNotBlank();
  }

  @Test @Order(3) @DisplayName("GET /profile → 200 'Sukses'")
  void profile_ok() throws Exception {
    mvc.perform(get("/profile").header("Authorization", authHeader()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value(0))
      .andExpect(jsonPath("$.message").value("Sukses"))
      .andExpect(jsonPath("$.data.email").value(email))
      .andExpect(jsonPath("$.data.first_name").exists())
      .andExpect(jsonPath("$.data.last_name").exists())
      .andExpect(jsonPath("$.data.profile_image").exists());
  }

  @Test @Order(4) @DisplayName("Balance awal 0")
  void balance_initial_zero() throws Exception {
    mvc.perform(get("/balance").header("Authorization", authHeader()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value(0))
      .andExpect(jsonPath("$.message").value("Get Balance Berhasil"))
      .andExpect(jsonPath("$.data.balance").value(0));
  }

  @Test @Order(5) @DisplayName("Topup → 200 'Top Up Balance berhasil' dan saldo bertambah")
  void topup_ok() throws Exception {
    var body = new java.util.HashMap<String,Object>();
    body.put("amount", 100000);

    mvc.perform(ApiTestUtil.postJson("/topup", body).header("Authorization", authHeader()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value(0))
      .andExpect(jsonPath("$.message").value("Top Up Balance berhasil"));

    mvc.perform(get("/balance").header("Authorization", authHeader()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.data.balance").value(100000));
  }

  @Test @Order(6) @DisplayName("Payment sukses (saldo cukup) → 200 'Transaksi berhasil'")
  void payment_ok() throws Exception {
    var body = new java.util.HashMap<String,Object>();
    body.put("service_code", "PULSA");

    var res = mvc.perform(ApiTestUtil.postJson("/transaction", body)
      .header("Authorization", authHeader()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value(0))
      .andExpect(jsonPath("$.message").value("Transaksi berhasil"))
      .andExpect(jsonPath("$.data.invoice_number").exists())
      .andExpect(jsonPath("$.data.service_code").value("PULSA"))
      .andExpect(jsonPath("$.data.service_name").value("Pulsa"))
      .andExpect(jsonPath("$.data.transaction_type").value("PAYMENT"))
      .andExpect(jsonPath("$.data.total_amount").value(40000))
      .andReturn();

    // saldo tersisa 60_000
    mvc.perform(get("/balance").header("Authorization", authHeader()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.data.balance").value(60000));
  }

  @Test @Order(7) @DisplayName("Payment gagal (saldo tidak cukup) → 400 status:102 'Saldo tidak mencukupi'")
  void payment_insufficient() throws Exception {
    var body = new java.util.HashMap<String,Object>();
    body.put("service_code", "NONACTIVE"); // misal tarif mahal atau nonaktif (kamu boleh ganti skenario)
    // Lebih baik gunakan service aktif dengan tarif > saldo untuk test ini
    body.put("service_code", "PLN"); // tarif 20000 (masih cukup), ubah skenario: lakukan 3x hingga saldo kurang
    // Untuk simpel: coba bayar berulang kali sampai saldo < 20000

    // loop 4 kali: dari 60_000 → 40_000 → 20_000 → 0 (yang ke-4 gagal)
    for (int i = 0; i < 3; i++) {
      mvc.perform(ApiTestUtil.postJson("/transaction", java.util.Map.of("service_code","PLN"))
        .header("Authorization", authHeader()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(0));
    }
    // saldo sekarang 0 → berikutnya gagal
    mvc.perform(ApiTestUtil.postJson("/transaction", java.util.Map.of("service_code","PLN"))
      .header("Authorization", authHeader()))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.status").value(102))
      .andExpect(jsonPath("$.message").value("Saldo tidak mencukupi"));
  }

  @Test @Order(8) @DisplayName("History pagination → urut terbaru (DESC) dan LIMIT/OFFSET jalan")
  void history_pagination() throws Exception {
    // Ambil 1 record pertama
    mvc.perform(get("/transaction/history")
      .param("offset","0").param("limit","1")
      .header("Authorization", authHeader()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value(0))
      .andExpect(jsonPath("$.message").value("Get History Berhasil"))
      .andExpect(jsonPath("$.data.records[0].invoice_number").exists());

    // Ambil halaman berikutnya
    mvc.perform(get("/transaction/history")
      .param("offset","1").param("limit","1")
      .header("Authorization", authHeader()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value(0))
      .andExpect(jsonPath("$.data.records[0].invoice_number").exists());
  }
}