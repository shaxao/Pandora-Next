package com.lou.freegpt.dao.impl;

import cn.hutool.core.collection.CollUtil;
import com.lou.freegpt.dao.MessageDao;
import com.lou.freegpt.domain.MessageEntity;
import com.lou.freegpt.domain.TitleEntity;
import com.lou.freegpt.utils.UserDetailsNow;
import com.lou.freegpt.vo.MessageVo;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class MessageDaoImpl implements MessageDao {
    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public int insertTitle(String conversationId, String title) {
        TitleEntity titleEntity = new TitleEntity();
        titleEntity.setUsername(UserDetailsNow.getUsername());
        titleEntity.setId(conversationId);
        titleEntity.setTitle(title);
        titleEntity.setIsDeleted(0);
        titleEntity.setConversationId(conversationId);
        titleEntity.setCreateTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        return mongoTemplate.insert(titleEntity) != null ? 1 : 0;
    }

    @Override
    public List<TitleEntity> findTitles(int offset, int limit) {
        System.out.println("UserDetailsNow:" + UserDetailsNow.getUsername());
        Query query = new Query();
        query.addCriteria(Criteria.where("username").is(UserDetailsNow.getUsername()));
        query.skip(offset).limit(limit).with(Sort.by(Sort.Order.desc("createTime")));
        List<TitleEntity> titleEntities = mongoTemplate.find(query, TitleEntity.class);
        return CollUtil.isNotEmpty(titleEntities) ? titleEntities : null;
    }

    @Override
    public Map<String,MessageEntity> findConverByTitleId(String id) {
        Query query = new Query();
        query.addCriteria(Criteria.where("conversationId").is(id));
        List<MessageEntity> messageEntities = mongoTemplate.find(query, MessageEntity.class);
        query.with(Sort.by(Sort.Direction.ASC, "message.createTime"));
        Map<String,MessageEntity> messageEntityMap = new HashMap<>();
        if(CollUtil.isEmpty(messageEntities)){
            return null;
        }
        messageEntities.forEach(messageEntity -> {
            messageEntityMap.put(messageEntity.getMessage().getId(), messageEntity);
        });
        return messageEntityMap;
    }

    @Override
    public int deleteConverById(String conversationId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("conversationId").is(conversationId));
        // 删除会话
        DeleteResult remove = mongoTemplate.remove(query, MessageEntity.class);
        // 删除标题
        DeleteResult remove1 = mongoTemplate.remove(query, TitleEntity.class);
        return remove.getDeletedCount() > 0 && remove1.getDeletedCount() > 0 ? 1 : 0;
    }

    @Override
    public int updateTitleById(String conversationId, String title) {
        Query query = new Query();
        query.addCriteria(Criteria.where("conversationId").is(conversationId));
        Update update = new Update().set("title", title);
        UpdateResult updateResult = mongoTemplate.updateFirst(query, update, TitleEntity.class);
        return updateResult.getModifiedCount() > 0 ? 1 : 0;
    }

    @Override
    public int insertMessage(MessageVo messageVo, String role, String type) {
        MessageEntity messageEntity = new MessageEntity();

        messageEntity.setId(messageVo.getMessageId());
        messageEntity.setConversationId(messageVo.getConversationId());

        MessageEntity.Message message = new MessageEntity.Message();
        message.setId(messageVo.getMessageId());

        MessageEntity.Author author = new MessageEntity.Author();
        author.setRole(role);
        author.setName(UserDetailsNow.getUsername());
        author.setMetadata(new HashMap<>());

        message.setAuthor(author);
        String formattedTime = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        message.setCreateTime(formattedTime);
        message.setUpdateTime(null);

        MessageEntity.Content content = new MessageEntity.Content();
        content.setContentType(type);
        content.setParts(Arrays.asList(messageVo.getContent().trim().replaceAll("^\"|\"$", "")));
        message.setContent(content);

        message.setStatus("finished_successfully");
        message.setEndTurn(true);
        message.setWeight(0);

        HashMap<String, Object> metadata = new HashMap<>();
        metadata.put("is_visually_hidden_from_conversation", true);
        message.setMetadata(metadata);

        message.setRecipient("all");

        messageEntity.setMessage(message);
        messageEntity.setParent(messageVo.getParentId());
        messageEntity.setChildren(Collections.emptyList());
        return mongoTemplate.insert(messageEntity) != null ? 1 : 0;
    }

    @Override
    public int updateById(String id, Map<String,String> params) {
        // 创建查询对象
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(id));

        // 创建更新对象
        Update update = new Update();
        params.keySet().forEach(param -> {
            if(params.get(param) != null) {
                update.set(param, param.equals("children") ? Arrays.asList(params.get(param)) : params.get(param));
            }
        });
        // 执行更新操作
        UpdateResult updateResult = mongoTemplate.updateFirst(query, update, MessageEntity.class);
        return (int) updateResult.getModifiedCount() | 0;
    }

    @Override
    public MessageEntity findById(String id) {
        return mongoTemplate.findById(id,MessageEntity.class);
    }

    @Override
    public TitleEntity findTitleById(String conversationId) {
        Query query = new Query(Criteria.where("conversationId").is(conversationId));
        return mongoTemplate.findOne(query, TitleEntity.class);
    }

    @Override
    public int getShareCount(String conversationId) {
        TitleEntity title = findTitleById(conversationId);
        return title != null ? title.getShareCount() : 0;
    }

    @Override
    public void incrementShareCount(String conversationId) {
        Query query = new Query(Criteria.where("conversationId").is(conversationId));
        Update update = new Update().inc("shareCount", 1);
        mongoTemplate.updateFirst(query, update, TitleEntity.class);
    }
}
