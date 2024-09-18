package org.paul.gateway.client.core;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Data
@ConfigurationProperties(prefix = "api")
@Component
public class ApiProperties {
    // 环境和注册中心地址
    private String registerAddress;

    private String env = "dev";
}
