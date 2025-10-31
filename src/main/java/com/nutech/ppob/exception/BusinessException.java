package com.nutech.ppob.exception;

public class BusinessException extends RuntimeException {
  private final ErrorCode error;
  public BusinessException(String message) { super(message); this.error = ErrorCode.BUSINESS; }
  public BusinessException(ErrorCode error, String message){ super(message); this.error = error; }
  public ErrorCode getError(){ return error; }
}
