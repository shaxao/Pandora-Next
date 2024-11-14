package com.lou.freegpt.service.impl;

import com.lou.freegpt.service.AliChatService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;

@Service
public class AliChatServiceImpl implements AliChatService {

    @Override
    public void textMessage(String model, String prompt, HttpServletResponse response,String globalSetJson) {

    }
}
