package com.threlease.base.utils.security;

import com.threlease.base.utils.responses.BasicResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.Optional;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(value = {NullPointerException.class})
    public ResponseEntity<?> handleNullPointerException(NullPointerException ex, WebRequest request) {
        BasicResponse response = BasicResponse.builder()
                .success(false)
                .message(Optional.of("올바른 요청이 아닙니다."))
                .data(Optional.empty())
                .build();

        return ResponseEntity.status(400).body(response);
    }
}
