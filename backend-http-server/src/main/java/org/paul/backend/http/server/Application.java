package org.paul.backend.http.server;

import org.paul.gateway.client.core.ApiInvoker;
import org.paul.gateway.client.core.ApiProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class Application {
    @Autowired(required = false)
    private ApiProperties apiProperties;

    @ApiInvoker(path="/http-server/ping")
    @GetMapping("/http-demo/ping")
    public String ping() {return "/http-demo/ping：pong";}

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
