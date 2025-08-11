package com.distri.chat.interfaces.web;

import com.distri.chat.domain.user.entity.User;
import com.distri.chat.domain.user.service.UserService;
import com.distri.chat.domain.user.dto.AuthResponse;
import com.distri.chat.domain.user.dto.LoginRequest;
import com.distri.chat.domain.user.dto.RegisterRequest;
import com.distri.chat.shared.dto.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 * 处理用户注册和登录
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "用户认证", description = "用户注册、登录接口")
public class AuthController {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    
    private final UserService userService;
    
    public AuthController(UserService userService) {
        this.userService = userService;
    }
    
    @Operation(summary = "用户注册", description = "通过手机号和密码注册新用户")
    @PostMapping("/register")
    public Result<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        // 用户注册
        User user = userService.register(request.getPhone(), request.getPassword(), request.getNickname());
        
        // 生成设备ID和访问令牌
        String deviceId = userService.generateDeviceId();
        String accessToken = userService.generateAccessToken(user.getId(), deviceId);
        
        // 构造响应
        AuthResponse response = new AuthResponse(
                user.getId(),
                accessToken,
                deviceId,
                user.getNickname(),
                user.getAvatar()
        );
        
        return Result.success("注册成功", response);
    }
    
    @Operation(summary = "用户登录", description = "通过手机号和密码登录")
    @PostMapping("/login")
    public Result<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        // 用户登录
        User user = userService.login(request.getPhone(), request.getPassword());
        
        // 生成或使用设备ID
        String deviceId = request.getDeviceId();
        if (deviceId == null || deviceId.trim().isEmpty()) {
            deviceId = userService.generateDeviceId();
        }
        
        // 生成访问令牌
        String accessToken = userService.generateAccessToken(user.getId(), deviceId);
        
        // 构造响应
        AuthResponse response = new AuthResponse(
                user.getId(),
                accessToken,
                deviceId,
                user.getNickname(),
                user.getAvatar()
        );
        
        return Result.success("登录成功", response);
    }
}
