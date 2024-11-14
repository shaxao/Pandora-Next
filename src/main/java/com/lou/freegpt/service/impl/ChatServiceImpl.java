package com.lou.freegpt.service.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.lou.freegpt.api.xunfei.AudioStream;
import com.lou.freegpt.dao.MessageDao;
import com.lou.freegpt.domain.InputStreamRequestBody;
import com.lou.freegpt.enums.AjaxResult;
import com.lou.freegpt.service.AppConfigService;
import com.lou.freegpt.service.ChatService;
import com.lou.freegpt.utils.RequestUtils;
import com.lou.freegpt.vo.MessageVo;
import com.lou.freegpt.vo.TitleVo;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.lang3.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;

@Service
@Slf4j
public class ChatServiceImpl implements ChatService {
    @Autowired
    private RequestUtils requestUtils;
    @Autowired
    private MessageDao messageDao;
    @Autowired
    private AppConfigService appConfigService;

    /**
     * 回复消息
     *
     * @param message
     * @param model
     * @param fileEncoder
     * @return
     */
    @Override
    public String processMessage(String message, String model, HttpServletResponse httpServletResponse, String fileEncoder) {
        System.out.println("文本消息处理service接收到的消息:" + message);

        String body = null;
        try {
            requestUtils.textRequest(message,model,httpServletResponse,fileEncoder);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //{
        //  "id": "chatcmpl-8MuHMhaMEYGT0xZNzoNBc3crWPIKy",
        //  "object": "chat.completion",
        //  "created": 1700470244,
        //  "model": "gpt-3.5-turbo-1106",
        //  "choices": [
        //    {
        //      "index": 0,
        //      "message": {
        //        "role": "assistant",
        //        "content": "{\"game\":\"英雄联盟\",\"season\":\"S9\",\"champion\":\"冠军\"}"
        //      },
        //      "finish_reason": "stop"
        //    }
        //  ],
        //  "usage": {
        //    "prompt_tokens": 47,
        //    "completion_tokens": 23,
        //    "total_tokens": 70
        //  },
        //  "system_fingerprint": "fp_eeff13170a"
        //}
        System.out.println("返回的消息体: " + body);
        String bodySub = body.substring(body.indexOf("data:") + "data:".length());
        String content = "";
        //获取message
        try {
            JSONObject jsonObject = JSONUtil.parseObj(bodySub);
            JSONObject firstChoice = jsonObject.getJSONArray("choices").getJSONObject(0);
            content = firstChoice.getJSONObject("delta").getStr("content");
            System.out.println("回复的内容:" + content);
        }catch (Exception e){
            e.printStackTrace();
            content = body;
        }

        return content;
    }


    /**
     * 生成图片
     * @param globalSetJson
     * @return
     */
    @Override
    public String imageGenera(MessageVo messageVo, String globalSetJson) {
        String body = null;
        try {
            body = requestUtils.imageRequest(messageVo, globalSetJson);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("返回的消息体: " + body);
        JSONObject jsonObject = null;
        try {
           jsonObject  = JSONUtil.parseObj(body);
        }catch (Exception e){
            System.out.println("格式化url失败，不是正常链接，是错误消息");
            e.printStackTrace();
            return body;
        }

        //获取message
        JSONArray dataArray = jsonObject.getJSONArray("data");

        // 检查 "data" 数组是否为空以及是否有元素
        if (dataArray != null && !dataArray.isEmpty()) {
            JSONObject firstDataObject = dataArray.getJSONObject(0);
            String imageUrl = firstDataObject.getStr("url");
            System.out.println("URL 值为：" + imageUrl);
            return imageUrl;
        } else {
            System.out.println("数据为空或不存在！");
            return body == null ? body : null;
        }
    }

    @Override
    public String genTitle(TitleVo titleVo) {
        if(titleVo.getIsWeb()) {
            JSONObject toJsonObject = new JSONObject();
            toJsonObject.putOnce("message_id", titleVo.getMessageId());
            String params = toJsonObject.toString();
//            String params = "{\r\n    \"message_id\": \"" + titleVo.getMessageId() + "\"  \r\n}\"";
            String response = RequestUtils.postRequest(titleVo.getBaseUrl() + "/backend-api/conversation/gen_title/" + titleVo.getConversationId(), params, titleVo.getApiKey());
            log.info("web gen_titlte response:{}", response);
            JSONObject jsonObject = JSONUtil.parseObj(response);
            String title = jsonObject.getStr("title");
            messageDao.insertTitle(titleVo.getConversationId(), title);
            return title != null ? title : null;
        }
        String message = StringEscapeUtils.escapeHtml4(titleVo.getContent());
        String escapedContent = message.replace("\n", "").replace("\"", "\\\"");
//        String params = "{\r\n  \"model\": \"gpt-3.5-turbo\",\r\n  \"messages\": [\r\n    {\r\n      \"role\": \"system\",\r\n      \"content\": \"你是一个经过优秀训练并且具备思考能力的人工智能大模型，熟悉各种语言语法以及典故，能够将长文本总结为精练的短文本\"\r\n    },\r\n    {\r\n      \"role\": \"user\",\r\n      \"content\": \"为以下查询创建一个简洁的、3-5 个词的短语作为标题，严格遵守 3-5 个词的限制并避免使用“标题”一词：: " + escapedContent + "\"\r\n    }\r\n  ],\r\n  \"stream\": false,\r\n  \"temperature\": 0.5,\r\n  \"top_p\": 1.0,\r\n  \"max_tokens\": 4000\r\n}";
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("model", "gpt-3.5-turbo");
        // 创建messages数组
        JSONArray messagesArray = new JSONArray();

        // 创建第一个message对象
        JSONObject systemMessage = new JSONObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", "你是一个经过优秀训练并且具备思考能力的人工智能大模型，熟悉各种语言语法以及典故，能够将长文本总结为精练的短文本");
        messagesArray.add(systemMessage);

        // 创建第二个message对象
        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", "为以下文本创建一个简洁的、3-5 个词的短语作为标题，严格遵守 3-5 个词的限制并避免使用“标题”一词：: " + escapedContent);
        messagesArray.add(userMessage);

        // 将messages数组放入外层的JSONObject
        jsonObject.put("messages", messagesArray);
        jsonObject.put("stream", false);
        jsonObject.put("temperature", 0.5);
        jsonObject.put("top_p", 1.0);
        jsonObject.put("max_tokens", 4000);
        String params = jsonObject.toStringPretty();
        if( titleVo.getBaseUrl() == null || titleVo.getBaseUrl().equals("")) {
            titleVo.setBaseUrl(appConfigService.getUrl());
            titleVo.setApiKey(appConfigService.getApiKey());
        }
        String response = RequestUtils.postRequest(titleVo.getBaseUrl() + "/v1/chat/completions", params, titleVo.getApiKey());
        log.info("web gen_titlte response:{}", response);
        JSONObject jsonObject1 = JSONUtil.parseObj(response);
        JSONObject choices = jsonObject1.getJSONArray("choices").getJSONObject(0);
        String title = choices != null ? choices.getJSONObject("message").getStr("content") : null;
        messageDao.insertTitle(titleVo.getConversationId(), title);
        return title;
    }

    @Override
    public AjaxResult convertAudioToText(MultipartFile audioFile) {
        // 讯飞
//        CountDownLatch latch = new CountDownLatch(1);
//        AudioStream webIATWS = new AudioStream(latch);
//
//        try {

//            // 将 MultipartFile 的 InputStream 传递到 WebIATWS 实例
//            webIATWS.processAudioStream(pcmInputStream, audioFile.getSize());
//            latch.await(); // 等待 WebSocket 处理完成
//            String result = webIATWS.getAudioResult();
//            return AjaxResult.success("音频处理成功", result);
//        } catch (Exception e) {
//            log.error("音频处理失败", e);
//            return AjaxResult.error("音频处理失败: " + e.getMessage());
//        }
        //String text = chatService.convertAudioToText(audioFile);
        // return text == null || text.equals("") ? AjaxResult.fail("音频处理失败") : AjaxResult.success("音频处理成功", text);
        // openAI
        // 将 MultipartFile 转换为 InputStream
        try (InputStream inputStream = audioFile.getInputStream()) {
            // 构建 OkHttp 请求
            OkHttpClient client = new OkHttpClient().newBuilder()
                    .build();

            // 使用 InputStream 创建 RequestBody
            RequestBody fileBody = new InputStreamRequestBody(MediaType.parse("application/octet-stream"), inputStream);

            // 构建 multipart 请求体
            RequestBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("file", audioFile.getOriginalFilename(), fileBody)
                    .addFormDataPart("model", "whisper-1")
                    .build();

            // 创建请求
            Request request = new Request.Builder()
                    .url(appConfigService.getAduioOaiUrl() + appConfigService.getAudioOaiTranUri())
                    .post(body)
                    .addHeader("Authorization", "Bearer " + appConfigService.getAudioOaiApiKey())
                    .addHeader("Content-Type", "multipart/form-data; boundary=--------------------------237733332766619974790278")
                    .build();

            // 执行请求
            Response response = client.newCall(request).execute();
            String responseBody = response.body().string();
            // 处理响应（例如，记录或返回结果）
            log.info("Response: {}", responseBody);
            JSONObject jsonObject = JSONUtil.parseObj(responseBody);
            String text = jsonObject.getStr("text");
            return text == null || text.equals("") ? AjaxResult.fail("生成文本失败") : AjaxResult.success("音频处理成功", text);

        } catch (IOException e) {
            log.error("Error processing audio file", e);
            return AjaxResult.error("处理音频文件时出错");
        }
    }

//    public static void main(String[] args) {
//        String bodySub = "data:{\"id\":\"chatcmpl-123\",\"object\":\"chat.completion.chunk\",\"created\":1694268190,\"model\":\"gpt-3.5-turbo-0613\", \"system_fingerprint\": \"fp_44709d6fcb\", \"choices\":[{\"index\":0,\"delta\":{\"role\":\"assistant\",\"content\":\"哈哈\"},\"logprobs\":null,\"finish_reason\":null}]}";
//        String jsonString = bodySub.substring(bodySub.indexOf("data:") + "data:".length());
//        System.out.println("jsonString:" + jsonString);
//        String content = "";
//        try {
//            JSONObject jsonObject = JSONUtil.parseObj(jsonString);
//            JSONObject firstChoice = jsonObject.getJSONArray("choices").getJSONObject(0);
//            content = firstChoice.getJSONObject("delta").getStr("content");
//        }catch (Exception e){
//            e.printStackTrace();
//            content = jsonString;
//        }
        //System.out.println("content:" + content);
//        JSONObject jsonObject = JSONUtil.parseObj(jsonString);
//        JSONArray dataArray = jsonObject.getJSONArray("data");
//
//        // 检查 "data" 数组是否为空以及是否有元素
//        if (dataArray != null && !dataArray.isEmpty()) {
//            JSONObject firstDataObject = dataArray.getJSONObject(0);
//            String urlValue = firstDataObject.getStr("url");
//
//            System.out.println("URL 值为：" + urlValue);
//        } else {
//            System.out.println("数据为空或不存在！");
//        }
}
