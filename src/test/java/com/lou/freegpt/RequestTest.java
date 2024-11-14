package com.lou.freegpt;

import com.lou.freegpt.service.AppConfigService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@SpringBootTest
public class RequestTest {
    @Autowired
    private AppConfigService appConfigService;

    @Test
    void verTest() {
        RestTemplate restTemplate = new RestTemplate();
        String url = "https://challenges.cloudflare.com/turnstile/v0/siteverify";
        String turnstileResponse = "0._mazuFmTGjqQalQozAH8RhuUxk77J7DE_NPzcHEDLyt_oIsnzSvavoMqq-c9RO88wT8k5Wrv0MyM0-vBzgQtxJfmPexlfdny8dP9bCLm-SpIU5Fl0zkdzCHizN8j6iKyuppg9hiZEnx7dckAPEEujSfU1Xgu_SbwcB5zyNI8Ue91L6i9EmkP6MnJUqACmJbkXmlzdOpBT9IGhdzcvkjw13q--R_tUkd966tcHHMWfxxfiV9FJbS2j176MgNJZJpXbAZTtFw8RRxM5dNFvqhD6XE1R-7Ad8xyQPjanOr8CIxQpwWLvX4wVJ06Bphq2HiCgAe7KH8y0RM3OSBshZu9e2YCz7e6OaDBco5aA4jZnm8MY1t9PbJ6pUFCVuqSPyoKY5C6S8ir_JWr7RxHct_Yj_AZ4DRe6TevOGnBlLM27is2-W3jdX4lYJc4gI4izegI.FK_egQVv32a6XLb7CqMGOg.7b95e4c368e15f117eeb1775e57eeb40073da1ef0d54783997f7eab8647b8f64";
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("secret", appConfigService.getSecretKey());
        requestBody.put("response", turnstileResponse);

        Map<String, Object> response = restTemplate.postForObject(url, requestBody, Map.class);

        boolean success = (Boolean) response.get("success");
        System.out.println("result:" + success);
    }


}
