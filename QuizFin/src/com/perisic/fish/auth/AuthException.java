package com.perisic.fish.auth;

/** Login/registration failure with a message that is safe to show the user. */
public class AuthException extends Exception {
  private static final long serialVersionUID = 1L;

  public AuthException(String message) {
    super(message);
  }
}
