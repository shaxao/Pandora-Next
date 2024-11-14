package com.lou.freegpt.mapper;

import com.lou.freegpt.domain.ChatLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
* @author Administrator
* @description 针对表【chat_log(系统日志表)】的数据库操作Mapper
* @createDate 2024-06-11 16:25:33
* @Entity com.lou.freegpt.domain.ChatLog
*/
@Mapper
public interface ChatLogMapper extends BaseMapper<ChatLog> {

}




