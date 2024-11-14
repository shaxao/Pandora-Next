package com.lou.freegpt.service;

import lombok.Data;
import lombok.ToString;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Data
@ToString
public class AppConfigService {
    @Value("${app.config.url}")
    private String url;
    @Value("${app.config.apiKey}")
    private String apiKey;
    @Value("${app.config.openTextUri}")
    private String openTextUri;
    @Value("${app.config.imageUri}")
    private String imageUri;
    @Value("${app.audio.openai.hostUrl}")
    private String aduioOaiUrl;
    @Value("${app.audio.openai.tranUri}")
    private String audioOaiTranUri;
    @Value("${app.audio.openai.speeUri}")
    private String audioOaiSpeeUri;
    @Value("${app.audio.openai.apiKey}")
    private String audioOaiApiKey;
    @Value("${app.config.top_p}")
    private Double top_p;
    @Value("${app.config.temperature}")
    private Double temperature;
    @Value("${app.config.presence_penalty}")
    private Double presencePenalty;
    @Value("${app.config.frequency_penalty}")
    private Double frequencyPenalty;
    @Value("${app.config.seed}")
    private String seed;
    @Value("${app.config.max_tokens}")
    private Integer maxTokens;
    @Value("${app.config.stream}")
    private Boolean stream;
    @Value("${turnstile.secret.key}")
    private String secretKey;
}
