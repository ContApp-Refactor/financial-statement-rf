package com.unicauca.edu.co.financial_statements.infrastructure.out.exception;

import com.unicauca.edu.co.financial_statements.application.useCase.financialStatement.FinancialStatementGenerationException;
import com.unicauca.edu.co.financial_statements.application.useCase.financialStatement.FinancialStatementSignatureException;
import com.unicauca.edu.co.financial_statements.infrastructure.in.rest.dto.ResponseDTO;
import com.unicauca.edu.co.financial_statements.infrastructure.out.clients.accountingInfo.AccountInfoClientException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(FinancialStatementGenerationException.class)
    public ResponseEntity<ResponseDTO<Void>> handleGenerationException(FinancialStatementGenerationException ex) {
        log.error("Financial statement generation failed.", ex);
        return ResponseDTO.<Void>builder()
                .statusCode(HttpStatus.BAD_GATEWAY.value())
                .code(HttpStatus.BAD_GATEWAY.value())
                .message("Error al generar el reporte. Intente más tarde")
                .build()
                .of();
    }

    @ExceptionHandler(FinancialStatementSignatureException.class)
    public ResponseEntity<ResponseDTO<Void>> handleSignatureException(FinancialStatementSignatureException ex) {
        log.error("Financial statement signature processing failed.", ex);
        return ResponseDTO.<Void>builder()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .message(ex.getMessage())
                .build()
                .of();
    }

    @ExceptionHandler(AccountInfoClientException.class)
    public ResponseEntity<ResponseDTO<Void>> handleAccountingIntegration(AccountInfoClientException ex) {
        HttpStatus status = ex.getStatus() != null ? ex.getStatus() : HttpStatus.BAD_GATEWAY;
        log.error("Accounting integration error.", ex);

        return ResponseDTO.<Void>builder()
                .statusCode(status.value())
                .code(status.value())
                .message("Error al generar el reporte. Intente más tarde")
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
        log.error("Unexpected financial statement error.", ex);
        return ResponseDTO.<Void>builder()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .message("Ocurrio un error inesperado. Intente nuevamente mas tarde.")
                .build()
                .of();
    }
}
