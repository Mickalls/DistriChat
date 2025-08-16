package com.distri.chat.infra.config;

import com.distri.chat.infra.interceptor.AuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置类
 * 配置拦截器、CORS等web相关设置
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    public WebConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")                    // 拦截所有请求
                .excludePathPatterns(
                        // 认证相关接口
                        "/api/auth/register",               // 用户注册
                        "/api/auth/login",                  // 用户登录

                        // 健康检查接口
                        "/api/health/**",                   // 健康检查相关

                        // API文档接口
                        "/swagger-ui/**",                   // Swagger UI
                        "/swagger-ui.html",                 // Swagger UI入口
                        "/v3/api-docs/**",                  // OpenAPI文档
                        "/swagger-resources/**",            // Swagger资源
                        "/webjars/**",                      // WebJars静态资源

                        // 静态资源
                        "/css/**",                          // CSS文件
                        "/js/**",                           // JavaScript文件
                        "/images/**",                       // 图片文件
                        "/favicon.ico",                     // 网站图标

                        // 错误页面
                        "/error",                           // Spring Boot默认错误页面

                        // WebSocket握手 (这里不拦截，在WebSocket层面进行认证)
                        "/ws/**"                            // WebSocket连接路径
                );
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")                // 允许所有来源（生产环境应该限制具体域名）
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);                            // 预检请求缓存时间（秒）
    }

}
