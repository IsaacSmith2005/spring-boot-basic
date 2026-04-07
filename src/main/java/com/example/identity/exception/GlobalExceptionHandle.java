package com.example.identity.exception;

import com.example.identity.dto.response.APIResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandle {
    @ExceptionHandler(RuntimeException.class)
    ResponseEntity<APIResponse<Object>> handleRuntimeException(RuntimeException e) {
        APIResponse<Object> response = new APIResponse<>();
        response.setText(e.getMessage());
        System.out.println("abc------------------------->");
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    ResponseEntity<APIResponse<Object>> handlingValidation(MethodArgumentNotValidException e) {
        APIResponse<Object> response = new APIResponse<>();
         System.out.println("12312312abc------------------------->");
        String messageError = e.getFieldError().getDefaultMessage();
        String fieldError = e.getFieldError().getField();
        response.setText(messageError);
        response.setFieldError(fieldError);
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(CustomException.class)
    ResponseEntity<APIResponse<Object>> handleCustomException(CustomException e) {
        APIResponse<Object> response = new APIResponse<>();
         System.out.println("12312312abc------------------------->");
        String message = e.getMessageError();
        String fieldError = e.getFieldError();
        response.setText(message);
        response.setFieldError(fieldError);
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(AuthenticateException.class)
    ResponseEntity<APIResponse<Object>> handleAuthenticationException(AuthenticateException e) {
        String message = e.getMessage();
        APIResponse<Object> response = APIResponse.builder()
        .code(HttpStatus.UNAUTHORIZED.value())
        .text(message)
        .build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<APIResponse<Object>> handleAuthorizeException(AccessDeniedException e) {
        APIResponse<Object> response = APIResponse.builder()
        .code(HttpStatus.FORBIDDEN.value())
        .text("Access Denied")
        .build();
        System.out.println("r32r32rt34ttewr23432----------------------------------");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(JwtException.class)
    ResponseEntity<APIResponse<Object>> handleJwtException(JwtException e) {
        APIResponse<Object> response = APIResponse.builder()
        .code(HttpStatus.UNAUTHORIZED.value())
        .text("Token invalid")
        .build();
        System.out.println("r32r32rt34ttewr23432----------------------------------");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }
}

