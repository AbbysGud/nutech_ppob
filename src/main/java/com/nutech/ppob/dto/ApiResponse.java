package com.nutech.ppob.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(String code, String message, T data) {
    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>("200", message, data);
    }
    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
