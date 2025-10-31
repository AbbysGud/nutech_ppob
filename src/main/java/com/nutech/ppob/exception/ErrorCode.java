package com.nutech.ppob.exception;

public enum ErrorCode {
    BAD_REQUEST("400"),
    UNAUTHORIZED("401"),
    FORBIDDEN("403"),
    NOT_FOUND("404"),
    CONFLICT("409"),
    VALIDATION("422"),
    BUSINESS("400"),
    INTERNAL("500");

    public final String code;
    ErrorCode(String c){ this.code = c; }
}