package com.lou.freegpt.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
@Document(collection = "message")
public class MessageEntity {
    @Id
    private String id;
    private String conversationId;
    private Message message;
    private String parent;
    private List<String> children;

    @Data
    public static class Message {
        private String id;
        private Author author;
        private String createTime;
        private String updateTime;
        private Content content;
        private String status;
        private boolean endTurn;
        private int weight;
        private Map<String, Object> metadata;
        private String recipient;

        public String getCreateTime() {
            return createTime;
        }

        public void setCreateTime(String time) {
            this.createTime = time;
        }
    }
    @Data
    public static class Author {
        private String role;
        private String name;
        private Map<String, Object> metadata;
    }
    @Data
    public static class Content {
        private String contentType;
        private List<String> parts;
    }
}
