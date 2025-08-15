package com.distri.chat.api.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;

/**
 * 用户登录请求
 */
@Getter
public class LoginRequest {
    
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
    
    @NotBlank(message = "密码不能为空")
    private String password;
    
    /**
     * 设备ID - 用于WebSocket连接标识
     * 如果为空，系统会自动生成
     */
    private String deviceId;
    
    // 构造函数
    public LoginRequest() {}
    
    public LoginRequest(String phone, String password) {
        this.phone = phone;
        this.password = password;
    }
    
    // Getter methods
    public String getPhone() { return phone; }
    public String getPassword() { return password; }
    public String getDeviceId() { return deviceId; }
    
    // Setter methods
    public void setPhone(String phone) { this.phone = phone; }
    public void setPassword(String password) { this.password = password; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
}