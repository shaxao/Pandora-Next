package com.lou.freegpt.service;

import jakarta.servlet.http.HttpServletResponse;

public interface AliChatService {
    public void textMessage(String model, String prompt, HttpServletResponse response,String globalSetJson);
}
