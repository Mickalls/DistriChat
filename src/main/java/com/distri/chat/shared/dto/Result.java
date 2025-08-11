package com.distri.chat.shared.dto;

import lombok.Getter;

/**
 * 统一API响应结果
 * 
 * @param <T> 数据类型
 */
@Getter
public class Result<T> {
    
    /**
     * 响应码
     */
    private Integer code;
    
    /**
     * 响应消息
     */
    private String message;
    
    /**
     * 响应数据
     */
    private T data;
    
    /**
     * 时间戳
     */
    private Long timestamp;
    
    // 私有构造函数
    private Result() {
        this.timestamp = System.currentTimeMillis();
    }
    
    private Result(Integer code, String message, T data) {
        this();
        this.code = code;
        this.message = message;
        this.data = data;
    }
    
    // === 成功响应 ===
    
    /**
     * 成功响应（无数据）
     */
    public static <T> Result<T> success() {
        return new Result<>(200, "操作成功", null);
    }
    
    /**
     * 成功响应（带数据）
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }
    
    /**
     * 成功响应（自定义消息）
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data);
    }
    
    // === 失败响应 ===
    
    /**
     * 失败响应（通用错误）
     */
    public static <T> Result<T> error() {
        return new Result<>(500, "操作失败", null);
    }
    
    /**
     * 失败响应（自定义消息）
     */
    public static <T> Result<T> error(String message) {
        return new Result<>(500, message, null);
    }
    
    /**
     * 失败响应（自定义错误码和消息）
     */
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }
    
    // === 业务状态响应 ===
    
    /**
     * 参数错误响应
     */
    public static <T> Result<T> badRequest(String message) {
        return new Result<>(400, message, null);
    }
    
    /**
     * 未授权响应
     */
    public static <T> Result<T> unauthorized() {
        return new Result<>(401, "未授权访问", null);
    }
    
    /**
     * 禁止访问响应
     */
    public static <T> Result<T> forbidden() {
        return new Result<>(403, "禁止访问", null);
    }
    
    /**
     * 资源不存在响应
     */
    public static <T> Result<T> notFound() {
        return new Result<>(404, "资源不存在", null);
    }
    
    // === 判断方法 ===
    
    /**
     * 判断是否成功
     */
    public boolean isSuccess() {
        return code != null && code == 200;
    }
    
    /**
     * 判断是否失败
     */
    public boolean isError() {
        return !isSuccess();
    }
    
    // === Getter/Setter ===
    
    // Getter methods
    public Integer getCode() { return code; }
    public String getMessage() { return message; }
    public T getData() { return data; }
    public Long getTimestamp() { return timestamp; }

    // Setter methods
    public void setCode(Integer code) { this.code = code; }
    public void setMessage(String message) { this.message = message; }
    public void setData(T data) { this.data = data; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
}
