package com.lou.freegpt.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

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

    private List<MessageSection> parsedSections;
    // ...

    public List<MessageSection> getParsedSections() {
        if (parsedSections == null) {
            parsedSections = parseMessageSections(getMessage().getContent().getParts().get(0));
        }
        return parsedSections;
    }

    private List<MessageSection> parseMessageSections(String text) {
        List<MessageSection> sections = new ArrayList<>();
        String[] lines = text.split("(```)");
        boolean isCode = false;
        StringBuilder currentContent = new StringBuilder();
        String language = "";

        for (String line : lines) {
            if (line.equals("```")) {
                if (isCode) {
                    String content = currentContent.toString();
                    content = content.replace("\\s", " ").replace("\\n", "\n");
                    sections.add(new MessageSection(true, language, content));
                    currentContent.setLength(0);
                    isCode = false;
                } else {
                    if (currentContent.length() > 0) {
                        String content = currentContent.toString();
                        content = content.replace("\\s", " ").replace("\\n", "\n");
                        sections.add(new MessageSection(false, null, content));
                        currentContent.setLength(0);
                    }
                    isCode = true;
                }
            } else {
                if (isCode) {
                    if (language.isEmpty()) {
                        language = line.trim();
                    } else {
                        currentContent.append(line).append("\n");
                    }
                } else {
                    currentContent.append(line);
                }
            }
        }

        if (currentContent.length() > 0) {
            String content = currentContent.toString();
            content = content.replace("\\s", " ").replace("\\n", "\n");
            sections.add(new MessageSection(isCode, language, content));
        }

        return sections;
    }

    @Data
    @AllArgsConstructor
    public class MessageSection {
        private boolean isCode;
        private String language;
        private String content;

        // Constructor, getters, and setters
        // ...
    }
}
