package com.distri.chat.infrastructure.config;

import com.distri.chat.shared.dto.Result;
import com.distri.chat.shared.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 统一处理各类异常，返回标准的Result格式
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * 业务异常处理
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Object>> handleBusinessException(BusinessException e) {
        logger.warn("业务异常：{}", e.getMessage());
        
        Result<Object> result = Result.error(e.getCode(), e.getMessage());
        HttpStatus status = HttpStatus.valueOf(e.getCode());
        
        return ResponseEntity.status(status).body(result);
    }
    
    /**
     * 参数校验异常处理 - @Valid
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Object>> handleValidationException(MethodArgumentNotValidException e) {
        logger.warn("参数校验失败：{}", e.getMessage());
        
        List<FieldError> fieldErrors = e.getBindingResult().getFieldErrors();
        String errorMessage = fieldErrors.stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining(", "));
        
        Result<Object> result = Result.badRequest("参数校验失败: " + errorMessage);
        return ResponseEntity.badRequest().body(result);
    }
    
    /**
     * 参数绑定异常处理
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<Object>> handleBindException(BindException e) {
        logger.warn("参数绑定失败：{}", e.getMessage());
        
        List<FieldError> fieldErrors = e.getBindingResult().getFieldErrors();
        String errorMessage = fieldErrors.stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining(", "));
        
        Result<Object> result = Result.badRequest("参数绑定失败: " + errorMessage);
        return ResponseEntity.badRequest().body(result);
    }
    
    /**
     * 非法参数异常处理
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Result<Object>> handleIllegalArgumentException(IllegalArgumentException e) {
        logger.warn("非法参数：{}", e.getMessage());
        
        Result<Object> result = Result.badRequest(e.getMessage());
        return ResponseEntity.badRequest().body(result);
    }
    
    /**
     * 运行时异常处理
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Result<Object>> handleRuntimeException(RuntimeException e) {
        logger.error("运行时异常", e);
        
        Result<Object> result = Result.error("系统异常，请稍后重试");
        return ResponseEntity.internalServerError().body(result);
    }
    
    /**
     * 通用异常处理
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Object>> handleException(Exception e) {
        logger.error("系统异常", e);
        
        Result<Object> result = Result.error("系统异常，请联系管理员");
        return ResponseEntity.internalServerError().body(result);
    }
}
