package com.lou.freegpt;

import cn.hutool.json.JSONException;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.lou.freegpt.dao.MessageDao;
import com.lou.freegpt.domain.MessageEntity;
import com.lou.freegpt.domain.TitleEntity;
import com.lou.freegpt.utils.RequestUtils;
import okhttp3.*;
import org.apache.commons.lang3.StringEscapeUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.io.IOException;
import java.util.*;

@SpringBootTest
class FreeGptApplicationTests {
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private MessageDao messageDao;

    @Test
    void contextLoads() {
        MessageEntity messageEntity = new MessageEntity();

        messageEntity.setId("e0536ebf-5dee-4538-acf4-f671e69d45bf");

        MessageEntity.Message message = new MessageEntity.Message();
        message.setId("e0536ebf-5dee-4538-acf4-f671e69d45bf");

        MessageEntity.Author author = new MessageEntity.Author();
        author.setRole("system");
        author.setName(null);
        author.setMetadata(new HashMap<>());

        message.setAuthor(author);
        message.setCreateTime(null);
        message.setUpdateTime(null);

        MessageEntity.Content content = new MessageEntity.Content();
        content.setContentType("text");
        content.setParts(Arrays.asList(""));
        message.setContent(content);

        message.setStatus("finished_successfully");
        message.setEndTurn(true);
        message.setWeight(0);

        HashMap<String, Object> metadata = new HashMap<>();
        metadata.put("is_visually_hidden_from_conversation", true);
        message.setMetadata(metadata);

        message.setRecipient("all");

        messageEntity.setMessage(message);
        messageEntity.setParent("6809d129-9118-41ff-a060-563b4ae61031");
        messageEntity.setChildren(Arrays.asList("aba7cfe5-b56b-43e0-a8eb-8279412677f2"));

        // 使用MongoTemplate插入数据
        mongoTemplate.insert(messageEntity);
    }

    @Test
    void testMessage() {
        MessageEntity messageEntity = mongoTemplate.findById("9c370915-1ad5-4228-8ab4-7980bfb070e2", MessageEntity.class);
        Map<String,MessageEntity> map = new HashMap<>();
        map.put(messageEntity.getId(),messageEntity);
        String string = JSONUtil.parse(map).toString();
        System.out.println(string);
    }

    @Test
    void testUuid() {
        String string = UUID.randomUUID().toString();
        System.out.println(string);
    }

    @Test
    void testPost() {
        //String params = "{\r\n  \"model\": \"gpt-3.5-turbo\",\r\n  \"messages\": [\r\n    {\r\n      \"role\": \"system\",\r\n      \"content\": \"你是一个经过优秀训练并且具备思考能力的人工智能大模型，熟悉各种语言语法以及典故，能够将长文本总结为精练的短文本\"\r\n    },\r\n    {\r\n      \"role\": \"user\",\r\n      \"content\": \"将下述文本根据文本语言总结成一个8个长度的文本: 西红柿炒钢丝\"这个说法听起来像是一种玩笑或误解，因为“钢丝”实际上并不是可以食用的。如果你是指\"西红柿炒鸡蛋\"这道常见的中式菜肴，这是一种非常受欢迎的家常菜，主要材料是西红柿和鸡蛋，通常会加入少许盐和葱来增味。如果有别的意思或者特别的食谱，也欢迎你进一步说明！\"\r\n    }\r\n  ],\r\n  \"stream\": false,\r\n  \"temperature\": 0.5,\r\n  \"top_p\": 1.0,\r\n  \"max_tokens\": 4000\r\n}";
        String content = "西红柿炒钢丝\"这个说法听起来像是一种玩笑或误解，因为“钢丝”实际上并不是可以食用的。如果你是指\"西红柿炒鸡蛋\"这道常见的中式菜肴，这是一种非常受欢迎的家常菜，主要材料是西红柿和鸡蛋，通常会加入少许盐和葱来增味。如果有别的意思或者特别的食谱，也欢迎你进一步说明！";
        String message = StringEscapeUtils.escapeHtml4(content);
        String escapedContent = message.replace("\n", "").replace("\"", "\\\"");
        String params = "{\r\n" +
                "  \"model\": \"gpt-3.5-turbo\",\r\n" +
                "  \"messages\": [\r\n" +
                "    {\r\n" +
                "      \"role\": \"system\",\r\n" +
                "      \"content\": \"你是一个经过优秀训练并且具备思考能力的人工智能大模型，熟悉各种语言语法以及典故，能够将长文本总结为精练的短文本\"\r\n" +
                "    },\r\n" +
                "    {\r\n" +
                "      \"role\": \"user\",\r\n" +
                "      \"content\": \"将下述文本根据文本语言总结成一个8个长度的文本: " + escapedContent + "\"\r\n" +
                "    }\r\n" +
                "  ],\r\n" +
                "  \"stream\": false,\r\n" +
                "  \"temperature\": 0.5,\r\n" +
                "  \"top_p\": 1.0,\r\n" +
                "  \"max_tokens\": 4000\r\n" +
                "}";
        String key = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6Ik1UaEVOVUpHTkVNMVFURTRNMEZCTWpkQ05UZzVNRFUxUlRVd1FVSkRNRU13UmtGRVFrRXpSZyJ9.eyJodHRwczovL2FwaS5vcGVuYWkuY29tL3Byb2ZpbGUiOnsiZW1haWwiOiJnYWJ5Y29zdGVhLjAxQGdtYWlsLmNvbSIsImVtYWlsX3ZlcmlmaWVkIjp0cnVlfSwiaHR0cHM6Ly9hcGkub3BlbmFpLmNvbS9hdXRoIjp7InBvaWQiOiJvcmctd0NiaTJYYkdyUEtvNkpUbVRyZGlZNlZMIiwidXNlcl9pZCI6InVzZXIteVRsYXY4RVJWTko5S2RTVFFIeG02a2tlIn0sImlzcyI6Imh0dHBzOi8vYXV0aDAub3BlbmFpLmNvbS8iLCJzdWIiOiJhdXRoMHw2M2I4NmRmMDM1ZTk3YTc5NDZkNmUzYmMiLCJhdWQiOlsiaHR0cHM6Ly9hcGkub3BlbmFpLmNvbS92MSIsImh0dHBzOi8vb3BlbmFpLm9wZW5haS5hdXRoMGFwcC5jb20vdXNlcmluZm8iXSwiaWF0IjoxNzE2NTAyMDAyLCJleHAiOjE3MTczNjYwMDIsInNjb3BlIjoib3BlbmlkIHByb2ZpbGUgZW1haWwgbW9kZWwucmVhZCBtb2RlbC5yZXF1ZXN0IG9yZ2FuaXphdGlvbi5yZWFkIG9mZmxpbmVfYWNjZXNzIiwiYXpwIjoicGRsTElYMlk3Mk1JbDJyaExoVEU5VlY5Yk45MDVrQmgifQ.bhvzdLWzFto6iBhbadtLKQxe2X8uh5WwI5a1pEgzNLZ7ureqrmNuwrXGX7S0ynmSHfK7KT9tkGw9kEpBrvYpE4VuXOu3fgyHGBPlFZZGrIDJ9Z8gDn9Xcps_H23nVHW-_Zf6UJG6ojRx7MQ6vgXm-dxbVgE2TsrS6RaJB1KnBX59jxKBHyl1UYUAbeKTvtzpMsObOB-LeRTfNpx-cHUhZYelJJCy0gSnVcY2eRVTJbYBDzkWv370goqaZ5MRZSsSFljDVQgXsAtr-oMUgxYmxHPvruCjoB2GpXOWB70gvxmcswRPaFdSkbfznyYjaeIxxxXqMhk40bGDPleQe-TCmQ";
        String response = RequestUtils.postRequest("https://api.oaifree.com/v1/chat/completions", params, key);
        System.out.println(response);
    }

    @Test
    void testMongoFind() {
        List<TitleEntity> all = mongoTemplate.findAll(TitleEntity.class);
        System.out.println(all.size());
        all.forEach(titleEntity -> {
            System.out.println(titleEntity.toString());
        });
        System.out.println("------------------------------------------------------------------------------------------------------------------");
        Query query = new Query();
        query.addCriteria(Criteria.where("conversationId").is("9c370915-1ad5-4228-8ab4-7980bfb070e2"));
        List<MessageEntity> messageEntities = mongoTemplate.find(query, MessageEntity.class);
        messageEntities.forEach(messageEntity -> {
            System.out.println(messageEntity.getMessage().getContent().getParts().get(0));
        });
        System.out.println("------------------------------------------------------------------------------------------------------------------");
        List<TitleEntity> titles = messageDao.findTitles(0, 3);
        titles.forEach(titleEntity -> {
            System.out.println(titleEntity.toString());
        });

    }

    @Test
    void testReToAcc() {
        OkHttpClient client = new OkHttpClient().newBuilder().build();
        RequestBody body = new FormBody.Builder()
                .add("refresh_token", "lJovobGJ8EvT48smfxgLV17PYMT0yi9yqfyqfNLBrOZy8")
                .build();
        Request request = new Request.Builder()
                .url("https://token.oaifree.com/api/auth/refresh")
                .post(body)
                .addHeader("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
                .build();
        try {
            Response execute = client.newCall(request).execute();
            if(execute.isSuccessful()) {
                String responseStr = execute.body().string();
                System.out.println(responseStr);
                JSONObject jsonObject = JSONUtil.parseObj(responseStr);
                String accessToken = jsonObject.getStr("access_token");
                System.out.println(accessToken);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

//        OkHttpClient client = new OkHttpClient().newBuilder()
//                .build();
//        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
//        RequestBody body = RequestBody.create(mediaType, "refresh_token=lJovobGJ8EvT48smfxgLV17PYMT0yi9yqfyqfNLBrOZy8");
//        Request request = new Request.Builder()
//                .url("https://token.oaifree.com/api/auth/refresh")
//                .method("POST", body)
//                .addHeader("Accept", "*/*")
//                .addHeader("Host", "token.oaifree.com")
//                .addHeader("Connection", "keep-alive")
//                .addHeader("Content-Type", "application/x-www-form-urlencoded")
//                .build();
//        try {
//            Response response = client.newCall(request).execute();
//            System.out.println(response.body().toString());
//            JSONObject jsonObject = JSONUtil.parseObj(response.body().toString());
//            String accessToken = jsonObject.getStr("access_token");
//            System.out.println(accessToken);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }

    }

}
