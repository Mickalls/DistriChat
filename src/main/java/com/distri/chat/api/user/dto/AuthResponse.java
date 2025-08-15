package com.distri.chat.api.user.dto;

import lombok.Getter;

/**
 * 认证响应（注册/登录通用）
 */
@Getter
public class AuthResponse {
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 访问令牌
     */
    private String accessToken;
    
    /**
     * 设备ID
     */
    private String deviceId;
    
    /**
     * 用户昵称
     */
    private String nickname;
    
    /**
     * 头像URL
     */
    private String avatar;
    
    // 构造函数
    public AuthResponse() {}
    
    public AuthResponse(Long userId, String accessToken, String deviceId, String nickname, String avatar) {
        this.userId = userId;
        this.accessToken = accessToken;
        this.deviceId = deviceId;
        this.nickname = nickname;
        this.avatar = avatar;
    }
    
    // Getter methods
    public Long getUserId() { return userId; }
    public String getAccessToken() { return accessToken; }
    public String getDeviceId() { return deviceId; }
    public String getNickname() { return nickname; }
    public String getAvatar() { return avatar; }
    
    // Setter methods
    public void setUserId(Long userId) { this.userId = userId; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
}