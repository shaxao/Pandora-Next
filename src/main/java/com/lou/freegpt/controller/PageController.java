package com.lou.freegpt.controller;

import com.lou.freegpt.dao.MessageDao;
import com.lou.freegpt.domain.MessageEntity;
import com.lou.freegpt.domain.TitleEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

@Controller
public class PageController {

    @Autowired
    private MessageDao messageDao;

    @Value("${chat.avatar.user-path}")
    private String userPath;

    @Value("${chat.avatar.system-path}")
    private String systemPath;

    @Value("${chat.system.name}")
    private String systemName;


    @RequestMapping("/{page}")
    public String page(@PathVariable String page) {
        return page;
    }


    /**
     * 分享页面
     */
    @GetMapping("/share/{conversationId}")
    public String shareTalk(@PathVariable String conversationId, Model model) {
        // 获取对话内容
        Map<String, MessageEntity> messageEntityMap = messageDao.findConverByTitleId(conversationId);
        if (messageEntityMap == null || messageEntityMap.isEmpty()) {
            return "error/404"; // 返回404页面
        }

        // 获取对话标题
        TitleEntity titleEntity = messageDao.findTitleById(conversationId);

        // 添加必要的数据到模型
        model.addAttribute("messageEntityMap", messageEntityMap);
        model.addAttribute("userPath", userPath);
        model.addAttribute("systemPath", systemPath);
        model.addAttribute("systemName", systemName);
        model.addAttribute("title", titleEntity != null ? titleEntity.getTitle() : "AI对话分享");
        model.addAttribute("shareTime", titleEntity != null ? titleEntity.getCreateTime() : "");

        return "share";
    }

    /**
     * 导出PDF
     */
    @GetMapping("/share/{conversationId}/pdf")
    public String exportPDF(@PathVariable String conversationId, Model model) {
        // 复用shareTalk的逻辑，但使用不同的视图模板
        shareTalk(conversationId, model);
        return "pdf-export";
    }
}
