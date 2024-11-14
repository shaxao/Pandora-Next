package com.lou.freegpt.domain;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "session")
public class Session {
    @Id
    private String id;
    private String title;
}
