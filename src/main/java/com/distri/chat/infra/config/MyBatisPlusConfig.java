package com.distri.chat.infra.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus配置类
 */
@Configuration
@MapperScan(basePackages = {
        "com.distri.chat.domain.*.dao",           // 扫描所有领域的dao包
})
public class MyBatisPlusConfig {
    // 暂时禁用配置进行调试
}
