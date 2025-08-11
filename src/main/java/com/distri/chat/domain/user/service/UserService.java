package com.distri.chat.domain.user.service;

import com.distri.chat.domain.user.entity.User;
import com.distri.chat.infrastructure.persistence.mapper.UserMapper;
import com.distri.chat.shared.exception.BusinessException;
import com.distri.chat.shared.utils.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 用户领域服务
 * 负责用户注册、登录、认证等核心业务逻辑
 */
@Service
public class UserService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    
        private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public UserService(UserMapper userMapper, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }
    
    /**
     * 用户注册
     */
    public User register(String phone, String password, String nickname) {
        // 检查手机号是否已存在
        User existingUser = userMapper.findByPhone(phone);
        if (existingUser != null) {
            throw BusinessException.badRequest("手机号已被注册");
        }
        
        // 加密密码
        String encodedPassword = passwordEncoder.encode(password);
        
        // 创建用户
        User user = new User(phone, encodedPassword, nickname);
        
        // 保存到数据库
        userMapper.insert(user);
        
        logger.info("用户注册成功：手机号={}, 用户ID={}", phone, user.getId());
        return user;
    }
    
    /**
     * 用户登录
     */
    public User login(String phone, String password) {
        // 查找用户
        User user = userMapper.findByPhone(phone);
        if (user == null) {
            throw BusinessException.badRequest("手机号未注册");
        }
        
        // 验证密码
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw BusinessException.badRequest("密码错误");
        }
        
        // 检查用户状态
        if (!"ACTIVE".equals(user.getStatus())) {
            throw BusinessException.forbidden("用户已被禁用");
        }
        
        // 更新最后登录时间
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        
        logger.info("用户登录成功：手机号={}, 用户ID={}", phone, user.getId());
        return user;
    }
    
    /**
     * 根据ID查找用户
     */
    public User findById(Long userId) {
        return userMapper.selectById(userId);
    }
    
    /**
     * 生成设备ID
     */
    public String generateDeviceId() {
        return "device_" + UUID.randomUUID().toString().replace("-", "");
    }
    
    /**
     * 生成JWT访问令牌
     * 
     * @param userId 用户ID
     * @param deviceId 设备ID
     * @return JWT token字符串
     */
    public String generateAccessToken(Long userId, String deviceId) {
        return jwtUtil.generateToken(userId, deviceId);
    }
}
