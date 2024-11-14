package com.lou.freegpt.domain;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.io.Serializable;

@Data
public class MessageRequest implements Serializable {
    private String message;

    private String model;

}

