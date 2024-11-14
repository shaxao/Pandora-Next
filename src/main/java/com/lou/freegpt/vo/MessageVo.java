package com.lou.freegpt.vo;

import lombok.Data;

@Data
public class MessageVo {

    private String messageId;
    private String parentId;
    private String content;
    private String conversationId;
    private String model;
    private String gizmo;
    private String contentType;
    private Boolean firstFlag;
    private Boolean isSearch;
}
