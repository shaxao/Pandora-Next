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
        if (text == null || text.isEmpty()) {
            return sections;
        }

        // 处理转义字符
        text = text.replace("\\n", "\n")
                   .replace("\\s", " ")
                   .replace("\\t", "\t")
                   .replace("\\\"", "\"")
                   .replace("\\\\", "\\");

        StringBuilder currentContent = new StringBuilder();
        boolean isInCodeBlock = false;
        String currentLanguage = "";

        String[] lines = text.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            
            if (line.trim().startsWith("```")) {
                if (!isInCodeBlock) {
                    // 开始新的代码块
                    if (currentContent.length() > 0) {
                        // 保存之前的普通文本
                        sections.add(new MessageSection(false, null, currentContent.toString().trim()));
                        currentContent = new StringBuilder();
                    }
                    // 获取语言类型
                    currentLanguage = line.trim().length() > 3 ? line.trim().substring(3).trim() : "";
                    isInCodeBlock = true;
                } else {
                    // 结束代码块，确保代码块内容不包含结束标记
                    String codeContent = currentContent.toString().trim();
                    if (!codeContent.isEmpty()) {
                        sections.add(new MessageSection(true, currentLanguage, codeContent));
                    }
                    currentContent = new StringBuilder();
                    isInCodeBlock = false;
                    currentLanguage = "";
                }
            } else {
                // 处理普通行
                if (currentContent.length() > 0) {
                    currentContent.append("\n");
                }
                // 如果是代码块的第一行，不要添加额外的换行
                if (isInCodeBlock || !line.trim().isEmpty()) {
                    currentContent.append(line);
                }
            }
        }

        // 处理最后剩余的内容
        if (currentContent.length() > 0) {
            String finalContent = currentContent.toString();
            if (!finalContent.trim().isEmpty()) {
                sections.add(new MessageSection(isInCodeBlock, currentLanguage, finalContent.trim()));
            }
        }

        return sections;
    }

    @Data
    @AllArgsConstructor
    public class MessageSection {
        private boolean isCode;
        private String language;
        private String content;
        private String timestamp;

        public MessageSection(boolean isCode, String language, String content) {
            this.isCode = isCode;
            this.language = language;
            this.content = content;
            this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
    }
}
