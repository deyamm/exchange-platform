package com.exchange.platform.common.server.advice;

import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolationException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.exchange.platform.common.core.exception.BusinessException;
import com.exchange.platform.common.core.result.ApiResponse;
import com.exchange.platform.common.server.support.RequestIdHolder;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    // 处理业务异常，返回统一的错误响应格式
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex, HttpServletRequest request) {
        
        return ResponseEntity.ok(
            ApiResponse.fail(ex.getCode(), ex.getMessage(), RequestIdHolder.getRequestId(request))
        );
    }

    // 处理方法参数验证异常，返回统一的错误响应格式
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex, HttpServletRequest request) {

        String msg = ex.getBindingResult().getFieldErrors().stream()
            .map(this::formatFieldError)
            .collect(Collectors.joining("; "));

        return ResponseEntity.badRequest().body(
            ApiResponse.fail(4001, msg, RequestIdHolder.getRequestId(request))
        );
    }

    // 处理参数约束验证异常，返回统一的错误响应格式
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(ConstraintViolationException ex, HttpServletRequest request) {

        return ResponseEntity.badRequest().body(
            ApiResponse.fail(4002, ex.getMessage(), RequestIdHolder.getRequestId(request))
        );
    }

    // 处理其他未捕获的异常，返回统一的错误响应格式
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex, HttpServletRequest request) {
        ex.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            ApiResponse.fail(5001, "系统内部错误", RequestIdHolder.getRequestId(request))
        );
    }

    private String formatFieldError(FieldError e) {
        return e.getField() + ": " + e.getDefaultMessage();
    }
}
