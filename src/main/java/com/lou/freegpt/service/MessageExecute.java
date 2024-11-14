package com.lou.freegpt.service;

import com.lou.freegpt.vo.MessageVo;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public interface MessageExecute {
    void executeFinishMessage(MessageVo messageVo, String content, String messageId, HttpServletResponse httpServletResponse) throws IOException;

    void executeFinishMessage(MessageVo messageVo, String content,String conversationId, String messageId, HttpServletResponse httpServletResponse) throws IOException;
}
