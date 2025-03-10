package org.paul.backend.http.server.controller;

import lombok.extern.slf4j.Slf4j;
import org.paul.gateway.client.core.ApiInvoker;
import org.paul.gateway.client.core.ApiProperties;
import org.paul.gateway.client.core.ApiProtocol;
import org.paul.gateway.client.core.ApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@ApiService(serviceId = "backend-http-server", protocol = ApiProtocol.HTTP, patternPath = "/http-server/**")
public class PingController {

    @Autowired
    private ApiProperties apiProperties;

    @ApiInvoker(path = "/http-server/ping")
    @GetMapping("/http-server/ping")
    public String ping() throws InterruptedException {
        log.info("{}", apiProperties);
//        Thread.sleep(50);
        return "pong";
    }

}
