package com.p2.isitout.exception;

import com.p2.isitout.dto.ResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ResponseDTO<Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        ResponseDTO<Object>  response = ResponseDTO.builder()
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .status(false)
                    .message(ex.getMessage())
                    .data(null)
                    .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ResponseDTO<Object>> handleAccessDeniedException(AccessDeniedException ex) {
        ResponseDTO<Object>  response = ResponseDTO.builder()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .status(false)
                .message(ex.getMessage())
                .data(null)
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}