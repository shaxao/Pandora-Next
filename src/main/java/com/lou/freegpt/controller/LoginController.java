package com.lou.freegpt.controller;

import com.lou.freegpt.common.anntation.LogInterface;
import com.lou.freegpt.enums.AjaxResult;
import com.lou.freegpt.enums.BusinessType;
import com.lou.freegpt.service.ChatLogService;
import com.lou.freegpt.service.ChatUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LoginController {
    @Autowired
    private ChatUserService chatUserService;

    @PostMapping("/login")
    @LogInterface(title = "登录", businessType = BusinessType.GRANT, isSaveRequestData = false)
    public AjaxResult login(@RequestParam("username")String username, @RequestParam("password")String password) {
        return chatUserService.login(username, password);
    }
}
