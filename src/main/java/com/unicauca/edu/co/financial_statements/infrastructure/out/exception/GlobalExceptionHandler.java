package com.unicauca.edu.co.financial_statements.infrastructure.out.exception;

import com.unicauca.edu.co.financial_statements.infrastructure.in.rest.dto.ResponseDTO;
import com.unicauca.edu.co.financial_statements.infrastructure.out.clients.accountingInfo.AccountInfoClientException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccountInfoClientException.class)
    public ResponseEntity<ResponseDTO<Void>> handleAccountingIntegration(AccountInfoClientException ex) {
        HttpStatus status = ex.getStatus() != null ? ex.getStatus() : HttpStatus.BAD_GATEWAY;

        return ResponseDTO.<Void>builder()
                .statusCode(status.value())
                .code(status.value())
                .message(ex.getMessage())
                .build()
                .of();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ResponseDTO<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseDTO.<Void>builder()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .code(HttpStatus.BAD_REQUEST.value())
                .message(ex.getMessage())
                .build()
                .of();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseDTO<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Validation error");

        return ResponseDTO.<Void>builder()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .code(HttpStatus.BAD_REQUEST.value())
                .message(message)
                .build()
                .of();
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ResponseDTO<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        String detailedMessage = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage();

        String message = "Invalid request payload. Verify JSON structure and use date format yyyy-MM-dd or dd/MM/yyyy.";
        if (detailedMessage != null && detailedMessage.contains("LocalDate")) {
            message = "Invalid date format. Use yyyy-MM-dd or dd/MM/yyyy.";
        }

        return ResponseDTO.<Void>builder()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .code(HttpStatus.BAD_REQUEST.value())
                .message(message)
                .build()
                .of();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseDTO<Void>> handleGeneralException(Exception ex) {
        return ResponseDTO.<Void>builder()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .message("An unexpected error occurred: " + ex.getMessage())
                .build()
                .of();
    }
}
