package com.lou.freegpt.service;


import com.lou.freegpt.enums.AjaxResult;
import com.lou.freegpt.vo.MessageVo;
import com.lou.freegpt.vo.TitleVo;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

public interface ChatService {
    String processMessage(String message, String model, HttpServletResponse httpServletResponse, String fileEncoder);

    String imageGenera(MessageVo messageVo, String globalSetJson);

    String genTitle(TitleVo titleVo);

    AjaxResult convertAudioToText(MultipartFile audioFile);
}
