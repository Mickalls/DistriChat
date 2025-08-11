package com.distri.chat.infrastructure.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus配置类
 */
@Configuration
@MapperScan("com.distri.chat.infrastructure.persistence.mapper")
public class MyBatisPlusConfig {
    // 暂时禁用配置进行调试
}
