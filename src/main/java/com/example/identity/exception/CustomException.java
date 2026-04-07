package com.example.identity.exception;

import lombok.Getter;

@Getter
public class CustomException extends RuntimeException {
    String messageError;
    String fieldError ;

    
    public CustomException() {
    }

    public CustomException(String messageError, String fieldError) {
        super(messageError);
        this.fieldError = fieldError;
        this.messageError = messageError;
    }

}
