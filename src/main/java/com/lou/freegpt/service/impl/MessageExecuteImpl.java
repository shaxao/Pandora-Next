package com.lou.freegpt.service.impl;

import cn.hutool.json.JSONObject;
import com.lou.freegpt.dao.MessageDao;
import com.lou.freegpt.vo.MessageVo;
import com.lou.freegpt.service.MessageExecute;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Service
public class MessageExecuteImpl implements MessageExecute {
    @Autowired
    private MessageDao messageDao;
    @Override
    public void executeFinishMessage(MessageVo messageVo, String content, String conversationId, String messageId, HttpServletResponse httpServletResponse) throws IOException {
        MessageVo messageVo1 = new MessageVo();
        messageVo1.setParentId(messageVo.getMessageId());
        messageVo1.setModel(messageVo.getModel());
        messageVo1.setContent(content.trim());
        messageVo1.setConversationId(messageVo.getConversationId());
        messageVo1.setMessageId(messageId);
        messageDao.insertMessage(messageVo1, "system", "text");
        // 修改父消息的children
        Map<String,String> params = new HashMap<>();
        params.put("message.updateTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        params.put("children", messageVo1.getMessageId());
        messageDao.updateById(messageVo.getMessageId(), params);
        httpServletResponse.setCharacterEncoding("UTF-8"); // 设置字符编码为UTF-8
        httpServletResponse.setContentType("application/json; charset=UTF-8"); // 设置内容类型为JSON，并指定字符编码
        JSONObject jsonObject = new JSONObject();
        jsonObject.putOnce("messageId", messageId);
        jsonObject.putOnce("conversationId", conversationId);
        String response = jsonObject.toString();
        httpServletResponse.getWriter().write("data:" + response + "\n\n");
        httpServletResponse.flushBuffer();
    }


    @Override
    public void executeFinishMessage(MessageVo messageVo, String content, String messageId, HttpServletResponse httpServletResponse) throws IOException {
        MessageVo messageVo1 = new MessageVo();
        messageVo1.setParentId(messageVo.getMessageId());
        messageVo1.setModel(messageVo.getModel());
        messageVo1.setContent(content.trim());
        messageVo1.setConversationId(messageVo.getConversationId());
        messageVo1.setMessageId(messageId);
        messageDao.insertMessage(messageVo1, "system", "text");
        // 修改父消息的children
        Map<String,String> params = new HashMap<>();
        params.put("message.updateTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        params.put("children", messageVo1.getMessageId());
        messageDao.updateById(messageVo.getMessageId(), params);
        httpServletResponse.setCharacterEncoding("UTF-8"); // 设置字符编码为UTF-8
        httpServletResponse.setContentType("application/json; charset=UTF-8"); // 设置内容类型为JSON，并指定字符编码
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("messageId", messageId);
        jsonObject.put("isFirstFlag", messageVo.getFirstFlag());
        String respongse = jsonObject.toString();
        httpServletResponse.getWriter().write("data:" + respongse + "\n\n");
        httpServletResponse.flushBuffer();
    }
}
