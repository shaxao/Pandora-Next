package com.lou.freegpt.controller;

import com.lou.freegpt.dao.MessageDao;
import com.lou.freegpt.domain.MessageEntity;
import com.lou.freegpt.domain.TitleEntity;
import lombok.extern.log4j.Log4j;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;
import java.util.Base64;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;
import java.net.URL;
import java.net.HttpURLConnection;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;

@Controller
@Log4j2
public class PageController {

    @Autowired
    private MessageDao messageDao;

    @Value("${chat.avatar.user-path}")
    private String userPath;

    @Value("${chat.avatar.system-path}")
    private String systemPath;

    @Value("${chat.system.name}")
    private String systemName;

    @GetMapping(value = "/mini-coi.js", produces = "application/javascript")
    public String getMiniCoiScript(Model model) {
        return "mini-coi.js"; // 渲染 templates/mini-coi.js
    }

    @RequestMapping("/{page}")
    public String page(@PathVariable String page) {
        log.info("跳转页面:{}", page);
        return page;
    }

    /**
     * 将图片转换为base64
     */
    private String imageToBase64(String imagePath) {
        try {
            byte[] imageBytes;

            if (imagePath.startsWith("http")) {
                // 处理网络图片
                URL url = new URL(imagePath);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);

                try (InputStream in = conn.getInputStream();
                     ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = in.read(buffer)) != -1) {
                        out.write(buffer, 0, len);
                    }
                    imageBytes = out.toByteArray();
                }
            } else {
                // 处理本地图片
                imageBytes = Files.readAllBytes(Paths.get(imagePath));
            }

            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            String imageType = imagePath.toLowerCase().endsWith(".png") ? "png" : "jpeg";
            return "data:image/" + imageType + ";base64," + base64Image;

        } catch (Exception e) {
            // 如果转换失败，返回一个默认的头像base64
            return "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAA..."; // 添加默认头像的base64
        }
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

        // 转换图片为base64
        String userImageBase64 = imageToBase64(userPath);
        String systemImageBase64 = imageToBase64(systemPath);

        // 使用base64图片
        model.addAttribute("messageEntityMap", messageEntityMap);
        model.addAttribute("userPath", userImageBase64);
        model.addAttribute("systemPath", systemImageBase64);
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
