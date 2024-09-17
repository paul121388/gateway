package org.paul.gateway.client.core;

import lombok.Data;

@Data
public class ApiProperties {
    // 环境和注册中心地址
    private String registerAddress;

    private String env = "dev";
}
