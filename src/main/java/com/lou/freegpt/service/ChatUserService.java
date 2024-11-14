package com.lou.freegpt.service;

import com.lou.freegpt.domain.ChatUser;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lou.freegpt.enums.AjaxResult;
import com.lou.freegpt.vo.RegisterVo;
import lombok.NonNull;

/**
* @author Administrator
* @description 针对表【chat_user(聊天应用用户表)】的数据库操作Service
* @createDate 2024-05-30 09:08:32
*/
public interface ChatUserService extends IService<ChatUser> {

    int register(@NonNull RegisterVo registerVo);

    AjaxResult sendCode(String email);

    AjaxResult login(String username, String password);
}
