package com.distri.chat.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

/**
 * 用户注册请求
 */
@Getter
public class RegisterRequest {

    // Getter/Setter
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
    
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须在6-20位之间")
    private String password;
    
    @NotBlank(message = "昵称不能为空")
    @Size(min = 1, max = 20, message = "昵称长度必须在1-20位之间")
    private String nickname;
    
    // 构造函数
    public RegisterRequest() {}
    
    public RegisterRequest(String phone, String password, String nickname) {
        this.phone = phone;
        this.password = password;
        this.nickname = nickname;
    }

    // Getter methods
    public String getPhone() { return phone; }
    public String getPassword() { return password; }
    public String getNickname() { return nickname; }
    
    // Setter methods
    public void setPhone(String phone) { this.phone = phone; }
    public void setPassword(String password) { this.password = password; }
    public void setNickname(String nickname) { this.nickname = nickname; }
}