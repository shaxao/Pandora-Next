package com.lou.freegpt.vo;

import lombok.Data;

@Data
public class TitleVo {
    private String messageId;
    private String conversationId;
    private Boolean isFirstFlag;
    private String apiKey;
    private String baseUrl;
    private Boolean isWeb;
    private String content;
}
