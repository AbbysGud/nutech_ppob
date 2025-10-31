package com.nutech.ppob.test.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

public class ApiTestUtil {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  public static MockHttpServletRequestBuilder postJson(String url, Object body){
    try {
      return post(url)
        .contentType(MediaType.APPLICATION_JSON)
        .content(MAPPER.writeValueAsString(body));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static String toJson(Object body){
    try {
      return MAPPER.writeValueAsString(body);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}

