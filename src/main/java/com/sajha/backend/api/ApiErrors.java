package com.sajha.backend.api;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
public class ApiErrors {
    private ResponseEntity<Map<String,Object>> response(HttpStatus status,String code,String message,List<?> details){
        return ResponseEntity.status(status).body(Map.of("error",Map.of(
            "code",code,"message",message,"details",details,"traceId",UUID.randomUUID().toString())));
    }
    @ExceptionHandler(ApiProblem.class)
    ResponseEntity<Map<String,Object>> known(ApiProblem e){
        return response(e.getStatus(),e.getCode(),e.getMessage(),List.of());
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String,Object>> validation(MethodArgumentNotValidException e){
        var issues=e.getBindingResult().getFieldErrors().stream()
            .map(f->Map.of("field",f.getField(),"reason",String.valueOf(f.getDefaultMessage())))
            .toList();
        return response(HttpStatus.UNPROCESSABLE_ENTITY,"VALIDATION_ERROR","Invalid request fields",issues);
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<Map<String,Object>> malformed(HttpMessageNotReadableException e){
        return response(HttpStatus.BAD_REQUEST,"BAD_REQUEST","Invalid JSON request",List.of());
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Map<String,Object>> conflict(DataIntegrityViolationException e){
        return response(HttpStatus.CONFLICT,"CONFLICT","An account or contact value is already in use",List.of());
    }
    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String,Object>> unexpected(Exception e){
        return response(HttpStatus.INTERNAL_SERVER_ERROR,"INTERNAL_ERROR","Unexpected server error",List.of());
    }
}
