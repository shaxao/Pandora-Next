package com.lou.freegpt.utils;

import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * 验证工具类
 */
public class VerUtils {

    public static boolean turnstile(String turnstile, String secretKey){
        RestTemplate restTemplate = new RestTemplate();
        String url = "https://challenges.cloudflare.com/turnstile/v0/siteverify";
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("secret", secretKey);
        requestBody.put("response", turnstile);

        Map<String, Object> response = restTemplate.postForObject(url, requestBody, Map.class);

        boolean success = (Boolean) response.get("success");
        return success;
    }
}
