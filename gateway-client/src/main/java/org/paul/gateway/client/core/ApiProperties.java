package org.paul.gateway.client.core;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@ConfigurationProperties(prefix = "api")
public class ApiProperties {
    // 环境和注册中心地址
    private String registerAddress;

    private String env = "dev";
}
