package com.lou.freegpt.listener;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class PortListener implements ApplicationListener<ApplicationReadyEvent> {

    private final Environment environment;

    public PortListener(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        String port = environment.getProperty("local.server.port");
        System.out.println("请访问: http://localhost:" + port + "/index.html");

    }
}
