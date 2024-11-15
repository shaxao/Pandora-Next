package com.lou.freegpt.domain;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "title")
public class TitleEntity {
    @Id
    private String id;
    private String username;
    private String title;
    private String conversationId;
    private Integer isDeleted;
    private String createTime;
    private int shareCount = 0;
    private boolean isPublic = false;
}
