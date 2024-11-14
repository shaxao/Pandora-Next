package com.lou.freegpt.controller;

import com.lou.freegpt.dao.MessageDao;
import com.lou.freegpt.domain.MessageEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

@Controller
public class PageController {

    @Autowired
    private MessageDao messageDao;

    @RequestMapping("/{page}")
    public String page(@PathVariable String page){
        return page;
    }

    /**
     * 分享页面
     */
    @GetMapping("/share/{conversationId}")
    public String shareTalk(@PathVariable String conversationId, Model model){
        Map<String, MessageEntity> messageEntityMap = messageDao.findConverByTitleId(conversationId);
        model.addAttribute("messageEntityMap", messageEntityMap);
        return "share";
    }
}
