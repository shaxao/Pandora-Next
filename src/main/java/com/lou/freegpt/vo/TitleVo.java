package com.lou.freegpt.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


public class  TitleVo {
    private String messageId;
    private String conversationId;
    private Boolean firstFlag;
    private String apiKey;
    private String baseUrl;
    private Boolean web;
    private String content;

    public TitleVo() {
    }

    public TitleVo(String messageId, String conversationId, Boolean firstFlag, String apiKey, String baseUrl, Boolean web, String content) {
        this.messageId = messageId;
        this.conversationId = conversationId;
        this.firstFlag = firstFlag;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.web = web;
        this.content = content;
    }

    @Override
    public String toString() {
        return "TitleVo{" +
                "messageId='" + messageId + '\'' +
                ", conversationId='" + conversationId + '\'' +
                ", firstFlag=" + firstFlag +
                ", apiKey='" + apiKey + '\'' +
                ", baseUrl='" + baseUrl + '\'' +
                ", Web=" + web +
                ", content='" + content + '\'' +
                '}';
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public Boolean getFirstFlag() {
        return firstFlag;
    }

    public void setFirstFlag(Boolean firstFlag) {
        this.firstFlag = firstFlag;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Boolean getWeb() {
        return web;
    }

    public void setWeb(Boolean web) {
        this.web = web;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
