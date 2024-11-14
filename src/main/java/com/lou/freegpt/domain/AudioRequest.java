package com.lou.freegpt.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AudioRequest {
    private String message;
    private String selectVoice;
}
