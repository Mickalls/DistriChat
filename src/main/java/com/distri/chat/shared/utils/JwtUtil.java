package com.distri.chat.shared.utils;

import com.distri.chat.shared.exception.BusinessException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

/**
 * JWT工具类
 * 负责JWT token的生成、解析和验证
 */
@Component
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    // JWT密钥 - 从配置文件读取，如果没有配置则使用默认值
    @Value("${jwt.secret:distriChat2025SecretKeyForJwtTokenGeneration}")
    private String jwtSecret;

    // JWT过期时间（小时）
    @Value("${jwt.expiration-hours:24}")
    private int jwtExpirationHours;

    /**
     * 生成JWT token
     * 
     * @param userId 用户ID
     * @param deviceId 设备ID
     * @return JWT token字符串
     */
    public String generateToken(Long userId, String deviceId) {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime expiration = now.plusHours(jwtExpirationHours);
            
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
            
            return Jwts.builder()
                    .subject(userId.toString())                    // 用户ID作为subject
                    .claim("user_id", userId)                      // 用户ID
                    .claim("device_id", deviceId)                  // 设备ID
                    .claim("type", "access_token")                 // token类型
                    .id(UUID.randomUUID().toString())              // token唯一标识
                    .issuedAt(Date.from(now.atZone(ZoneId.systemDefault()).toInstant()))
                    .expiration(Date.from(expiration.atZone(ZoneId.systemDefault()).toInstant()))
                    .signWith(key, Jwts.SIG.HS256)
                    .compact();
                    
        } catch (Exception e) {
            logger.error("生成JWT token失败", e);
            throw BusinessException.business("生成访问令牌失败");
        }
    }

    /**
     * 解析JWT token
     * 
     * @param token JWT token字符串
     * @return JwtClaims 包含用户信息的对象
     */
    public JwtClaims parseToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
            
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Long userId = claims.get("user_id", Long.class);
            String deviceId = claims.get("device_id", String.class);
            
            if (userId == null || deviceId == null) {
                throw BusinessException.unauthorized("token格式错误");
            }

            return new JwtClaims(userId, deviceId, claims.getId(), claims.getExpiration());
            
        } catch (ExpiredJwtException e) {
            logger.warn("JWT token已过期: {}", e.getMessage());
            throw BusinessException.unauthorized("访问令牌已过期，请重新登录");
        } catch (UnsupportedJwtException e) {
            logger.warn("不支持的JWT token: {}", e.getMessage());
            throw BusinessException.unauthorized("不支持的访问令牌格式");
        } catch (MalformedJwtException e) {
            logger.warn("JWT token格式错误: {}", e.getMessage());
            throw BusinessException.unauthorized("访问令牌格式错误");
        } catch (SecurityException e) {
            logger.warn("JWT token签名验证失败: {}", e.getMessage());
            throw BusinessException.unauthorized("访问令牌签名验证失败");
        } catch (IllegalArgumentException e) {
            logger.warn("JWT token为空或无效: {}", e.getMessage());
            throw BusinessException.unauthorized("访问令牌无效");
        } catch (Exception e) {
            logger.error("解析JWT token失败", e);
            throw BusinessException.unauthorized("访问令牌解析失败");
        }
    }

    /**
     * 验证token是否有效（不抛出异常版本）
     * 
     * @param token JWT token字符串
     * @return true-有效, false-无效
     */
    public boolean isTokenValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 从token中提取用户ID
     * 
     * @param token JWT token字符串
     * @return 用户ID
     */
    public Long extractUserId(String token) {
        return parseToken(token).getUserId();
    }

    /**
     * 从token中提取设备ID
     * 
     * @param token JWT token字符串
     * @return 设备ID
     */
    public String extractDeviceId(String token) {
        return parseToken(token).getDeviceId();
    }

    /**
     * JWT Claims封装类
     */
    public static class JwtClaims {
        private final Long userId;
        private final String deviceId;
        private final String jwtId;
        private final Date expiration;

        public JwtClaims(Long userId, String deviceId, String jwtId, Date expiration) {
            this.userId = userId;
            this.deviceId = deviceId;
            this.jwtId = jwtId;
            this.expiration = expiration;
        }

        public Long getUserId() { return userId; }
        public String getDeviceId() { return deviceId; }
        public String getJwtId() { return jwtId; }
        public Date getExpiration() { return expiration; }

        @Override
        public String toString() {
            return "JwtClaims{" +
                    "userId=" + userId +
                    ", deviceId='" + deviceId + '\'' +
                    ", jwtId='" + jwtId + '\'' +
                    ", expiration=" + expiration +
                    '}';
        }
    }
}
