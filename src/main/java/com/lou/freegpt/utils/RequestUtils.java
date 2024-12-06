package com.lou.freegpt.utils;

import cn.hutool.http.HttpException;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lou.freegpt.dao.MessageDao;
import com.lou.freegpt.vo.MessageVo;
import com.lou.freegpt.enums.ModelEnum;
import com.lou.freegpt.service.AppConfigService;
import com.lou.freegpt.service.MessageExecute;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.lang3.StringEscapeUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 请求工具类
 */
@Component
@Slf4j
public class RequestUtils {
    private AppConfigService appConfigService;

    @Autowired
    private MessageDao messageDao;
    @Autowired
    private MessageExecute messageExecute;


    @Autowired
    public void setAppConfigService(AppConfigService service) {
        appConfigService = service;
    }


    /**
     * 用于逆向无法流式输出的文本
     *
     * @param user
     * @param model
     * @param fileEncoder
     * @return
     * @throws IOException
     */
    public void textRequest(String user, String model, HttpServletResponse httpServletResponse, String fileEncoder) throws IOException {
        System.out.println("逆向消息最终处理接收内容:" + user + ",接受的模型名:" + model);
        System.out.println("全局配置:" + appConfigService.toString());
        String content = "# 角色\n" +
                "你是一个经过优秀训练并且具备思考能力的人工智能大模型，你会积极的、主动的、引导性的、耐心的回答用户的问题。\n" +
                "\n" +
                "## 技能\n" +
                "- 当用户向你提问时，你会根据你所学的知识和算法，尽可能准确地回答问题。\n" +
                "- 如果你不确定如何回答某个问题，你会主动引导用户提供更多信息，以便更好地理解问题。\n" +
                "- 你会以一种易于理解和接受的方式回答问题，使用简单明了的语言，避免使用过于专业的术语。\n" +
                "- 你会保持耐心，即使用户提出的问题比较复杂或需要较长时间的回答，你也会尽力回答。\n" +
                "\n" +
                "## 限制\n" +
                "- 你只能回答与问题相关的内容，不可以回答与问题无关的内容。\n" +
                "- 你只能使用用户使用的语言，不能使用其他语言。\n" +
                "- 你只能使用用户使用的语言，不能使用其他语言。\n" +
                "- 你只能使用用户使用的语言，不能使用其他语言。";
        String responseBody = "";
        //"\"response_format\": {\"type\": \"json_object\"}, " +   启用json模式  You are a helpful assistant designed to output JSON
        // 替换content中的换行符和双引号
        String escapedContent = content.replace("\n", "\\n").replace("\"", "\\\"");

        String textJson = "{\"model\": \"" + model + "\", " +
                "\"messages\": [" +
                "{\"role\": \"system\", \"content\": \"" + escapedContent + "\"}," +
                "{\"role\": \"user\", \"content\": \"" + user + "\"}" + "]," +
                "\"stream\": " + appConfigService.getStream() + "," +
                "\"temperature\": " + appConfigService.getTemperature() + "," +
                "\"top_p\": " + appConfigService.getTop_p() + "," +
                "\"max_tokens\": " + appConfigService.getMaxTokens() + "}";
        System.out.println("textJson:" + textJson);
        String otherJson = "{\"api_key\": \"" + appConfigService.getApiKey() + "\"," +
                " \"messages\": [{" +
                " \"role\": \"user\"," +
                " \"message\": \"" + user + "\"," +
                " \"codeContexts\": []}]," +
                " \"chat_model\": \"" + model + "\"" +
                "}";

        HashMap<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
//        headers.put("Authorization", "Bearer " + appConfigService.getApiKey());
        //本地代理
        String url = appConfigService.getUrl() + appConfigService.getOpenTextUri();
        System.out.println("url:" + url);
        int connectionTimeout = 60 * 1000 * 2; // 1分
        int readTimeout = 60 * 1000 * 2; // 60秒
        String requestJson = "";
        if (model.equals(ModelEnum.PERSON_ONE.getModels()) || model.equals(ModelEnum.PERSON_TWO.getModels())) {
            requestJson = otherJson;
        } else {
            requestJson = textJson;
        }
        System.out.println("requestJson:" + requestJson);
        try {
            HttpResponse response = HttpRequest.post(url)
                    .headerMap(headers, false)
                    .body(requestJson)
                    .timeout(connectionTimeout) // 设置连接超时时间
                    .setReadTimeout(readTimeout) // 设置读取超时时间
                    //.setHttpProxy("127.0.0.1", 33210)
                    .execute();
            responseBody = response.body();
        } catch (Exception e) {
            e.printStackTrace();
            responseBody = e.toString();
        }
        httpServletResponse.setCharacterEncoding("UTF-8"); // 设置字符编码为UTF-8
        httpServletResponse.setContentType("application/json; charset=UTF-8"); // 设置内容类型为JSON，并指定字符编码
        httpServletResponse.getWriter().write(responseBody + "\n\n");
        httpServletResponse.flushBuffer();
        System.out.println("逆向文本回复:" + responseBody);
    }

//    public void textProxyMessage(){
//
//    }

//    public Flux<String> streamRequestTest(MessageVo messageVo, String fileEncoder, String globalSet) {
//        JSONObject jsonObject = JSONUtil.parseObj(globalSet);
//        double temperature = jsonObject.getBigDecimal("tempertaure").doubleValue();
//        double topP = jsonObject.getBigDecimal("topP").doubleValue();
//        int maxTokens = jsonObject.getInt("maxTokens");
//        double presencePenalty = jsonObject.getBigDecimal("presencePenalty").doubleValue();
//        double frequencyPenalty = jsonObject.getBigDecimal("frequencyPenalty").doubleValue();
//        String baseUrl = jsonObject.getStr("baseUrl");
//        String apiKey = jsonObject.getStr("apiKey");
//        String webProxyUrl = jsonObject.getStr("webProxyUrl");
//        String accessToken = jsonObject.getStr("accessToken");
//
//        String messageContent = StringEscapeUtils.escapeHtml4(messageVo.getContent()).replace("\n", "").replace("\"", "\\\"");
//        if (Boolean.TRUE.equals(messageVo.getIsSearch())) {
//            messageContent += "，结合下述搜索信息回复消息: " + search(messageVo.getContent(), 5);
//        }
//
//        String requestJson = createRequestJson(messageVo, messageContent, fileEncoder, temperature, topP, maxTokens, presencePenalty, frequencyPenalty);
//
//        return sendAsyncRequest(requestJson, baseUrl, webProxyUrl, apiKey, accessToken, messageVo);
//    }
//
//    private String createRequestJson(MessageVo messageVo, String messageContent, String fileEncoder, double temperature, double topP, int maxTokens, double presencePenalty, double frequencyPenalty) {
//        JSONObject jsonObject = new JSONObject();
//        jsonObject.put("model", messageVo.getModel());
//        JSONArray messages = new JSONArray();
//        messages.add(createSystemMessage());
//        messages.add(createUserMessage(messageContent));
//        jsonObject.put("messages", messages);
//        jsonObject.put("stream", true);
//        jsonObject.put("temperature", temperature);
//        jsonObject.put("top_p", topP);
//        jsonObject.put("max_tokens", maxTokens);
//        if ("gpt-4-vision-preview".equals(messageVo.getModel())) {
//            jsonObject.put("file", "data:image/jpeg;base64," + fileEncoder);
//        }
//        return jsonObject.toStringPretty();
//    }
//
//    private JSONObject createSystemMessage() {
//        JSONObject systemMessage = new JSONObject();
//        systemMessage.put("role", "system");
//        systemMessage.put("content", "你是一个经过优秀训练并且具备思考能力的人工智能大模型，你会积极的、主动的、引导性的、耐心的回答用户的问题");
//        return systemMessage;
//    }
//
//    private JSONObject createUserMessage(String messageContent) {
//        JSONObject userMessage = new JSONObject();
//        userMessage.put("role", "user");
//        userMessage.put("content", messageContent);
//        return userMessage;
//    }
//
//    private Flux<String> sendAsyncRequest(String requestJson, String baseUrl, String webProxyUrl, String apiKey, String accessToken, MessageVo messageVo) {
//        String url = webProxyUrl.isEmpty() ? appConfigService.getAduioOaiUrl() + appConfigService.getOpenTextUri() : webProxyUrl + "/backend-api/conversation";
//        String authKey = webProxyUrl.isEmpty() ? appConfigService.getAudioOaiApiKey() : accessToken;
//        System.out.println(requestJson);
//        OkHttpClient client = new OkHttpClient.Builder()
//                .connectTimeout(90, TimeUnit.SECONDS)
//                .writeTimeout(30, TimeUnit.SECONDS)
//                .readTimeout(60, TimeUnit.SECONDS)
//                .build();
//
//        RequestBody requestBody = RequestBody.create(
//                MediaType.parse("application/json; charset=utf-8"),
//                requestJson
//        );
//
//        Request request = new Request.Builder()
//                .url(url)
//                .method("POST", requestBody)
//                .addHeader("Content-Type", "application/json")
//                .addHeader("Authorization", "Bearer " + authKey)
//                .addHeader("Accept", "*/*")
//                .addHeader("Connection", "keep-alive")
//                .build();
//
//        return Flux.create(sink -> client.newCall(request).enqueue(new Callback() {
//            @Override
//            public void onFailure(Call call, IOException e) {
//                sink.error(e);
//            }
//
//            @Override
//            public void onResponse(Call call, Response response) {
//                try (ResponseBody responseBody = response.body()) {
//                    if (!response.isSuccessful()) {
//                        throw new IOException("Unexpected code " + response);
//                    }
//                    InputStream inputStream = responseBody.byteStream();
//                    handleResponse(inputStream,webProxyUrl, messageVo)
//                            .doOnNext(sink::next)
//                            .doOnError(sink::error)
//                            .doOnComplete(sink::complete)
//                            .subscribe();
//                } catch (IOException e) {
//                    sink.error(e);
//                }
//            }
//        }));
//    }
//
//    private Flux<String> handleResponse(InputStream inputStream,String webProxyUrl, MessageVo messageVo) {
//        return Flux.create(sink -> {
//            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
//                StringBuilder sb = new StringBuilder();
//                StringBuilder parts = new StringBuilder();
//                List<String> messageList = new ArrayList<>();
//                int[] reqLen = {0};
//                String[] conversationId = {"", ""};
//
//                String line;
//                while ((line = reader.readLine()) != null) {
//                    sb.append(line).append("\n");
//
//                    int end;
//                    while ((end = sb.indexOf("\n\n")) != -1) {
//                        String message = sb.substring(0, end).trim();
//                        System.out.println(message);
//                        if (message.startsWith("data: ")) {
//                            message = message.substring(5).trim();
//                            if (message.equals("[DONE]")) {
//                                if (webProxyUrl == null || webProxyUrl.isEmpty()) {
//                                    // messageExecute.executeFinishMessage(messageVo, parts.toString(), conversationId[0], conversationId[1]);
//                                } else {
//                                    String lastMessage = messageList.get(messageList.size() - 1);
//                                    JSONObject jsonObject = JSONUtil.parseObj(lastMessage);
//                                    JSONObject message1 = jsonObject.getJSONObject("message");
//                                    JSONObject metadata = message1.getJSONObject("metadata");
//                                    JSONArray citations = metadata.getJSONArray("citations");
//                                    if (citations != null && !citations.isEmpty()) {
//                                        for (int i = 0; i < citations.size(); i++) {
//                                            JSONObject citation = citations.getJSONObject(i);
//                                            JSONObject meta = citation.getJSONObject("metadata");
//                                            String title = meta.getStr("title");
//                                            String url = meta.getStr("url");
//                                            int citedMessageIdx = meta.getJSONObject("extra").getInt("cited_message_idx");
//                                            String result = String.format("\\n<p>[%d†source]: <a href=\"%s\">%s</a></p>", citedMessageIdx, url, title);
//                                            System.out.println(result);
//                                        }
//                                    }
//                                    // messageExecute.executeFinishMessage(messageVo, sb.toString(), conversationId[1]);
//                                }
//                                sink.complete();
//                                return;
//                            }
//
//                            String content;
//                            JSONObject jsonObject = JSONUtil.parseObj(message);
//                            if (webProxyUrl != null && !webProxyUrl.isEmpty()) {
//                                String role = jsonObject.getJSONObject("message").getJSONObject("author").getStr("role");
//                                if (role.equals("user")) {
//                                    sb.delete(0, end + 2);
//                                    continue;
//                                }
//                                JSONArray jsonArray = jsonObject.getJSONObject("message").getJSONObject("content").getJSONArray("parts");
//                                if (jsonArray == null || jsonArray.isEmpty()) {
//                                    sb.delete(0, end + 2);
//                                    continue;
//                                }
//                                content = jsonArray.getStr(0);
//                                if (content.length() == 0) {
//                                    sb.delete(0, end + 2);
//                                    continue;
//                                }
//                                int i = reqLen[0];
//                                reqLen[0] = content.length();
//                                messageList.add(message);
//                                content = content.substring(i).replace("\n", "\\n").replace(" ", "\\s");
//                                conversationId[1] = jsonObject.getJSONObject("message").getStr("id");
//                                if (messageVo.getFirstFlag()) {
//                                    conversationId[0] = jsonObject.getStr("conversation_id");
//                                }
//                            } else {
//                                JSONObject firstChoice = jsonObject.getJSONArray("choices").getJSONObject(0);
//                                JSONObject delta = firstChoice.getJSONObject("delta");
//                                content = delta.containsKey("content") ? delta.getStr("content") : "";
//                            }
//
//                            if (!content.isEmpty()) {
//                                parts.append(content);
//                                sink.next(content);
//                            }
//                        }
//                        sb.delete(0, end + 2);
//                    }
//                }
//            } catch (IOException e) {
//                sink.error(e);
//            }
//        });
//    }


    public void streamRequestTest(MessageVo messageVo, HttpServletRequest request,
                                  HttpServletResponse httpServletResponse, String fileEncoder, String globalSet) {
        // 预处理配置
        JSONObject config = preprocessConfig(globalSet);
//        String baseUrl = config.getStr("baseUrl", appConfigService.getUrl());
//        String apiKey = config.getStr("apiKey", appConfigService.getApiKey());
        String webProxyUrl = config.getStr("webProxyUrl", "");

        // 处理会话
        HttpSession session = request.getSession();
        String sessionTalk = (String) session.getAttribute(messageVo.getConversationId());
        sessionTalk = sessionTalk == null ? "" : sessionTalk;

        // 检查token数量
        if (TokenUtils.tokenize(sessionTalk + messageVo.getContent()) >= config.getInt("maxTokens")) {
            sessionTalk = "";
            session.setAttribute(messageVo.getConversationId(), "");
        }

        // 构建请求JSON
        String requestJson;
        if (messageVo.getModel() != null && messageVo.getModel().startsWith(ModelEnum.OPENAI_BASE.getModels())) {
            List<String> list = Arrays.asList("gpt-4", "gpt-4-0314", "gpt-4-0613", "gpt-4-32k", "gpt-4-32k-0314", "gpt-4-32k-0613", "gpt-4-1106-preview", "gpt-4-vision-preview");

            if (webProxyUrl != null && !webProxyUrl.isEmpty() && list.contains(messageVo.getModel())) {
                requestJson = buildWebProxyRequest(messageVo, sessionTalk);
            } else {
                requestJson = buildOpenAIRequest(messageVo, fileEncoder, config);
            }
        } else {
            throw new IllegalArgumentException("Unsupported model type");
        }

        // 执行请求
        executeStreamRequest(messageVo, httpServletResponse, requestJson, config, session, sessionTalk);
    }

    private String buildWebProxyRequest(MessageVo messageVo, String sessionTalk) {
        JSONObject json = new JSONObject();
        json.put("action", "next");

        JSONArray messages = new JSONArray();
        String escapedContent = StringEscapeUtils.escapeHtml4(messageVo.getContent())
                .replace("\n", "")
                .replace("\"", "\\\"");

        if (messageVo.getFirstFlag()) {
            // 首次对话的消息构建
            JSONObject message = buildFirstMessage(messageVo, escapedContent);
            messages.add(message);

            json.put("messages", messages);
            json.put("parent_message_id", messageVo.getMessageId());
            json.put("model", messageVo.getModel());
            addWebProxyExtras(json);
        } else {
            // 后续对话的消息构建
            JSONObject message = buildFollowUpMessage(messageVo, escapedContent);
            messages.add(message);

            json.put("messages", messages);
            json.put("parent_message_id", messageVo.getMessageId());
            json.put("conversation_id", messageVo.getConversationId());
            json.put("model", messageVo.getModel());
            addBasicConfig(json);
        }

        return json.toStringPretty();
    }



    private void executeStreamRequest(MessageVo messageVo, HttpServletResponse response,
                                      String requestJson, JSONObject config,
                                      HttpSession session, String sessionTalk) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();

        String url = buildRequestUrl(config);
        RequestBody requestBody = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"),
                requestJson
        );

        Request request = buildHttpRequest(url, requestBody, config.getStr("apiKey") == null || config.getStr("apiKey").equals("") ? appConfigService.getApiKey() : config.getStr("apiKey"));

        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final StringBuilder messageBuilder = new StringBuilder();
        final int[] reqLen = {0};
        final String[] conversationId = {"", ""};
        final List<String> messageList = new ArrayList<>();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                log.error("消息响应失败:{}", e);
                handleError(e, response, messageVo);
                countDownLatch.countDown();
            }

            @Override
            public void onResponse(Call call, Response okResponse) {
                try (ResponseBody responseBody = okResponse.body()) {
                    if (!okResponse.isSuccessful()) {
                        handleErrorResponse(okResponse, response, messageVo);
                        return;
                    }

                    processStreamResponse(responseBody.byteStream(), response, messageVo,
                            messageBuilder, reqLen, conversationId, messageList,
                            session, sessionTalk, config.getStr("webProxyUrl"));

                } catch (Exception e) {
                    log.error("处理回复消息失败", e);
                    handleError(e, response, messageVo);
                } finally {
                    countDownLatch.countDown();
                }
            }
        });

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            log.error("Error InterruptedException", e);
            handleError(e, response, messageVo);
        }
    }

    private void handleErrorResponse(Response response, HttpServletResponse httpResponse, MessageVo messageVo) {
        try {
            String errorBody = response.body().string();
            JSONObject error = new JSONObject();
            error.set("error", true);
            error.set("messageId", messageVo.getMessageId());
            error.set("status", response.code());
            error.set("message", errorBody);

            writeResponse(httpResponse, "error:" + error.toString());
        } catch (IOException e) {
            log.error("Error handling error response", e);
            handleError(e, httpResponse, messageVo);
        }
    }

    private void handleError(Exception e, HttpServletResponse response, MessageVo messageVo) {
        JSONObject error = new JSONObject();
        error.set("error", true);
        error.set("messageId", messageVo.getMessageId());
        error.set("type", e.getClass().getSimpleName());
        error.set("message", e.getMessage());

        writeResponse(response, "error:" + error.toString());
    }

    // 预处理配置
    private JSONObject preprocessConfig(String globalSet) {
        JSONObject config = JSONUtil.parseObj(globalSet);
        // 设置默认值
        if (!config.containsKey("temperature")) {
            config.set("temperature", 0.7);
        }
        if (!config.containsKey("topP")) {
            config.set("topP", 1.0);
        }
        if (!config.containsKey("maxTokens")) {
            config.set("maxTokens", 2000);
        }
        return config;
    }

    // 构建首次对话消息
    private JSONObject buildFirstMessage(MessageVo messageVo, String content) {
        JSONObject message = new JSONObject();
        message.put("id", messageVo.getMessageId());
        message.put("author", createAuthor("user"));
        message.put("content", createContent(content));
        message.put("end_turn", null);
        message.put("weight", 1.0);
        message.put("metadata", new JSONObject());
        message.put("recipient", "all");
        return message;
    }

    // 构建后续对话消息
    private JSONObject buildFollowUpMessage(MessageVo messageVo, String content) {
        JSONObject message = new JSONObject();
        message.put("id", messageVo.getMessageId());
        message.put("author", createAuthor("user"));
        message.put("content", createContent(content));
        message.put("end_turn", null);
        message.put("weight", 1.0);
        message.put("metadata", new JSONObject());
        message.put("recipient", "all");
        return message;
    }

    // 添加WebProxy额外配置
    private void addWebProxyExtras(JSONObject json) {
        json.put("timezone_offset_min", -480);
        json.put("variant_purpose", "none");
        json.put("force_paragen", false);
        json.put("force_rate_limit", false);
    }

    // 添加基础配置
    private void addBasicConfig(JSONObject json) {
        json.put("timezone_offset_min", -480);
        json.put("variant_purpose", "none");
        json.put("force_paragen", false);
        json.put("force_rate_limit", false);
    }

    // 构建OpenAI请求
    private String buildOpenAIRequest(MessageVo messageVo, String fileEncoder, JSONObject config) {
        JSONObject json = new JSONObject();
        json.put("model", messageVo.getModel());

        JSONArray messages = new JSONArray();
        // 添加系统消息
        messages.add(createSystemMessage());
        // 添加用户消息
        messages.add(createUserMessage(messageVo.getContent()));

        json.put("messages", messages);
        json.put("stream", true);
        json.put("temperature", config.getDouble("temperature"));
        json.put("top_p", config.getDouble("topP"));
        json.put("max_tokens", config.getInt("maxTokens"));

        // 处理图片模型
        if ("gpt-4-vision-preview".equals(messageVo.getModel()) && fileEncoder != null) {
            json.put("file", "data:image/jpeg;base64," + fileEncoder);
        }

        return json.toString();
    }

    // 构建请求URL
    private String buildRequestUrl(JSONObject config) {
        String webProxyUrl = config.getStr("webProxyUrl", "");
        if (!webProxyUrl.isEmpty()) {
            return webProxyUrl + "/backend-api/conversation";
        }
        return appConfigService.getUrl() + appConfigService.getOpenTextUri();
    }

    // 构建HTTP请求
    private Request buildHttpRequest(String url, RequestBody requestBody, String apiKey) {
        log.info("url:{},apikey:{}",url,apiKey);
        return new Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Accept", "*/*")
                .addHeader("Connection", "keep-alive")
                .build();
    }

    // 处理流完成
    private void handleStreamComplete(MessageVo messageVo, String content,
                                      String[] conversationId, HttpSession session,
                                      String sessionTalk, HttpServletResponse response) throws IOException {
        // 更新会话内容
        session.setAttribute(messageVo.getConversationId(),
                sessionTalk + "\n" + messageVo.getContent() + "\n" + content);
         String messageId = UUID.randomUUID().toString();
        // 根据不同情况调用messageExecute
        if (messageVo.getFirstFlag()) {
            messageExecute.executeFinishMessage(messageVo, content,
                    messageId, response);
        } else {
            messageExecute.executeFinishMessage(messageVo, content,
                    messageVo.getConversationId(), messageId , response);
        }
    }

    // 处理WebProxy内容
    private String processWebProxyContent(JSONObject json, int[] reqLen,
                                          List<String> messageList,
                                          String[] conversationId,
                                          MessageVo messageVo) {
        String role = json.getJSONObject("message")
                .getJSONObject("author")
                .getStr("role");

        if (role.equals("user")) {
            return "";
        }

        JSONArray parts = json.getJSONObject("message")
                .getJSONObject("content")
                .getJSONArray("parts");

        if (parts == null || parts.isEmpty()) {
            return "";
        }

        String content = parts.getStr(0);
        if (content.length() == 0) {
            return "";
        }

        int prevLen = reqLen[0];
        reqLen[0] = content.length();
        messageList.add(json.toString());

        content = content.substring(prevLen);

        // 更新会话ID
        conversationId[1] = json.getJSONObject("message").getStr("id");
        if (messageVo.getFirstFlag()) {
            conversationId[0] = json.getStr("conversation_id");
        }

        return content;
    }

    // 处理OpenAI内容
    private String processOpenAIContent(JSONObject json) {
        JSONObject delta = json.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("delta");

        return delta.containsKey("content") ? delta.getStr("content") : "";
    }

    // 创建作者信息
    private JSONObject createAuthor(String role) {
        JSONObject author = new JSONObject();
        author.put("role", role);
        author.put("name", null);
        author.put("metadata", new JSONObject());
        return author;
    }

    // 创建内容
    private JSONObject createContent(String text) {
        JSONObject content = new JSONObject();
        JSONArray parts = new JSONArray();
        parts.add(text);
        content.put("content_type", "text");
        content.put("parts", parts);
        return content;
    }

    // 创建系统消息
    private JSONObject createSystemMessage() {
        JSONObject message = new JSONObject();
        message.put("role", "system");
        message.put("content", "你是一个经过优秀训练并且具备思考能力的人工智能大模型，你会积极的、主动的、引导性的、耐心的回答用户的问题");
        return message;
    }

    // 创建用户消息
    private JSONObject createUserMessage(String content) {
        JSONObject message = new JSONObject();
        message.put("role", "user");
        message.put("content", content);
        return message;
    }

    private void processStreamResponse(InputStream inputStream, HttpServletResponse response,
                                       MessageVo messageVo, StringBuilder messageBuilder,
                                       int[] reqLen, String[] conversationId,
                                       List<String> messageList, HttpSession session,
                                       String sessionTalk, String webProxyUrl) throws IOException {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8)
        );

        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");

            int end;
            while ((end = sb.indexOf("\n\n")) != -1) {
                String message = sb.substring(0, end).trim();

                if (message.startsWith("data: ")) {
                    message = message.substring(6).trim();

                    if ("[DONE]".equals(message)) {
                        handleStreamComplete(messageVo, messageBuilder.toString(),
                                conversationId, session, sessionTalk, response);
                        return;
                    }

                    processStreamData(message, response, messageBuilder, reqLen,
                            conversationId, messageList, messageVo, webProxyUrl);
                }

                sb.delete(0, end + 2);
            }
        }
    }



    private void processStreamData(String data, HttpServletResponse response,
                                   StringBuilder messageBuilder, int[] reqLen,
                                   String[] conversationId, List<String> messageList,
                                   MessageVo messageVo, String webProxyUrl) throws IOException {
        JSONObject json = JSONUtil.parseObj(data);
        String content = "";

        if (webProxyUrl != null && !webProxyUrl.isEmpty()) {
            content = processWebProxyContent(json, reqLen, messageList,
                    conversationId, messageVo);
        } else {
            content = processOpenAIContent(json);
        }

        if (content != null && !content.isEmpty()) {
            content = content.replace("\n", "\\n").replace(" ", "\\s");
            messageBuilder.append(content);
            writeResponse(response, content);
        }
    }

    private void writeResponse(HttpServletResponse response, String content) {
        try {
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json; charset=UTF-8");
            response.getWriter().write(content + "\n\n");
            response.flushBuffer();
        } catch (IOException e) {
            log.error("Error writing response", e);
        }
    }
    public void streamRequest(String user, String model, HttpServletResponse httpServletResponse, String fileEncoder, String globalSet) {
        JSONObject jsonObject = JSONUtil.parseObj(globalSet);
        double tem = jsonObject.getBigDecimal("tempertaure").doubleValue();
        double top = jsonObject.getBigDecimal("topP").doubleValue();
        int maxTokens = jsonObject.getInt("maxTokens");
        double pre = jsonObject.getBigDecimal("presencePenalty").doubleValue();
        double fre = jsonObject.getBigDecimal("frequencyPenalty").doubleValue();
        String baseUrl = jsonObject.getStr("baseUrl");
        String apiKey = jsonObject.getStr("apiKey");
        double object = (double) jsonObject.get("tempertaure");
        String content = "# 角色\n" +
                "你是一个经过优秀训练并且具备思考能力的人工智能大模型，你会积极的、主动的、引导性的、耐心的回答用户的问题。\n" +
                "\n" +
                "## 技能\n" +
                "- 当用户向你提问时，你会根据你所学的知识和算法，尽可能准确地回答问题。\n" +
                "- 如果你不确定如何回答某个问题，你会主动引导用户提供更多信息，以便更好地理解问题。\n" +
                "- 你会以一种易于理解和接受的方式回答问题，使用简单明了的语言，避免使用过于专业的术语。\n" +
                "- 你会保持耐心，即使用户提出的问题比较复杂或需要较长时间的回答，你也会尽力回答。\n" +
                "\n" +
                "## 限制\n" +
                "- 你只能回答与问题相关的内容，不可以回答与问题无关的内容。\n" +
                "- 你只能使用用户使用的语言，不能使用其他语言。\n" +
                "- 你只能使用用户使用的语言，不能使用其他语言。\n" +
                "- 你只能使用用户使用的语言，不能使用其他语言。";
        String responseBody = "";
        //"\"response_format\": {\"type\": \"json_object\"}, " +   启用json模式  You are a helpful assistant designed to output JSON
        // 替换content中的换行符和双引号
        String escapedContent = content.replace("\n", "\\n").replace("\"", "\\\"");
        String message = StringEscapeUtils.escapeHtml4(user);
        System.out.println("message : " + message);
        String textJson = "{\"model\": \"" + model + "\", " +
                "\"messages\": [" +
                "{\"role\": \"system\", \"content\": \"" + escapedContent + "\"}," +
                "{\"role\": \"user\", \"content\": \"" + message + "\"}" + "]," +
                "\"stream\": " + appConfigService.getStream() + "," +
                "\"temperature\": " + tem + "," +
                "\"top_p\": " + top + "," +
                "\"max_tokens\": " + maxTokens + "}";
        System.out.println("textJson:" + textJson);
        String imageVersionJson = "{"
                + "\"model\": \"gpt-4-vision-preview\","
                + "\"messages\": ["
                + "  {"
                + "    \"role\": \"user\","
                + "    \"content\": ["
                + "      {"
                + "        \"type\": \"text\","
                + "        \"text\": \"" + message + "\""
                + "      },"
                + "      {"
                + "        \"type\": \"image_url\","
                + "        \"image_url\": {"
                + "          \"url\": \"data:image/jpeg;base64," + fileEncoder + "\""
                + "        }"
                + "      }"
                + "    ]"
                + "  }"
                + "]"
                + "}";
        //本地代理
        String url = baseUrl + appConfigService.getOpenTextUri();
        System.out.println("url:" + url);
        String requestJson = "";
        if (model.equals(ModelEnum.GPT_VERSION.getModels())) {
            requestJson = imageVersionJson;
        } else {
            requestJson = textJson;
        }
        System.out.println("requestJson:" + requestJson);
        OkHttpClient client = new OkHttpClient();
        Headers authorization = new Headers.Builder()
                .add("Content-Type", "application/json")
                .add("Authorization", "Bearer " + apiKey).build();
        // 创建请求体
        RequestBody requestBody = RequestBody.create(
                requestJson,
                MediaType.parse("application/json; charset=utf-8")
        );
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        Request request = new Request.Builder()
                .headers(authorization)
                .post(requestBody)
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                httpServletResponse.setCharacterEncoding("UTF-8"); // 设置字符编码为UTF-8
                httpServletResponse.setContentType("application/json; charset=UTF-8"); // 设置内容类型为JSON，并指定字符编码
                try {
                    httpServletResponse.getWriter().write(e + "\n\n");
                    httpServletResponse.flushBuffer();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

                    // 获取InputStream，进行流式处理
                    InputStream inputStream = responseBody.byteStream();
                    // 这里可以边读边处理数据，例如：
                    byte[] buffer = new byte[2048];
                    int read;
                    StringBuilder sb = new StringBuilder();
                    while ((read = inputStream.read(buffer)) != -1) {
                        // 将读取的数据转换为字符串并累积
                        String str = new String(buffer, 0, read, StandardCharsets.UTF_8);
                        sb.append(str);
                        // 检查并处理所有完整的消息
                        int end;
                        while ((end = sb.indexOf("\n\n")) != -1) { // 假设每个消息后都有两个换行符
                            String message = sb.substring(0, end).trim();
                            if (message.equals("[DONE]")) {
                                countDownLatch.countDown();
                                return;
                            }
                            if (message.startsWith("data: ")) {
                                message = message.substring(5); // 移除"data: "前缀
                                // 处理消息
                                System.out.println("获取的流消息：" + message);
                                JSONObject jsonObject = JSONUtil.parseObj(message);
                                JSONObject firstChoice = jsonObject.getJSONArray("choices").getJSONObject(0);
                                JSONObject delta = firstChoice.getJSONObject("delta");
                                if (delta.toString().equals("{}")) {
                                    countDownLatch.countDown();
                                    return;
                                }
                                String content = delta.getStr("content");
                                content = content.replace("\n", "\\n").replace(" ", "\\s");
                                if (!content.equals("")) {
                                    httpServletResponse.setCharacterEncoding("UTF-8"); // 设置字符编码为UTF-8
                                    httpServletResponse.setContentType("application/json; charset=UTF-8"); // 设置内容类型为JSON，并指定字符编码
                                    httpServletResponse.getWriter().write(content + "\n\n");
                                    httpServletResponse.flushBuffer();
                                }
                                System.out.println("回复的内容:" + content);
                            }
                            // 从StringBuilder中移除已处理的消息
                            sb.delete(0, end + 2);
                        }
                    }
                    countDownLatch.countDown();
                }
            }
        });
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
            httpServletResponse.setCharacterEncoding("UTF-8"); // 设置字符编码为UTF-8
            httpServletResponse.setContentType("application/json; charset=UTF-8"); // 设置内容类型为JSON，并指定字符编码
            try {
                httpServletResponse.getWriter().write(e + "\n\n");
                httpServletResponse.flushBuffer();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }

        }
    }

    /**
     * 字符串转JSON
     *
     * @param str
     * @return
     */
    public String strToJson(String str) {
        try {
            throw new IOException("This is an IOException example.");
        } catch (IOException e) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                String json = mapper.writeValueAsString(e);
                System.out.println(json);
                return json;
            } catch (JsonProcessingException ex) {
                ex.printStackTrace();
                return str;
            }
        }
    }

    public String imageRequest(MessageVo messageVo, String globalSetJson) throws IOException {
        System.out.println("消息最终处理接收内容:" + messageVo.getContent() + ",接受的模型名:" + messageVo.getModel());
        JSONObject globalJson = JSONUtil.parseObj(globalSetJson);
        String baseUrl = globalJson.getStr("baseUrl");
        String apiKey = globalJson.getStr("apiKey");
        //"\"response_format\": {\"type\": \"json_object\"}, " +   启用json模式  You are a helpful assistant designed to output JSON
        String messageContent = StringEscapeUtils.escapeHtml4(messageVo.getContent());
        String escapedContent = messageContent.replace("\n", "").replace("\"", "\\\"");
        JSONObject jsonObject = new JSONObject();
        jsonObject.putOnce("model", messageVo.getModel());
        jsonObject.putOnce("prompt", escapedContent);
        jsonObject.putOnce("size", "1024x1024");
        jsonObject.putOnce("n", 1);
        String imageJson = jsonObject.toStringPretty();
//        String imageJson = "{\"model\": \"" + messageVo.getModel() + "\", " +
//                // "{\"role\": \"system\", \"content\": \"You are a helpful assistant.\"}," +
//                "\"prompt\": \"" + escapedContent + "\", " +
//                "\"size\": \"" + "1024*1024\", " +
//                "\"n\": " + "1" +
//                "}";
        baseUrl = baseUrl.equals("") ? appConfigService.getUrl() : baseUrl;
        apiKey = apiKey.equals("") ? appConfigService.getApiKey() : apiKey;
        System.out.println("图片请求参数" + imageJson);
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Authorization", "Bearer " + apiKey);
        //本地代理
        String url = baseUrl + appConfigService.getImageUri();
        System.out.println("url:" + url);
        int connectionTimeout = 60 * 1000 * 5;
        int readTimeout = 60 * 1000 * 5;

        try {
            HttpResponse response = HttpRequest.post(url)
                    .headerMap(headers, false)
                    .body(imageJson)
                    .timeout(connectionTimeout) // 设置连接超时时间
                    .setReadTimeout(readTimeout) // 设置读取超时时间
                    .execute();
            System.out.println("image response:" + response.body());
            return response.body();
        } catch (HttpException e) {
            return e.toString();
        }
    }

    public static String postRequest(String url, String params, String token) {
        Response response = null;
        try {
            OkHttpClient client = new OkHttpClient().newBuilder()
                    .build();
            MediaType mediaType = MediaType.parse("application/json");
            RequestBody body = RequestBody.create(mediaType, params);
            Request request = new Request.Builder()
                    .url(url)
                    .method("POST", body)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", "Bearer " + token)
                    .addHeader("Accept", "*/*")
                    .addHeader("Connection", "keep-alive")
                    .build();
            response = client.newCall(request).execute();
            System.out.println(response.toString());
            if (response.isSuccessful()) {
                return response.body().string();
            }
        } catch (IOException e) {
            log.error("生成标题请求异常:{}", e);
            return null;

        }
        return null;
    }

    public byte[] fetchAudioDataFromService(String message, String selectVoice) {
        OkHttpClient client = new OkHttpClient().newBuilder().build();
        String messageContent = StringEscapeUtils.escapeHtml4(message);
        String escapedContent = messageContent.replace("\n", "").replace("\"", "\\\"");
        JSONObject jsonObject = new JSONObject();
        jsonObject.putOnce("model", "tts-1");
        jsonObject.putOnce("input", escapedContent);
        jsonObject.putOnce("voice", selectVoice);
        jsonObject.putOnce("response_format", "opus");
        String stringPretty = jsonObject.toStringPretty();

        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType, stringPretty);
        Request request = new Request.Builder()
                .url(appConfigService.getAduioOaiUrl() + appConfigService.getAudioOaiSpeeUri())
                .method("POST", body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + appConfigService.getAudioOaiApiKey())
                .build();

        try (Response response = client.newCall(request).execute()) {
            log.info(String.valueOf(response.isSuccessful()));
            if (response.isSuccessful() && response.body() != null) {
                return response.body().bytes();
            }
        } catch (IOException e) {
            log.error("音频数据获取异常:{}", e.getMessage());
        }
        return null;
    }

    public String search(String value, Integer maxResults) {
        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();

        Request request = new Request.Builder()
                .url("https://search.qipusong.site/search?q=" + value + "&max_results=" + maxResults)
                .get() // 使用.get()方法代替.method("GET", body)
                .addHeader("Accept", "*/*")
                .addHeader("Connection", "keep-alive")
                .build();

        try {
            Response response = client.newCall(request).execute();
            String responseBody = response.body().string();
            System.out.println(responseBody);
            String parseAndPrintJson = parseAndPrintJson(responseBody);

            return parseAndPrintJson;
        } catch (IOException e) {
            log.error("搜索请求异常:{}", e.getMessage());
        }
        return null;
    }

    private static String parseAndPrintJson(String responseBody) {
        JSONObject newJson = new JSONObject();
        JSONArray jsonArray = new JSONArray();

        JSONObject jsonObject = new JSONObject(responseBody);
        JSONArray results = jsonObject.getJSONArray("results");

        for (int i = 0; i < results.size(); i++) {
            JSONObject result = results.getJSONObject(i);
            String body = result.getStr("body");
            String href = result.getStr("href");
            String title = result.getStr("title");

            JSONObject resultJson = new JSONObject();
            resultJson.put("Title", decodeUnicode(title));
            resultJson.put("Body", decodeUnicode(body));
            resultJson.put("Href", href);

            jsonArray.put(resultJson);
        }

        newJson.put("results", jsonArray);
        return newJson.toStringPretty();
    }

    private static String decodeUnicode(String str) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < str.length()) {
            char c = str.charAt(i++);
            if (c == '\\' && i < str.length()) {
                c = str.charAt(i++);
                if (c == 'u' && i + 4 <= str.length()) {
                    char unicodeChar = (char) Integer.parseInt(str.substring(i, i + 4), 16);
                    sb.append(unicodeChar);
                    i += 4;
                } else {
                    sb.append('\\').append(c);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}





