package com.example.identity.exception;

import lombok.Getter;

@Getter
public class AuthenticateException extends RuntimeException {
    
    public AuthenticateException() {
    }

    public AuthenticateException(String messageError) {
        super(messageError);
    }

}