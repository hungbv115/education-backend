package com.education.model.dto;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Class tạo đối tượng lấy dữ liệu từ file cấu hình
 */
@ConfigurationProperties(prefix = "jwt")
@Configuration
@Data
public class JwtConfig {

    private String hmacSecret;
    private String subject;
    private String id;
    private String issuer;
    private int timeToLive;
}
