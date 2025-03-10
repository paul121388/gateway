package org.paul.common.constants;

public interface GatewayProtocol {

    String HTTP = "http";

    String DUBBO = "dubbo";

    static boolean isHttp(String protocol) {
        return HTTP.equals(protocol);
    }

    static boolean isDubbo(String protocol) {
        return DUBBO.equals(protocol);
    }

}