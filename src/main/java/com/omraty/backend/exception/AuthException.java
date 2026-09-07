package com.omraty.backend.exception;

public class AuthException extends RuntimeException {

  AuthException(String message) {
    super(message);
  }

  AuthException(String message, Throwable cause) {
    super(message, cause);
  }

  public static class PhoneAlreadyUsedException extends AuthException {
    public PhoneAlreadyUsedException(String message) {
      super(message);
    }
  }

  public static class InvalidCredentialsException extends AuthException {
    public InvalidCredentialsException(String message) {
      super(message);
    }
  }

  public static class InvalidRefreshTokenException extends AuthException {
    public InvalidRefreshTokenException(String message) {
      super(message);
    }
  }

  public static class InvalidTokenException extends AuthException {
    public InvalidTokenException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
