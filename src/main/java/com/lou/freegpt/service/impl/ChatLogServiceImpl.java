package com.lou.freegpt.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lou.freegpt.domain.ChatLog;
import com.lou.freegpt.service.ChatLogService;
import com.lou.freegpt.mapper.ChatLogMapper;
import org.springframework.stereotype.Service;

/**
* @author Administrator
* @description 针对表【chat_log(系统日志表)】的数据库操作Service实现
* @createDate 2024-06-11 16:25:33
*/
@Service
public class ChatLogServiceImpl extends ServiceImpl<ChatLogMapper, ChatLog>
    implements ChatLogService{

}




