package com.lou.freegpt.enums;

import java.util.PrimitiveIterator;

public enum ModelEnum {
    DALLE("dall-e-3"),

    OPENAI_BASE("gpt"),

    GPT_VERSION("gpt-4-vision-preview"),

    CHAT_GLM_BASE("chatglm"),

    CLAUDE_BASE("claude"),

    GEMINI_BASE("gemini"),

    QWEN_BASE("qwen"),

    PERSON_ONE("GPT4 Turbo"),

    PERSON_TWO("Claude 3 (Opus)"),

    LINK_MJ("midjourney"),

    STABLE_DIFFUSION("stable-diffusion");



    private String models;

    ModelEnum(String models) {
        this.models = models;
    }

    public String getModels(){
        return models;
    }
}
