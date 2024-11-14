package com.lou.freegpt.domain;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

@Data
@Document(collection = "title")
public class TitleEntity {
    @Id
    private String id;
    private String username;
    private String conversationId;
    private String title;
    private Integer isDeleted;
    private String updateTime;
    private String createTime;
}
