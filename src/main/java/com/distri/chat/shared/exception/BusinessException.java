package com.distri.chat.shared.exception;

/**
 * 业务异常
 * 用于处理业务逻辑中的可预期异常
 */
public class BusinessException extends RuntimeException {
    
    /**
     * 错误码
     */
    private Integer code;
    
    /**
     * 错误消息
     */
    private String message;
    
    public BusinessException(String message) {
        super(message);
        this.code = 500;
        this.message = message;
    }
    
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }
    
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.code = 500;
        this.message = message;
    }
    
    public BusinessException(Integer code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.message = message;
    }
    
    // === 便捷工厂方法 ===
    
    /**
     * 参数错误异常
     */
    public static BusinessException badRequest(String message) {
        return new BusinessException(400, message);
    }
    
    /**
     * 未授权异常
     */
    public static BusinessException unauthorized(String message) {
        return new BusinessException(401, message);
    }
    
    /**
     * 禁止访问异常
     */
    public static BusinessException forbidden(String message) {
        return new BusinessException(403, message);
    }
    
    /**
     * 资源不存在异常
     */
    public static BusinessException notFound(String message) {
        return new BusinessException(404, message);
    }
    
    /**
     * 业务逻辑异常
     */
    public static BusinessException business(String message) {
        return new BusinessException(500, message);
    }
    
    // === Getter/Setter ===
    
    public Integer getCode() { return code; }
    public void setCode(Integer code) { this.code = code; }
    
    @Override
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
