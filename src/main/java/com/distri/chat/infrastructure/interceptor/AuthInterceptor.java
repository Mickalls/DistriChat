package com.distri.chat.infrastructure.interceptor;

import com.distri.chat.shared.context.UserContext;
import com.distri.chat.shared.dto.Result;
import com.distri.chat.shared.exception.BusinessException;
import com.distri.chat.shared.utils.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

/**
 * 登录认证拦截器
 * 负责解析JWT token并设置用户上下文
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(AuthInterceptor.class);

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    // JWT token的Header名称
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    public AuthInterceptor(JwtUtil jwtUtil, ObjectMapper objectMapper) {
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestUri = request.getRequestURI();
        String method = request.getMethod();
        
        logger.debug("拦截器处理请求: {} {}", method, requestUri);

        try {
            // 提取JWT token
            String token = extractToken(request);
            
            if (!StringUtils.hasText(token)) {
                logger.warn("请求缺少Authorization头: {} {}", method, requestUri);
                return handleAuthError(response, "缺少访问令牌");
            }

            // 解析token并设置用户上下文
            JwtUtil.JwtClaims claims = jwtUtil.parseToken(token);
            UserContext.setUser(claims.getUserId(), claims.getDeviceId());
            
            logger.debug("用户认证成功: userId={}, deviceId={}", claims.getUserId(), claims.getDeviceId());
            return true;

        } catch (BusinessException e) {
            logger.warn("用户认证失败: {} - {}", requestUri, e.getMessage());
            return handleAuthError(response, e.getMessage());
        } catch (Exception e) {
            logger.error("拦截器处理异常: {}", requestUri, e);
            return handleAuthError(response, "认证服务异常");
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 清除用户上下文，防止内存泄漏
        UserContext.clear();
        logger.debug("清除用户上下文: {}", request.getRequestURI());
    }

    /**
     * 从请求中提取JWT token
     * 
     * @param request HTTP请求
     * @return JWT token字符串，如果没有则返回null
     */
    private String extractToken(HttpServletRequest request) {
        // 1. 优先从Authorization header获取
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(authHeader) && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length()).trim();
        }

        // 2. 备选方案：从query parameter获取（用于WebSocket等特殊场景）
        String tokenParam = request.getParameter("token");
        if (StringUtils.hasText(tokenParam)) {
            return tokenParam.trim();
        }

        return null;
    }

    /**
     * 处理认证错误，返回统一格式的错误响应
     * 
     * @param response HTTP响应
     * @param message 错误消息
     * @return false 阻止请求继续处理
     */
    private boolean handleAuthError(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        Result<Object> result = Result.error(401, message);
        String jsonResponse = objectMapper.writeValueAsString(result);
        
        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
        
        return false;
    }
}
