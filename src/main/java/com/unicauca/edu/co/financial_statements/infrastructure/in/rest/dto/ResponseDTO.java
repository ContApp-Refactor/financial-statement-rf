package com.unicauca.edu.co.financial_statements.infrastructure.in.rest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.ResponseEntity;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseDTO<T> {
    private T data;
    private Integer statusCode;
    private Integer code;
    private String message;

    public ResponseEntity<ResponseDTO<T>> of() {
        int resolvedStatus = this.statusCode != null
                ? this.statusCode
                : (this.code != null ? this.code : 200);

        if (this.statusCode == null) {
            this.statusCode = resolvedStatus;
        }

        if (this.code == null) {
            this.code = resolvedStatus;
        }

        return ResponseEntity.status(resolvedStatus).body(this);
    }
}
