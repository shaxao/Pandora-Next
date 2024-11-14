package com.lou.freegpt.dao;

import com.lou.freegpt.domain.MessageEntity;
import com.lou.freegpt.domain.TitleEntity;
import com.lou.freegpt.vo.MessageVo;

import java.util.List;
import java.util.Map;

public interface MessageDao {
    int deleteConverById(String conversationId);

    int updateTitleById(String conversationId,String title);

    int insertMessage(MessageVo message, String role, String type);

    MessageEntity findById(String id);

    int updateById(String id, Map<String,String> params);

    int insertTitle(String conversationId, String title);

    List<TitleEntity> findTitles(int offset, int limit);

    Map<String,MessageEntity> findConverByTitleId(String id);

}
