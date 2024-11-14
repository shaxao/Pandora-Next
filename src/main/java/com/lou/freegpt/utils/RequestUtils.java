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
     * @param user
     * @param model
     * @param fileEncoder
     *
     * @return
     * @throws IOException
     */
    public void textRequest(String user, String model,HttpServletResponse httpServletResponse, String fileEncoder) throws IOException {
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
        String otherJson = "{\"api_key\": \""+  appConfigService.getApiKey() + "\"," +
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
        if(model.equals(ModelEnum.PERSON_ONE.getModels()) || model.equals(ModelEnum.PERSON_TWO.getModels())){
            requestJson = otherJson;
        }else {
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
        }catch (Exception e) {
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


    public void streamRequestTest(MessageVo messageVo, HttpServletRequest request, HttpServletResponse httpServletResponse, String fileEncoder, String globalSet){
        JSONObject jsonObject = JSONUtil.parseObj(globalSet);
        double tem = jsonObject.getBigDecimal("tempertaure").doubleValue();
        double top = jsonObject.getBigDecimal("topP").doubleValue();
        int maxTokens = jsonObject.getInt("maxTokens");
        double pre = jsonObject.getBigDecimal("presencePenalty").doubleValue();
        double fre = jsonObject.getBigDecimal("frequencyPenalty").doubleValue();
        String baseUrl = jsonObject.getStr("baseUrl");
        String apiKey = jsonObject.getStr("apiKey");
        String webProxyUrl = jsonObject.getStr("webProxyUrl");
        String accessToken = jsonObject.getStr("accessToken");
        // System.out.println("acctoken" + accessToken);
        String textJson = "";
        String content = "你是一个经过优秀训练并且具备思考能力的人工智能大模型，你会积极的、主动的、引导性的、耐心的回答用户的问题";
        String responseBody = "";
        //"\"response_format\": {\"type\": \"json_object\"}, " +   启用json模式  You are a helpful assistant designed to output JSON
        // 替换content中的换行符和双引号
        HttpSession session = request.getSession();
        String sessionTalk = (String) session.getAttribute(messageVo.getConversationId());
        sessionTalk = sessionTalk == null ? "" : sessionTalk;
        int tokenize = TokenUtils.tokenize(sessionTalk + messageVo.getContent());
        if(tokenize >= maxTokens) {
            sessionTalk = "";
            session.setAttribute(messageVo.getConversationId(), "");
        }
        // System.out.println("sessionTalk: " + sessionTalk);
        String messageContent = StringEscapeUtils.escapeHtml4(sessionTalk + "\n" + messageVo.getContent());
        String escapedContent = messageContent.replace("\n", "").replace("\"", "\\\"");
        if(messageVo.getIsSearch() != null && messageVo.getIsSearch().booleanValue()) {
            escapedContent =  escapedContent + "，结合下述搜索信息回复消息: " + search(messageVo.getContent(), 5);
        }

        System.out.println("escapedContent : " + escapedContent );
        final String[] messageId = {UUID.randomUUID().toString()};
        final String[] conversationId = {"", ""};
        final int[] reqLen = {0};
        if(messageVo.getModel() != null || messageVo.getModel().startsWith(ModelEnum.OPENAI_BASE.getModels())) {
            List<String> list = Arrays.asList("gpt-4", "gpt-4o");
            if(webProxyUrl != null && !webProxyUrl.equals("") && list.contains(messageVo.getModel())) {
                apiKey = accessToken;
                if(messageVo.getFirstFlag().booleanValue()) {
                    JSONObject json = new JSONObject();
                    json.put("action", "next");

                    JSONArray messages = new JSONArray();

                    JSONObject message = new JSONObject();
                    message.put("id", messageVo.getMessageId());
                    JSONObject author = new JSONObject();
                    author.put("role", "user");
                    message.put("author", author);
                    JSONObject contentObject = new JSONObject();
                    contentObject.put("content_type", messageVo.getContentType());
                    JSONArray parts = new JSONArray();
                    parts.add(escapedContent);
                    contentObject.put("parts", parts);
                    message.put("content", contentObject);
                    JSONObject metadata = new JSONObject();
                    metadata.put("is_starter_prompt", messageVo.getFirstFlag());
                    message.put("metadata", metadata);

                    messages.add(message);

                    json.put("messages", messages);
                    json.put("parent_message_id", messageVo.getMessageId());
                    json.put("model", messageVo.getModel());
                    json.put("timezone_offset_min", -480);
                    JSONArray suggestions = new JSONArray();
                    suggestions.add("我刚搬到一个新城市，希望结交些朋友。你能推荐一些有趣的活动来帮助我实现这个目标吗？");
                    suggestions.add("帮助我学习词汇：写一道填空题，然后我来尝试选择正确的选项。");
                    suggestions.add("我的朋友今天心情很糟糕，我想鼓励他/她一下。你能提供几个温馨的句子，并搭配一张小猫的动图吗？");
                    suggestions.add("我计划去首尔玩 4 天。你能提供不涉及热门旅游景点的行程安排吗？");
                    json.put("suggestions", suggestions);
                    json.put("history_and_training_disabled", false);
                    JSONObject conversationMode = new JSONObject();
                    conversationMode.put("kind", "primary_assistant");
                    json.put("conversation_mode", conversationMode);
                    json.put("force_paragen", false);
                    json.put("force_paragen_model_slug", "");
                    json.put("force_nulligen", false);
                    json.put("force_rate_limit", false);
                    json.put("reset_rate_limits", false);
                    json.put("websocket_request_id", UUID.randomUUID().toString());
                    json.put("force_use_sse", true);

                    textJson = json.toStringPretty();
                } else {
                    JSONObject json = new JSONObject();
                    json.put("action", "next");

                    JSONArray messages = new JSONArray();

                    JSONObject message = new JSONObject();
                    message.put("id", messageVo.getMessageId());
                    JSONObject author = new JSONObject();
                    author.put("role", "user");
                    message.put("author", author);
                    JSONObject contentObject = new JSONObject();
                    contentObject.put("content_type", messageVo.getContentType());
                    JSONArray parts = new JSONArray();
                    parts.add(escapedContent);
                    contentObject.put("parts", parts);
                    message.put("content", contentObject);
                    message.put("metadata", new JSONObject());

                    messages.add(message);

                    json.put("messages", messages);
                    json.put("parent_message_id", messageVo.getMessageId());
                    json.put("conversation_id", messageVo.getConversationId());
                    json.put("model", messageVo.getModel());
                    json.put("timezone_offset_min", -480);
                    json.put("history_and_training_disabled", false);
                    JSONObject conversationMode = new JSONObject();
                    conversationMode.put("kind", "primary_assistant");
                    json.put("conversation_mode", conversationMode);
                    json.put("force_paragen", false);
                    json.put("force_paragen_model_slug", "");
                    json.put("force_nulligen", false);
                    json.put("force_rate_limit", false);
                    json.put("reset_rate_limits", false);
                    json.put("websocket_request_id", UUID.randomUUID().toString());
                    json.put("force_use_sse", true);

                    textJson = json.toStringPretty();
                }

            }else {
                JSONObject json = new JSONObject();
                String model = messageVo.getGizmo().equals("") ? messageVo.getModel() : messageVo.getGizmo();
                json.put("model", model);
                // 创建 messages 数组
                JSONArray messages = new JSONArray();

               // 创建 system 消息对象并添加到数组
                JSONObject systemMessage = new JSONObject();
                systemMessage.put("role", "system");
                systemMessage.put("content", content);
                messages.add(systemMessage);

                // 创建 user 消息对象并添加到数组
                JSONObject userMessage = new JSONObject();
                userMessage.put("role", "user");
                userMessage.put("content", escapedContent);
                messages.add(userMessage);

               // 将 messages 数组添加到主 JSON 对象
                json.put("messages", messages);

               // 添加其他属性
                json.put("stream", appConfigService.getStream());
                json.put("temperature", tem);
                json.put("top_p", top);
                json.put("max_tokens", maxTokens);
                textJson = json.toString();
            }

        }
        // System.out.println("textJson:" + textJson);
        JSONObject imageVersionJson = new JSONObject();
        imageVersionJson.put("model", "gpt-4-vision-preview");

        JSONArray messagesArray = new JSONArray();

        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");

        JSONArray contentArray = new JSONArray();

        JSONObject textContent = new JSONObject();
        textContent.put("type", "text");
        textContent.put("text", escapedContent);
        contentArray.put(textContent);

        JSONObject imageUrlContent = new JSONObject();
        imageUrlContent.put("type", "image_url");

        JSONObject imageUrl = new JSONObject();
        imageUrl.put("url", "data:image/jpeg;base64," + fileEncoder);
        imageUrlContent.put("image_url", imageUrl);

        contentArray.put(imageUrlContent);

        userMessage.put("content", contentArray);
        messagesArray.put(userMessage);

        imageVersionJson.put("messages", messagesArray);
        //本地代理
        String url = webProxyUrl == "" ? baseUrl + appConfigService.getOpenTextUri() : webProxyUrl + "/backend-api/conversation";
        if(baseUrl.equals("") && webProxyUrl.equals("")) {
            url =  appConfigService.getUrl() + appConfigService.getOpenTextUri();
            apiKey = appConfigService.getApiKey();
        }
        System.out.println("url:" + url);
        String requestJson = "";
        if(messageVo.getModel().equals(ModelEnum.GPT_VERSION.getModels())){
            requestJson = imageVersionJson.toStringPretty();
        }else {
            requestJson = textJson;
        }
        System.out.println("requestJson:" + requestJson);
        // System.out.println(apiKey);
        // String proxyHost = "127.0.0.1";
        // int proxyPort = 7897;

        // 创建代理
        // Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
        OkHttpClient client = new OkHttpClient().newBuilder()
                .connectTimeout(90, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();

        // 创建请求体
        RequestBody requestBody = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"),
                requestJson

        );
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        Request apirequest = new Request.Builder()
                .url(url)
                .method("POST", requestBody)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Accept", "*/*")
                .addHeader("Connection", "keep-alive")
                .build();
        // System.out.println(request.toString());
        String finalApiKey = apiKey;
        String finalSessionTalk = sessionTalk;
        client.newCall(apirequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                System.out.println("异常处理");
                e.printStackTrace();
                errorResponse(e,httpServletResponse, messageVo);
            }

            @Override
            public void onResponse(Call call, Response response)  {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful()){
                        // 处理非成功响应，例如401错误
                        String errorMessage = response.body().string();
//                        System.out.println("HTTP错误: " + response.code() + ", 错误信息: " + errorMessage);
                        JSONObject jsonObject1 = JSONUtil.parseObj(errorMessage);
                        jsonObject1.putOnce("messageId", messageVo.getMessageId());
                        String errorStr = jsonObject1.toStringPretty();
                        httpServletResponse.setCharacterEncoding("UTF-8"); // 设置字符编码为UTF-8
                        httpServletResponse.setContentType("application/json; charset=UTF-8"); // 设置内容类型为JSON，并指定字符编码
                        httpServletResponse.getWriter().write("error:" + errorStr + "\n\n");
                        httpServletResponse.flushBuffer();
                        countDownLatch.countDown();
                    }else {
                        // 获取InputStream，进行流式处理
                        InputStream inputStream = responseBody.byteStream();
                        // 这里可以边读边处理数据，例如：
                        byte[] buffer = new byte[2048];
                        int read;
                        StringBuilder sb = new StringBuilder();
                        StringBuilder parts = new StringBuilder();
                        List<String> messageList = new ArrayList<>();
                        while ((read = inputStream.read(buffer)) != -1) {
                            // 将读取的数据转换为字符串并累积
                            String str = new String(buffer, 0, read, StandardCharsets.UTF_8);
                            sb.append(str);
                            // 检查并处理所有完整的消息
                            int end;
                            while ((end = sb.indexOf("\n\n")) != -1) { // 假设每个消息后都有两个换行符
                                String message = sb.substring(0, end).trim();
                                System.out.println("message:" + message);

                                if (message.startsWith("data: ")) {
                                    message = message.substring(5).trim(); // 移除"data: "前缀
                                    // 处理消息
                                    if (message.equals("[DONE]")) {
                                        // System.out.println("done: " + parts.toString());
                                        session.setAttribute(messageVo.getConversationId(), finalSessionTalk + "\n" + messageVo.getContent() + "\n" + parts.toString());
                                        if (webProxyUrl == null || webProxyUrl.equals("")) {
//                                        MessageVo messageVo1 = new MessageVo();
//                                        BeanUtils.copyProperties(messageVo,messageVo1);
//                                        messageVo1.setContent(parts.toString());
//                                        messageVo1.setParentId(messageVo.getMessageId());
//                                        messageVo1.setMessageId(Arrays.toString(messageId));
//                                        messageDao.insertMessage(messageVo1, "user", messageVo.getContentType());
                                            if (messageVo.getFirstFlag().booleanValue()) {
                                                messageExecute.executeFinishMessage(messageVo, parts.toString(), messageId[0], httpServletResponse);
                                            } else {
                                                messageExecute.executeFinishMessage(messageVo, parts.toString(), messageVo.getConversationId(), messageId[0], httpServletResponse);
                                            }
                                            countDownLatch.countDown();
                                            return;
                                        }
                                        String lastMessage = messageList.get(messageList.size() - 1);
                                        // System.out.println(lastMessage);
                                        JSONObject jsonObject = JSONUtil.parseObj(lastMessage);
                                        JSONObject message1 = jsonObject.getJSONObject("message");
                                        JSONObject metadatas = message1.getJSONObject("metadata");
                                        JSONArray citations = metadatas.getJSONArray("citations");
                                        if (citations != null && citations.size() > 0) {
                                            for (int i = 0; i < citations.size(); i++) {
                                                JSONObject citation = citations.getJSONObject(i);
                                                JSONObject metadata = citation.getJSONObject("metadata");
                                                String title = metadata.getStr("title");
                                                String url = metadata.getStr("url");
                                                JSONObject extra = metadata.getJSONObject("extra");
                                                int citedMessageIdx = extra.getInt("cited_message_idx");
                                                String result = "\\n<p>[" + citedMessageIdx + "†source]: <a href=\"" + url + "\">" + title + "</a></p>";
                                                System.out.println(result);
                                                httpServletResponse.setCharacterEncoding("UTF-8"); // 设置字符编码为UTF-8
                                                httpServletResponse.setContentType("application/json; charset=UTF-8"); // 设置内容类型为JSON，并指定字符编码
                                                httpServletResponse.getWriter().write(result + "\n\n");
                                                httpServletResponse.flushBuffer();
                                            }
                                        }
                                        if (messageVo.getFirstFlag().booleanValue()) {
                                            messageExecute.executeFinishMessage(messageVo, sb.toString(), conversationId[0], conversationId[1], httpServletResponse);
                                        } else {
                                            messageExecute.executeFinishMessage(messageVo, sb.toString(), conversationId[1], httpServletResponse);
                                        }

                                        countDownLatch.countDown();
                                        return;
                                    }
                                    System.out.println("获取的流消息：" + message);
                                    String content = "";
                                    JSONObject jsonObject = JSONUtil.parseObj(message);
                                    if (webProxyUrl != null && !webProxyUrl.equals("")) {
                                        String role = jsonObject.getJSONObject("message").getJSONObject("author").getStr("role");
                                        if (role.equals("user")) {
                                            sb.delete(0, end + 2);
                                            continue;
                                        }
                                        JSONArray jsonArray = jsonObject.getJSONObject("message").getJSONObject("content").getJSONArray("parts");
                                        if (jsonArray == null) {
                                            sb.delete(0, end + 2);
                                            continue;
                                        }
                                        content = jsonArray.getStr(0);
                                        System.out.println(content.length());
                                        if (content.length() == 0) {
                                            sb.delete(0, end + 2);
                                            continue;
                                        }
                                        int i = reqLen[0];
                                        reqLen[0] = content.length();
                                        messageList.add(message);
                                        System.out.println("分割前内容：" + content);
                                        content = content.substring(i);
                                        content = content.replace("\n", "\\n").replace(" ", "\\s");
                                        conversationId[1] = jsonObject.getJSONObject("message").getStr("id");
                                        if (messageVo.getFirstFlag().booleanValue()) {
                                            conversationId[0] = jsonObject.getStr("conversation_id");
                                        }

                                    } else {
                                        JSONObject firstChoice = jsonObject.getJSONArray("choices").getJSONObject(0);
                                        JSONObject delta = firstChoice.getJSONObject("delta");
//                                    if(delta.toString().equals("{}")){
//                                        System.out.println("jeishu: " + parts.toString());
//                                        MessageVo messageVo1 = new MessageVo();
//                                        BeanUtils.copyProperties(messageVo,messageVo1);
//                                        messageVo1.setContent(parts.toString());
//                                        messageVo1.setParentId(messageVo.getMessageId());
//                                        messageVo1.setMessageId(Arrays.toString(messageId));
//                                        messageDao.insertMessage(messageVo1, "user", messageVo.getContentType());
//                                        messageExecute.executeFinishMessage(messageVo, sb.toString(), messageId[0], httpServletResponse);
//                                        countDownLatch.countDown();
//                                        return;
//                                    }
                                        content = delta.containsKey("content") ? delta.getStr("content") : "";
                                    }
                                    // String content = delta.getStr("content");
                                    content = content.replace("\n", "\\n").replace(" ", "\\s");
                                    if (!content.equals("")) {
                                        parts.append(content);
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
                    }
                } catch (IOException e) {
                    System.out.println("异常处理");
                    errorResponse(e, httpServletResponse, messageVo);
                } catch (JSONException e) {
                    System.out.println("json异常处理");
                    errorResponse(e, httpServletResponse, messageVo);
                }finally {
                    countDownLatch.countDown();
                }
            }
        });
        try {
           countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
            errorResponse(e,httpServletResponse, messageVo);
        }
    }

    public void errorResponse(Exception e,HttpServletResponse httpServletResponse, MessageVo messageVo){
        httpServletResponse.setCharacterEncoding("UTF-8"); // 设置字符编码为UTF-8
        httpServletResponse.setContentType("application/json; charset=UTF-8"); // 设置内容类型为JSON，并指定字符编码
        Map<String, String> errorDetails = new HashMap<>();
        errorDetails.put("messageId", messageVo.getMessageId());
        errorDetails.put("errorType", e.getClass().getSimpleName());
        errorDetails.put("errorMessage", e.getMessage());
        String errStr = JSONUtil.toJsonStr(errorDetails);
//        MessageVo messageVo1 = new MessageVo();
//        messageVo1.setParentId(messageVo.getMessageId());
//        messageVo1.setModel(messageVo.getModel());
//        messageVo1.setContent(e.getMessage());
//        messageVo1.setConversationId(messageVo.getConversationId());
//        messageVo1.setMessageId(messageId);
//        messageDao.insertMessage(messageVo1, "system", "text");
        // 修改父消息的children
//        Map<String,String> params = new HashMap<>();
//        params.put("message.update_time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
//        params.put("message.status", "回复失败");
//        messageDao.updateById(messageVo.getMessageId(), params);
        try {
            httpServletResponse.getWriter().write("error:" + errStr + "\n\n");
            httpServletResponse.flushBuffer();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void streamRequest(String user, String model, HttpServletResponse httpServletResponse, String fileEncoder,String globalSet){
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
        System.out.println("message : " + message );
        String textJson = "{\"model\": \"" + model + "\", " +
                "\"messages\": [" +
                "{\"role\": \"system\", \"content\": \"" + escapedContent + "\"}," +
                "{\"role\": \"user\", \"content\": \"" + message  + "\"}" + "]," +
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
                + "        \"text\": \""+ message  +"\""
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
        if(model.equals(ModelEnum.GPT_VERSION.getModels())){
            requestJson = imageVersionJson;
        }else {
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
                            if(message.equals("[DONE]")){
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
                                if(delta.toString().equals("{}")){
                                    countDownLatch.countDown();
                                    return;
                                }
                                String content = delta.getStr("content");
                                content = content.replace("\n", "\\n").replace(" ", "\\s");
                                if(!content.equals("")){
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
     * @param str
     * @return
     */
    public String strToJson(String str){
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
        }catch (HttpException e){
              return e.toString();
        }
    }

    public static String postRequest(String url,String params, String token){
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
            if(response.isSuccessful()) {
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

//    public static void main(String[] args) {
////        JSONObject jsonObject = new JSONObject();
////        jsonObject.putOnce("model", "tts-1");
////        jsonObject.putOnce("input", "我就是神");
////        jsonObject.putOnce("voice", "shimmer");
////        jsonObject.putOnce("response_format", "opus");
////        String stringPretty = jsonObject.toStringPretty();
////        String url = "https://api.oaifree.com/v1/audio/speech";
////        String apikey = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6Ik1UaEVOVUpHTkVNMVFURTRNMEZCTWpkQ05UZzVNRFUxUlRVd1FVSkRNRU13UmtGRVFrRXpSZyJ9.eyJzZXNzaW9uX2lkIjoiSl9nN0RaNjc3dzVib1NUYUtnNW44UEJYcHh4NlFNTC0iLCJodHRwczovL2FwaS5vcGVuYWkuY29tL3Byb2ZpbGUiOnsiZW1haWwiOiJla2VsbGVyMUBnbXUuZWR1IiwiZW1haWxfdmVyaWZpZWQiOnRydWV9LCJodHRwczovL2FwaS5vcGVuYWkuY29tL2F1dGgiOnsicG9pZCI6Im9yZy1pTDQyZUFGN3RzSURqSE9EZEQzcW92ZXoiLCJ1c2VyX2lkIjoidXNlci11QnVoOGNHS0doNkVsT2xVSGFOeERoUUIifSwiaXNzIjoiaHR0cHM6Ly9hdXRoMC5vcGVuYWkuY29tLyIsInN1YiI6ImF1dGgwfDYzZjdlODgzMTRiNzUzM2Q0YjQyOWRlMyIsImF1ZCI6WyJodHRwczovL2FwaS5vcGVuYWkuY29tL3YxIiwiaHR0cHM6Ly9vcGVuYWkub3BlbmFpLmF1dGgwYXBwLmNvbS91c2VyaW5mbyJdLCJpYXQiOjE3MTcyNDg4NjUsImV4cCI6MTcxODExMjg2NSwic2NvcGUiOiJvcGVuaWQgcHJvZmlsZSBlbWFpbCBtb2RlbC5yZWFkIG1vZGVsLnJlcXVlc3Qgb3JnYW5pemF0aW9uLnJlYWQgb2ZmbGluZV9hY2Nlc3MiLCJhenAiOiJwZGxMSVgyWTcyTUlsMnJoTGhURTlWVjliTjkwNWtCaCJ9.GM7-eLaomrCr7I-MXpK7gGpzId9HE0eGrYPDYEcrN3jJouL5aZrXYOBO27ML_uQsh5RbEIuQJSAszGY2Apmx3-PmOAHg1RxZRBMG1oyRbINDs2drjMH_fmu5Sfe2t8i31sgVVmEb7WcVjuo2y0bBZqO7-NrhfHC9-wxu4s8OBE299-Pk_8DCsloOWW53dVFDgYBhJHrCXmOJxgZT6ucGjPe2IbBjyFHTf8CJ3-yHg2Zs7NSWLmf1nfDUYiA5xNpFYBiA9bLj0lPbXlHzRClOpX-0jPWB-0levO7sid-WHYFAcMKqwJp5d9ZOLexCInklOPprG5LziCdoUYxGhmn9Rw";
////
////        String string = postRequest(url, stringPretty, apikey);
////        System.out.println(string);
//        String responseJson = "{\"results\":[{\"body\":\"UN News produces daily news content in Arabic, Chinese, English, French, Kiswahili, Portuguese, Russian, Spanish, Hindi and Urdu. Our multimedia service, through this new integrated single platform, updates throughout the day, in text, audio and video - also making use of quality images and other media from across the UN system. We produce news features and analyses on a variety of social ...\",\"href\":\"https://news.un.org/zh/news\",\"title\":\"\\u6700\\u65b0\\u6d88\\u606f | 1 \\u8054\\u5408\\u56fd\\u65b0\\u95fb - UN News\"},{\"body\":\"\\u5979\\u544a\\u8bc9\\u534a\\u5c9b\\u7535\\u89c6\\u53f0\\uff1a\\\"\\u6211\\u4eec\\u8bb0\\u5f55\\u4e86\\u9020\\u6210100\\u591a\\u4eba\\u6b7b\\u4ea1\\u7684\\u88ad\\u51fb\\u4e8b\\u4ef6\\uff0c\\u4f46\\u8fd9\\u4e00\\u6d88\\u606f\\u51e0\\u4e4e\\u6ca1\\u6709\\u767b\\u4e0a\\u56fd\\u9645\\u5934\\u6761\\u65b0\\u95fb\\u3002 \\u8054\\u5408\\u56fd\\u5df4\\u52d2\\u65af\\u5766\\u88ab\\u5360\\u9886\\u571f\\u95ee\\u9898\\u7279\\u522b\\u62a5\\u544a\\u5458\\u5f17\\u6717\\u897f\\u65af\\u5361\\u00b7\\u963f\\u5c14\\u5df4\\u5185\\u585e\\u5728\\u4e09\\u6708\\u4e0b\\u65ec\\u53d1\\u8868\\u7684\\u4e00\\u4efd\\u62a5\\u544a\\u4e2d\\u8868\\u793a\\uff0c\\u6709\\u660e\\u663e\\u8ff9\\u8c61\\u8868\\u660e\\uff0c\\u4ee5\\u8272\\u5217\\u8fdd\\u53cd\\u4e86\\u300a\\u8054\\u5408\\u56fd\\u706d\\u7edd\\u79cd\\u65cf\\u7f6a ...\",\"href\":\"https://chinese.aljazeera.net/palestine-israel-conflict/2024/4/24/\\u4ee5\\u8272\\u5217\\u5bf9\\u52a0\\u6c99\\u6218\\u4e89\\u5df2\\u6301\\u7eed200\\u5929\",\"title\":\"\\u4ee5\\u8272\\u5217\\u5bf9\\u52a0\\u6c99\\u6218\\u4e89\\u5df2\\u6301\\u7eed200\\u5929 | \\u4ee5\\u8272\\u5217\\u5bf9\\u52a0\\u6c99\\u6218\\u4e89 \\u65b0\\u95fb | \\u534a\\u5c9b\\u7535\\u89c6\\u53f0\"},{\"body\":\"\\u73af\\u7403\\u7f51\\u56fd\\u9645\\u65b0\\u95fb\\u9891\\u9053\\u4e3a\\u60a8\\u63d0\\u4f9b\\u6700\\u65b0\\u7684\\u56fd\\u9645\\u6d88\\u606f\\u548c\\u70ed\\u70b9\\u8bdd\\u9898\\uff0c\\u5305\\u62ec\\u73af\\u7403\\u8981\\u95fb\\u3001\\u4eca\\u65e5\\u63a8\\u8350\\u3001\\u7cbe\\u5f69\\u4e13\\u9898\\u3001\\u70ed\\u95e8\\u65b0\\u95fb\\u7b49\\u3002\\u60a8\\u53ef\\u4ee5\\u4e86\\u89e3\\u4e2d\\u65b9\\u547c\\u5401\\u5bf9\\\"\\u5317\\u6eaa\\\"\\u7ba1\\u9053\\u7206\\u70b8\\u4e8b\\u4ef6\\u542f\\u52a8\\u56fd\\u9645\\u8c03\\u67e5\\u3001\\u6bd4\\u5c14\\u00b7\\u76d6\\u8328\\u4eba\\u6c11\\u65e5\\u62a5\\u64b0\\u6587\\u3001\\u7f8e\\u5a92\\u62dc\\u767b\\u6539\\u53d8\\u5f80\\u8fd4\\u4e13\\u673a\\u4e0e\\u767d\\u5bab\\u529e\\u516c\\u5ba4\\u4e60\\u60ef\\u7b49\\u56fd\\u9645\\u65b0\\u95fb\\u3002\",\"href\":\"https://world.huanqiu.com/\",\"title\":\"\\u56fd\\u9645\\u65b0\\u95fb_\\u73af\\u7403\\u7f51\"},{\"body\":\"100\\u591a\\u4f4d\\u5168\\u7403\\u9886\\u5bfc\\u4eba\\u547c\\u5401\\u5404\\u56fd\\u653f\\u5e9c\\u7d27\\u6025\\u8fbe\\u6210\\u5171\\u8bc6\\uff0c\\u4e3a\\u9632\\u8303\\u548c\\u9884\\u9632\\u672a\\u6765\\u7684\\u5927\\u6d41\\u884c\\u75ab\\u60c5\\u5546\\u5b9a\\u5b8f\\u4f1f\\u800c\\u516c\\u5e73\\u7684\\u56fd\\u9645\\u534f\\u5b9a. 2024\\u5e743\\u670820\\u65e5. \\u65b0\\u95fb\\u7a3f. \\u4eca\\u5929\\uff0c23\\u4f4d\\u524d\\u603b\\u7edf\\u300122\\u4f4d\\u524d\\u603b\\u7406\\u30011\\u4f4d\\u8054\\u5408\\u56fd\\u524d\\u79d8\\u4e66\\u957f\\u548c3\\u4f4d\\u8bfa\\u8d1d\\u5c14\\u5956\\u83b7\\u5f97\\u8005\\u53d1\\u8868\\u9ad8\\u7ea7\\u522b\\u58f0\\u660e\\uff0c\\u6566\\u4fc3\\u56fd\\u9645\\u8c08\\u5224\\u4ee3\\u8868\\u6839\\u636e\\u300a\\u4e16\\u754c\\u536b\\u751f\\u7ec4\\u7ec7 ...\",\"href\":\"https://www.who.int/zh/news/item/20-03-2024-call-for-urgent-agreement-on-international-deal-to-prepare-for-and-prevent-future-pandemics\",\"title\":\"\\u547c\\u5401\\u5c31\\u56fd\\u9645\\u534f\\u5b9a\\u7d27\\u6025\\u8fbe\\u6210\\u5171\\u8bc6\\uff0c\\u4ee5\\u9632\\u8303\\u548c\\u9884\\u9632\\u672a\\u6765\\u7684\\u5927\\u6d41\\u884c\\u75ab\\u60c5\"},{\"body\":\"\\u4ee5\\u5df4\\u51b2\\u7a81\\uff1a\\u54c8\\u9a6c\\u65af\\u53d1\\u52a8\\u7a81\\u88ad\\uff0c\\u4ee5\\u8272\\u5217\\u5ba3\\u5e03\\u8fdb\\u5165\\u6218\\u4e89\\u72b6\\u6001. \\u4ee5\\u5df4\\u7206\\u53d1\\u4e25\\u91cd\\u6b66\\u88c5\\u51b2\\u7a81\\u3002. \\u5df4\\u52d2\\u65af\\u5766\\u6b66\\u88c5\\u7ec4\\u7ec7\\u54c8\\u9a6c\\u65af\\uff08Hamas\\uff0910\\u67087\\u65e5\\u6e05\\u6668\\u6d3e\\u9063\\u6570\\u5341\\u540d\\u67aa\\u624b ...\",\"href\":\"https://www.bbc.com/zhongwen/simp/world-67038483\",\"title\":\"\\u4ee5\\u5df4\\u51b2\\u7a81\\uff1a\\u54c8\\u9a6c\\u65af\\u53d1\\u52a8\\u7a81\\u88ad\\uff0c\\u4ee5\\u8272\\u5217\\u5ba3\\u5e03\\u8fdb\\u5165\\u6218\\u4e89\\u72b6\\u6001 - BBC News \\u4e2d\\u6587\"}]}\n";
//        String string = parseAndPrintJson(responseJson);
//        System.out.println(string);
//    }



//    public static void main(String[] args) {
//        OkHttpClient client = new OkHttpClient().newBuilder()
//                .build();
//        MediaType mediaType = MediaType.parse("application/json");
//        RequestBody body = RequestBody.create(mediaType, "{\r\n  \"action\": \"next\",\r\n  \"messages\": [\r\n    {\r\n      \"id\": \"bfa90e5d-378b-43a7-946d-6260b8f04fa8\",\r\n      \"author\": {\r\n        \"role\": \"user\"\r\n      },\r\n      \"content\": {\r\n        \"content_type\": \"text\",\r\n        \"parts\": [\r\n          \"我计划去首尔玩 4 天。你能提供不涉及热门旅游景点的行程安排吗？\"\r\n        ]\r\n      },\r\n      \"metadata\": {\r\n        \"is_starter_prompt\": true\r\n      }\r\n    }\r\n  ],\r\n  \"parent_message_id\": \"fb42b64e-ae25-4065-98ce-ce3432a186b8\",\r\n  \"model\": \"gpt-4o\",\r\n  \"timezone_offset_min\": -480,\r\n  \"suggestions\": [\r\n    \"我刚搬到一个新城市，希望结交些朋友。你能推荐一些有趣的活动来帮助我实现这个目标吗？\",\r\n    \"帮助我学习词汇：写一道填空题，然后我来尝试选择正确的选项。\",\r\n    \"我的朋友今天心情很糟糕，我想鼓励他/她一下。你能提供几个温馨的句子，并搭配一张小猫的动图吗？\",\r\n    \"我计划去首尔玩 4 天。你能提供不涉及热门旅游景点的行程安排吗？\"\r\n  ],\r\n  \"history_and_training_disabled\": false,\r\n  \"conversation_mode\": {\r\n    \"kind\": \"primary_assistant\"\r\n  },\r\n  \"force_paragen\": false,\r\n  \"force_paragen_model_slug\": \"\",\r\n  \"force_nulligen\": false,\r\n  \"force_rate_limit\": false,\r\n  \"reset_rate_limits\": false,\r\n  \"websocket_request_id\": \"5d7626ba-22da-4948-9dfd-34878aa07dca\",\r\n  \"force_use_sse\": true\r\n}");
//        Request request = new Request.Builder()
//                .url("https://chat.oaifree.com/dad04481-fa3f-494e-b90c-b822128073e5/backend-api/conversation")
//                .method("POST", body)
//                .addHeader("User-Agent", "Apifox/1.0.0 (https://apifox.com)")
//                .addHeader("Content-Type", "application/json")
//                .addHeader("Authorization", "Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6Ik1UaEVOVUpHTkVNMVFURTRNMEZCTWpkQ05UZzVNRFUxUlRVd1FVSkRNRU13UmtGRVFrRXpSZyJ9.eyJodHRwczovL2FwaS5vcGVuYWkuY29tL3Byb2ZpbGUiOnsiZW1haWwiOiJnYWJ5Y29zdGVhLjAxQGdtYWlsLmNvbSIsImVtYWlsX3ZlcmlmaWVkIjp0cnVlfSwiaHR0cHM6Ly9hcGkub3BlbmFpLmNvbS9hdXRoIjp7InBvaWQiOiJvcmctd0NiaTJYYkdyUEtvNkpUbVRyZGlZNlZMIiwidXNlcl9pZCI6InVzZXIteVRsYXY4RVJWTko5S2RTVFFIeG02a2tlIn0sImlzcyI6Imh0dHBzOi8vYXV0aDAub3BlbmFpLmNvbS8iLCJzdWIiOiJhdXRoMHw2M2I4NmRmMDM1ZTk3YTc5NDZkNmUzYmMiLCJhdWQiOlsiaHR0cHM6Ly9hcGkub3BlbmFpLmNvbS92MSIsImh0dHBzOi8vb3BlbmFpLm9wZW5haS5hdXRoMGFwcC5jb20vdXNlcmluZm8iXSwiaWF0IjoxNzE2NTAyMDAyLCJleHAiOjE3MTczNjYwMDIsInNjb3BlIjoib3BlbmlkIHByb2ZpbGUgZW1haWwgbW9kZWwucmVhZCBtb2RlbC5yZXF1ZXN0IG9yZ2FuaXphdGlvbi5yZWFkIG9mZmxpbmVfYWNjZXNzIiwiYXpwIjoicGRsTElYMlk3Mk1JbDJyaExoVEU5VlY5Yk45MDVrQmgifQ.bhvzdLWzFto6iBhbadtLKQxe2X8uh5WwI5a1pEgzNLZ7ureqrmNuwrXGX7S0ynmSHfK7KT9tkGw9kEpBrvYpE4VuXOu3fgyHGBPlFZZGrIDJ9Z8gDn9Xcps_H23nVHW-_Zf6UJG6ojRx7MQ6vgXm-dxbVgE2TsrS6RaJB1KnBX59jxKBHyl1UYUAbeKTvtzpMsObOB-LeRTfNpx-cHUhZYelJJCy0gSnVcY2eRVTJbYBDzkWv370goqaZ5MRZSsSFljDVQgXsAtr-oMUgxYmxHPvruCjoB2GpXOWB70gvxmcswRPaFdSkbfznyYjaeIxxxXqMhk40bGDPleQe-TCmQ")
//                .addHeader("Accept", "*/*")
//                .addHeader("Host", "chat.oaifree.com")
//                .addHeader("Connection", "keep-alive")
//                .build();
//        try {
//            Response response = client.newCall(request).execute();
//            String string = response.body().string();
//            System.out.println(string);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }

//    public static void main(String[] args) {
//        String json = "{\n" +
//                "    \"message\": {\n" +
//                "        \"id\": \"b297f4ac-40fb-445b-a97e-e1de2799a4da\",\n" +
//                "        \"author\": {\n" +
//                "            \"role\": \"assistant\",\n" +
//                "            \"name\": null,\n" +
//                "            \"metadata\": {}\n" +
//                "        },\n" +
//                "        \"create_time\": 1716695790.509433,\n" +
//                "        \"update_time\": null,\n" +
//                "        \"content\": {\n" +
//                "            \"content_type\": \"text\",\n" +
//                "            \"parts\": [\n" +
//                "                \"当然可以！下面是一个用Java编写的冒泡排序（Bubble Sort）算法的代码示例：\\n\\n```java\\npublic class BubbleSort {\\n    public static void main(String[]\"\n" +
//                "            ]\n" +
//                "        },\n" +
//                "        \"status\": \"in_progress\",\n" +
//                "        \"end_turn\": null,\n" +
//                "        \"weight\": 1.0,\n" +
//                "        \"metadata\": {\n" +
//                "            \"citations\": [],\n" +
//                "            \"gizmo_id\": \"g-vpdGZagEo\",\n" +
//                "            \"message_type\": \"next\",\n" +
//                "            \"model_slug\": \"gpt-4o\",\n" +
//                "            \"default_model_slug\": \"gpt-4\",\n" +
//                "            \"pad\": \"AAAAA\",\n" +
//                "            \"parent_id\": \"ef7ff21f-77e9-4b06-b06b-b8abb3a408a0\",\n" +
//                "            \"model_switcher_deny\": [\n" +
//                "                {\n" +
//                "                    \"slug\": \"text-davinci-002-render-sha\",\n" +
//                "                    \"context\": \"regenerate\",\n" +
//                "                    \"reason\": \"unsupported_gizmo\",\n" +
//                "                    \"description\": \"This GPT is not supported\"\n" +
//                "                }\n" +
//                "            ]\n" +
//                "        },\n" +
//                "        \"recipient\": \"all\"\n" +
//                "    },\n" +
//                "    \"conversation_id\": \"1782b67d-d737-4286-aa67-e6068a641274\",\n" +
//                "    \"error\": null\n" +
//                "}";
//        JSONObject jsonObject = JSONUtil.parseObj(json);
//        String object = String.valueOf(jsonObject.getJSONObject("message").getJSONObject("content").getJSONArray("parts").getStr(0));
////        String conversationId = jsonObject.getStr("conversation_id");
//        System.out.println("你好啊".substring(2));
//    }

//    public static void main(String[] args) {
//        // JSON 数据（示例）
//        String jsonString = "{\n" +
//                "    \"message\": {\n" +
//                "        \"id\": \"9c4a0057-7a3e-436f-830e-64f2c8dadded\",\n" +
//                "        \"author\": {\n" +
//                "            \"role\": \"assistant\",\n" +
//                "            \"name\": null,\n" +
//                "            \"metadata\": {}\n" +
//                "        },\n" +
//                "        \"create_time\": 1716728927.445164,\n" +
//                "        \"update_time\": null,\n" +
//                "        \"content\": {\n" +
//                "            \"content_type\": \"text\",\n" +
//                "            \"parts\": [\n" +
//                "                \"明天上海的天气预报如下：\\n\\n**温度**：最高温度约为24°C，最低温度约为18°C【6†source】【9†source】。\\n\\n**天气状况**：全天大部分时间为阴天，并有较强降雨的可能。预计降雨量约为12毫米【7†source】【9†source】。\\n\\n**风速**：东北风，风速大约为14公里每小时【9†source】。\\n\\n建议带好雨具并穿着适合阴雨天气的衣物。\\n\\n希望这些信息对您有帮助！\"\n" +
//                "            ]\n" +
//                "        },\n" +
//                "        \"status\": \"finished_successfully\",\n" +
//                "        \"end_turn\": true,\n" +
//                "        \"weight\": 1.0,\n" +
//                "        \"metadata\": {\n" +
//                "            \"citations\": [\n" +
//                "                {\n" +
//                "                    \"start_ix\": 42,\n" +
//                "                    \"end_ix\": 52,\n" +
//                "                    \"citation_format_type\": \"tether_og\",\n" +
//                "                    \"metadata\": {\n" +
//                "                        \"type\": \"webpage\",\n" +
//                "                        \"title\": \"Shanghai weather in May 2024 | Shanghai 14 day weather\",\n" +
//                "                        \"url\": \"https://www.weather25.com/asia/china/shanghai?page=month&month=May\",\n" +
//                "                        \"text\": \"\\n## The average weather in Shanghai in May\\n\\nThe temperatures in Shanghai in May are comfortable with low of 17°C and and high up to 26°C.\\n\\nYou can expect about 3 to 8 days of rain in Shanghai during the month of May. It’s a good idea to bring along your umbrella so that you don’t get caught in poor weather.\\n\\nOur weather forecast can give you a great sense of what weather to expect in Shanghai in May 2024.\\n\\nIf you’re planning to visit Shanghai in the near future, we highly recommend that you review the 【6†14 day weather forecast for Shanghai】 before you arrive.\\n\\n[Image 5: Temperatures]\\n\\nTemperatures\\n\\n 26° / 17° \\n\\n[Image 6: Rainy Days]\\n\\nRainy Days\\n\\n6\\n\\n[Image 7: Snowy Days]\\n\\nSnowy Days\\n\\n0\\n\\n[Image 8: Dry Days]\\n\\nDry Days\\n\\n25\\n\\n[Image 9: Rainfall]\\n\\nRainfall\\n\\n91\\n\\nmm\\n\\n[Image 10: 9.1]\\n\\nSun Hours\\n\\n9.1\\n\\nHrs\\n\\nHistoric average weather for May\\n\\n【15† 】 \\n\\nMay\\n\\n【8† 】 \\n\\nSun Mon Tue Wed Thu Fri Sat  \\n\\n【28† 1 23 ° / 13 ° 】【29† 2 24 ° / 14 ° 】【30† 3 23 ° / 15 ° 】【31† 4 21 ° / 15 ° 】  \\n【32† 5 24 ° / 17 ° 】【33† 6 24 ° / 17 ° 】【34† 7 25 ° / 15 ° 】【35† 8 25 ° / 16 ° 】【36† 9 26 ° / 16 ° 】【37† 10 25 ° / 16 ° 】【38† 11 22 ° / 18 ° 】  \\n【39† 12 24 ° / 18 ° 】【40† 13 26 ° / 16 ° 】【41† 14 26 ° / 17 ° 】【42† 15 27 ° / 18 ° 】【43† 16 28 ° / 16 ° 】【44† 17 24 ° / 16 ° 】【45† 18 25 ° / 18 ° 】  \\n【46† 19 24 ° / 16 ° 】【47† 20 23 ° / 17 ° 】【48† 21 27 ° / 18 ° 】【49† 22 25 ° / 18 ° 】【50† 23 26 ° / 17 ° 】【51† 24 23 ° / 18 ° 】【52† 25 28 ° / 19 ° 】  \\n【10† 26 34 ° / 22 ° 】【11† 27 24 ° / 18 ° 】【16† 28 26 ° / 17 ° 】【17† 29 26 ° / 19 ° 】【18† 30 25 ° / 19 ° 】【19† 31 19 ° / 18 ° 】  \\n\\nClick on a day for an hourly weather forecast\\n\\n## Explore the weather in Shanghai in other months\\n\\n【53†01 January】【54†02 February】【55†03 March】【15†04 April】【7†05 May】【8†06 June】【56†07 July】【57†08 August】【58†09 September】【59†10 October】【60†11 November】【61†12 December】 \\n\\n## Shanghai annual weather\\n\\nMonth Temperatures Rainy Days Dry Days Snowy Days Rainfall Weather More details  \\nJanuary 9° / 3° 5 24 2 52 mm\\n\\nBad\\n\\n【53†Shanghai in January】  \\nFebruary 11° / 4° 6 20 2 79 mm\\n\\nBad\\n\\n【54†Shanghai in February】  \\nMarch 15° / 7° 7 24 0 93 mm\\n\\nOk\\n\\n【55†Shanghai in March】  \\nApril 20° / 12° 4 26 0 65 mm\\n\\nGood\\n\\n【15†Shanghai in April】  \\nMay 26° / 17° 6 25 0 91 mm\\n\\nPerfect\\n\\n【7†Shanghai in May】  \\nJune 28° / 21° 10 20 0 256 mm\\n\\nGood\\n\\n【8†Shanghai in June】  \\nJuly 31° / 25° 14 17 0 284 mm\\n\\nOk\\n\\n【56†Shanghai in July】  \\nAugust 32° / 25° 12 19 0 212 mm\\n\\nOk\\n\\n【57†Shanghai in August】  \\nSeptember 28° / 21° 8 22 0 146 mm\\n\\nGood\\n\\n【58†Shanghai in September】  \\nOctober 23° / 17° 3 28 0 62 mm\\n\\nPerfect\\n\\n【59†Shanghai in October\",\n" +
//                "                        \"pub_date\": null,\n" +
//                "                        \"extra\": {\n" +
//                "                            \"cited_message_idx\": 6,\n" +
//                "                            \"search_result_idx\": null,\n" +
//                "                            \"evidence_text\": \"source\"\n" +
//                "                        }\n" +
//                "                    }\n" +
//                "                },\n" +
//                "                {\n" +
//                "                    \"start_ix\": 52,\n" +
//                "                    \"end_ix\": 62,\n" +
//                "                    \"citation_format_type\": \"tether_og\",\n" +
//                "                    \"metadata\": {\n" +
//                "                        \"type\": \"webpage\",\n" +
//                "                        \"title\": \"5 Day Weather Forecast: Shanghai, China\",\n" +
//                "                        \"url\": \"https://weather-and-climate.com/5-five-day-forecast,Shanghai,China\",\n" +
//                "                        \"text\": \"\\nURL: https://weather-and-climate.com/5-five-day-forecast,Shanghai,China\\n\\n  【0†Weather & Climate】   \\n\\n  * 【0†Home】\\n  * 【1†Bucket List】\\n  * 【2†Weather Planner】\\n\\n[Image 0: China]【0†Countries】  【3†China】  【4†Shanghai Area】  【5†Shanghai】  【6†5 days】\\n\\n[Image 1: China]【3†China】  【4†Shanghai Area】  【5†Shanghai】\\n\\nShanghai [Image 2] \\n\\n5 day Weather Forecast \\n\\n【5†Climate】【7†Forecast】【8†May】【9†June】 >>\\n\\n【10†January】【11†February】【12†March】【13†April】【8†May】【9†June】【14†July】【15†August】【16†September】【17†October】【18†November】【19†December】 \\n\\n# 5-Day Weather Forecast, Shanghai\\n\\nSun   May 26\\n\\n34°C | 22°C\\n\\nSW [Image 3] 11 km/h\\n\\n8.9 mm\\n\\n[Image 4: partly cloudy and thunder] \\n\\npartly cloudy and thunder\\n\\n【20†hourly forecast】 \\n\\nMon   May 27\\n\\n24°C | 18°C\\n\\nNE [Image 5] 14 km/h\\n\\n12 mm\\n\\n[Image 6: overcast and heavy rain] \\n\\novercast and heavy rain\\n\\n【21†hourly forecast】 \\n\\nTue   May 28\\n\\n26°C | 17°C\\n\\nNE [Image 7] 14 km/h\\n\\n[Image 8: clear and no rain] \\n\\nclear and no rain\\n\\n【22†hourly forecast】 \\n\\nWed   May 29\\n\\n26°C | 17°C\\n\\nSE [Image 9] 14 km/h\\n\\n[Image 10: almost clear and no rain] \\n\\nalmost clear and no rain\\n\\nThu   May 30\\n\\n28°C | 19°C\\n\\nSE [Image 11] 14 km/h\\n\\n[Image 12: broken clouds and no rain] \\n\\nbroken clouds and no rain\\n\\nSee the 10-day forecast for popular European destinations\\n\\n【23†Europe Forecast】 \\n\\nGet more details in the 【24†extended 10 day weather forecast】 for Shanghai. Check out our 【8†May climate page】 to see if the current temperatures in Shanghai is typical for this time of year. \\n\\n### Shanghai Today\\n\\n【7†10 day Forecast】 \\n\\n【8†Weather in May】 \\n\\n【5†Averages & Climate】 \\n\\n### Travel Inspiration\\n\\n【25† 】 \\n\\n【25† The Li River is the most beautiful place in the world The Li River might be the most beautiful and most photographed landscape in China. It is one of the highlights that is on the bucket list for many travelers who go to China. There are 3 different periods to visit the Li River. 】 \\n\\n【26† 】 \\n\\n【26† Be amazed by the Altai mountains The Atlai mountains are a sparsely populated area that most tourists have not yet discovered. Definitely bucket list material for the adventurous travelers among us. 】 \\n\\n【27† 】 \\n\\n【27† Hike tiger leaping Gorge In the South-West of China you will find a beautiful gorge which is a must for your bucket list, the name of this gap is Tiger Leaping Gorge. We recommend to visit the Tiger Leaping Gorge from March til May and from October til November. 】 \\n\\n【28† 】 \\n\\n【28† Go to Xinjiang in China In Xinjiang you get to see a mix of East and Central Asia. An experience that you might want to add to your bucket list. You can travel through the province as if you are following an ancient silk route. 】 \\n\\n### About This Website\\n\\nDiscover this weather-focused website, providing precise forecasts and climate data for popular destinations worldwide. Sourced from Foreca, it offers insights spanning 1990 to 2020. Explore with confidence, armed with accurate weather information at your fingertips. \\n\\n【2† Weather Planner Not sure where to go? 】 【1† Bucket list Unique places on the globe 】 \\n\\nSubscribe to our newsletter for the latest updates!\\n\\nConnect with us on: 【29† Facebook †web.facebook.com】 【30† Instagram †instagram.com】 \\n\\n[Image 13: Weather and climate homepage] \\n\\n© 2010-2024 World Weather & Climate Information   \\nAll rights reserved.\\n\\n【31†About】 【32†Contact】 【33†Terms】 【34†Privacy】\\nVisible: 0% - 100%\",\n" +
//                "                        \"pub_date\": null,\n" +
//                "                        \"extra\": {\n" +
//                "                            \"cited_message_idx\": 9,\n" +
//                "                            \"search_result_idx\": null,\n" +
//                "                            \"evidence_text\": \"source\"\n" +
//                "                        }\n" +
//                "                    }\n" +
//                "                },\n" +
//                "                {\n" +
//                "                    \"start_ix\": 106,\n" +
//                "                    \"end_ix\": 116,\n" +
//                "                    \"citation_format_type\": \"tether_og\",\n" +
//                "                    \"metadata\": {\n" +
//                "                        \"type\": \"webpage\",\n" +
//                "                        \"title\": \"Hourly forecast for Shanghai, Shanghai Municipality, China\",\n" +
//                "                        \"url\": \"https://www.timeanddate.com/weather/china/shanghai/hourly\",\n" +
//                "                        \"text\": \"    * 【69†Alternative Age Calculator】\\n    * 【70†Date Pattern Calculator】\\n    * 【71†Distance Calculator】\\n  * 【72† My Account】\\n    * 【73†My Account】\\n    * 【74†My Location】\\n    * 【75†My Units】\\n    * 【27†My Events】\\n    * 【9†My World Clock】\\n    * 【76†My Privacy】\\n    * 【77†Paid Services】\\n    * 【78†Sign in】\\n    * 【79†Register】\\n\\n【80†Home】   【35†Weather】   【81†China】   【82†Shanghai】   Hour-by-hour\\n\\n[Image 1: Flag for China] \\n\\n# Hour-by-Hour Forecast for Shanghai, Shanghai Municipality, China\\n\\n  * 【83†Time/General】\\n  * 【82†Weather 】\\n    * 【82†Weather Today/Tomorrow 】\\n    * 【84†Hour-by-Hour Forecast 】\\n    * 【85†14 Day Forecast 】\\n    *     * 【86†Yesterday/Past Weather】\\n    * 【87†Climate (Averages)】\\n  * 【88†Time Zone 】\\n  * 【89†DST Changes】\\n  * 【90†Sun & Moon 】\\n    * 【90†Sun & Moon Today 】\\n    * 【91†Sunrise & Sunset 】\\n    *     * 【92†Moonrise & Moonset 】\\n    * 【93†Moon Phases 】\\n    *     * 【94†Eclipses 】\\n    * 【95†Night Sky 】\\n\\n【82†Weather Today】【84†Weather Hourly】【85†14 Day Forecast】【86†Yesterday/Past Weather】【87†Climate (Averages)】\\n\\n[Image 2]Currently: 84 °F. Passing clouds. (Weather station: Shanghai Hongqiao Airport, China). 【82†See more current weather】\\n\\n×\\n\\n## Hour-by-hour Forecast in Shanghai — Graph\\n\\n【75†°F】\\n\\nSee 【86†Historic Weather】 for previous weather \\n\\nSee 【85†Extended Forecast】 for more weather\\n\\n【82†See weather overview】\\n\\n## Detailed Hourly Forecast — Next 24 hours \\n\\nShow weather on: Next 24 hours May 26, 2024 May 27, 2024 May 28, 2024 May 29, 2024 May 30, 2024 May 31, 2024 June 1, 2024 June 2, 2024 \\n\\nScroll right to see more Conditions Comfort Precipitation   \\nTime Temp Weather Feels Like Wind Humidity Chance Amount   \\n10:00 pm   \\nSun, May 26[Image 3: Passing showers. Cloudy.]76 °F Passing showers. Cloudy.77 °F 6 mph↑89%50%0.01\\\" (rain)  \\n11:00 pm[Image 4: Overcast.]76 °F Overcast.76 °F 8 mph↑89%10%0.00\\\" (rain)  \\n12:00 am   \\nMon, May 27[Image 5: Overcast.]75 °F Overcast.74 °F 8 mph↑90%10%0.00\\\" (rain)  \\n1:00 am[Image 6: Sprinkles. Overcast.]74 °F Sprinkles. Overcast.72 °F 8 mph↑91%22%0.00\\\" (rain)  \\n2:00 am[Image 7: Sprinkles. Overcast.]73 °F Sprinkles. Overcast.70 °F 9 mph↑92%24%0.00\\\" (rain)  \\n3:00 am[Image 8: Sprinkles. Overcast.]72 °F Sprinkles. Overcast.68 °F 9 mph↑93%27%0.00\\\" (rain)  \\n4:00 am[Image 9: Passing showers. Overcast.]71 °F Passing showers. Overcast.66 °F 9 mph↑93%30%0.00\\\" (rain)  \\n5:00 am[Image 10: Passing showers. Overcast.]70 °F Passing showers. Overcast.66 °F 9 mph↑92%34%0.00\\\" (rain)  \\n6:00 am[Image 11: Passing showers. Overcast.]70 °F Passing showers. Overcast.67 °F 9 mph↑90%33%0.00\\\" (rain)  \\n7:00 am[Image 12: Passing showers. Overcast.]71 °F Passing showers. Overcast.69 °F 10 mph↑87%30%0.00\\\" (rain)  \\n8:00 am[Image 13: Passing showers. Overcast.]72 °F Passing showers. Overcast.71 °F 10 mph↑83%28%0.00\\\" (rain)  \\n9:00 am[Image 14: Passing showers. Overcast.]72 °F Passing showers. Overcast.72 °F 10 mph↑80%41%0.00\\\" (rain)  \\n10:00 am[Image 15: Passing showers. Overcast.]71 °F Passing showers. Overcast.73 °F 10 mph↑77%67%0.01\\\" (rain)  \\n11:00 am[Image 16: Sprinkles. Overcast.]71 °F Sprinkles. Overcast.73 °F 11 mph↑74%74%0.02\\\" (rain)  \\n12:00 pm[Image 17: Light rain. Overcast.]71 °F Light rain. Overcast.74 °F 11 mph↑72%79%0.03\\\" (\",\n" +
//                "                        \"pub_date\": null,\n" +
//                "                        \"extra\": {\n" +
//                "                            \"cited_message_idx\": 7,\n" +
//                "                            \"search_result_idx\": null,\n" +
//                "                            \"evidence_text\": \"source\"\n" +
//                "                        }\n" +
//                "                    }\n" +
//                "                },\n" +
//                "                {\n" +
//                "                    \"start_ix\": 116,\n" +
//                "                    \"end_ix\": 126,\n" +
//                "                    \"citation_format_type\": \"tether_og\",\n" +
//                "                    \"metadata\": {\n" +
//                "                        \"type\": \"webpage\",\n" +
//                "                        \"title\": \"5 Day Weather Forecast: Shanghai, China\",\n" +
//                "                        \"url\": \"https://weather-and-climate.com/5-five-day-forecast,Shanghai,China\",\n" +
//                "                        \"text\": \"\\nURL: https://weather-and-climate.com/5-five-day-forecast,Shanghai,China\\n\\n  【0†Weather & Climate】   \\n\\n  * 【0†Home】\\n  * 【1†Bucket List】\\n  * 【2†Weather Planner】\\n\\n[Image 0: China]【0†Countries】  【3†China】  【4†Shanghai Area】  【5†Shanghai】  【6†5 days】\\n\\n[Image 1: China]【3†China】  【4†Shanghai Area】  【5†Shanghai】\\n\\nShanghai [Image 2] \\n\\n5 day Weather Forecast \\n\\n【5†Climate】【7†Forecast】【8†May】【9†June】 >>\\n\\n【10†January】【11†February】【12†March】【13†April】【8†May】【9†June】【14†July】【15†August】【16†September】【17†October】【18†November】【19†December】 \\n\\n# 5-Day Weather Forecast, Shanghai\\n\\nSun   May 26\\n\\n34°C | 22°C\\n\\nSW [Image 3] 11 km/h\\n\\n8.9 mm\\n\\n[Image 4: partly cloudy and thunder] \\n\\npartly cloudy and thunder\\n\\n【20†hourly forecast】 \\n\\nMon   May 27\\n\\n24°C | 18°C\\n\\nNE [Image 5] 14 km/h\\n\\n12 mm\\n\\n[Image 6: overcast and heavy rain] \\n\\novercast and heavy rain\\n\\n【21†hourly forecast】 \\n\\nTue   May 28\\n\\n26°C | 17°C\\n\\nNE [Image 7] 14 km/h\\n\\n[Image 8: clear and no rain] \\n\\nclear and no rain\\n\\n【22†hourly forecast】 \\n\\nWed   May 29\\n\\n26°C | 17°C\\n\\nSE [Image 9] 14 km/h\\n\\n[Image 10: almost clear and no rain] \\n\\nalmost clear and no rain\\n\\nThu   May 30\\n\\n28°C | 19°C\\n\\nSE [Image 11] 14 km/h\\n\\n[Image 12: broken clouds and no rain] \\n\\nbroken clouds and no rain\\n\\nSee the 10-day forecast for popular European destinations\\n\\n【23†Europe Forecast】 \\n\\nGet more details in the 【24†extended 10 day weather forecast】 for Shanghai. Check out our 【8†May climate page】 to see if the current temperatures in Shanghai is typical for this time of year. \\n\\n### Shanghai Today\\n\\n【7†10 day Forecast】 \\n\\n【8†Weather in May】 \\n\\n【5†Averages & Climate】 \\n\\n### Travel Inspiration\\n\\n【25† 】 \\n\\n【25† The Li River is the most beautiful place in the world The Li River might be the most beautiful and most photographed landscape in China. It is one of the highlights that is on the bucket list for many travelers who go to China. There are 3 different periods to visit the Li River. 】 \\n\\n【26† 】 \\n\\n【26† Be amazed by the Altai mountains The Atlai mountains are a sparsely populated area that most tourists have not yet discovered. Definitely bucket list material for the adventurous travelers among us. 】 \\n\\n【27† 】 \\n\\n【27† Hike tiger leaping Gorge In the South-West of China you will find a beautiful gorge which is a must for your bucket list, the name of this gap is Tiger Leaping Gorge. We recommend to visit the Tiger Leaping Gorge from March til May and from October til November. 】 \\n\\n【28† 】 \\n\\n【28† Go to Xinjiang in China In Xinjiang you get to see a mix of East and Central Asia. An experience that you might want to add to your bucket list. You can travel through the province as if you are following an ancient silk route. 】 \\n\\n### About This Website\\n\\nDiscover this weather-focused website, providing precise forecasts and climate data for popular destinations worldwide. Sourced from Foreca, it offers insights spanning 1990 to 2020. Explore with confidence, armed with accurate weather information at your fingertips. \\n\\n【2† Weather Planner Not sure where to go? 】 【1† Bucket list Unique places on the globe 】 \\n\\nSubscribe to our newsletter for the latest updates!\\n\\nConnect with us on: 【29† Facebook †web.facebook.com】 【30† Instagram †instagram.com】 \\n\\n[Image 13: Weather and climate homepage] \\n\\n© 2010-2024 World Weather & Climate Information   \\nAll rights reserved.\\n\\n【31†About】 【32†Contact】 【33†Terms】 【34†Privacy】\\nVisible: 0% - 100%\",\n" +
//                "                        \"pub_date\": null,\n" +
//                "                        \"extra\": {\n" +
//                "                            \"cited_message_idx\": 9,\n" +
//                "                            \"search_result_idx\": null,\n" +
//                "                            \"evidence_text\": \"source\"\n" +
//                "                        }\n" +
//                "                    }\n" +
//                "                },\n" +
//                "                {\n" +
//                "                    \"start_ix\": 152,\n" +
//                "                    \"end_ix\": 162,\n" +
//                "                    \"citation_format_type\": \"tether_og\",\n" +
//                "                    \"metadata\": {\n" +
//                "                        \"type\": \"webpage\",\n" +
//                "                        \"title\": \"5 Day Weather Forecast: Shanghai, China\",\n" +
//                "                        \"url\": \"https://weather-and-climate.com/5-five-day-forecast,Shanghai,China\",\n" +
//                "                        \"text\": \"\\nURL: https://weather-and-climate.com/5-five-day-forecast,Shanghai,China\\n\\n  【0†Weather & Climate】   \\n\\n  * 【0†Home】\\n  * 【1†Bucket List】\\n  * 【2†Weather Planner】\\n\\n[Image 0: China]【0†Countries】  【3†China】  【4†Shanghai Area】  【5†Shanghai】  【6†5 days】\\n\\n[Image 1: China]【3†China】  【4†Shanghai Area】  【5†Shanghai】\\n\\nShanghai [Image 2] \\n\\n5 day Weather Forecast \\n\\n【5†Climate】【7†Forecast】【8†May】【9†June】 >>\\n\\n【10†January】【11†February】【12†March】【13†April】【8†May】【9†June】【14†July】【15†August】【16†September】【17†October】【18†November】【19†December】 \\n\\n# 5-Day Weather Forecast, Shanghai\\n\\nSun   May 26\\n\\n34°C | 22°C\\n\\nSW [Image 3] 11 km/h\\n\\n8.9 mm\\n\\n[Image 4: partly cloudy and thunder] \\n\\npartly cloudy and thunder\\n\\n【20†hourly forecast】 \\n\\nMon   May 27\\n\\n24°C | 18°C\\n\\nNE [Image 5] 14 km/h\\n\\n12 mm\\n\\n[Image 6: overcast and heavy rain] \\n\\novercast and heavy rain\\n\\n【21†hourly forecast】 \\n\\nTue   May 28\\n\\n26°C | 17°C\\n\\nNE [Image 7] 14 km/h\\n\\n[Image 8: clear and no rain] \\n\\nclear and no rain\\n\\n【22†hourly forecast】 \\n\\nWed   May 29\\n\\n26°C | 17°C\\n\\nSE [Image 9] 14 km/h\\n\\n[Image 10: almost clear and no rain] \\n\\nalmost clear and no rain\\n\\nThu   May 30\\n\\n28°C | 19°C\\n\\nSE [Image 11] 14 km/h\\n\\n[Image 12: broken clouds and no rain] \\n\\nbroken clouds and no rain\\n\\nSee the 10-day forecast for popular European destinations\\n\\n【23†Europe Forecast】 \\n\\nGet more details in the 【24†extended 10 day weather forecast】 for Shanghai. Check out our 【8†May climate page】 to see if the current temperatures in Shanghai is typical for this time of year. \\n\\n### Shanghai Today\\n\\n【7†10 day Forecast】 \\n\\n【8†Weather in May】 \\n\\n【5†Averages & Climate】 \\n\\n### Travel Inspiration\\n\\n【25† 】 \\n\\n【25† The Li River is the most beautiful place in the world The Li River might be the most beautiful and most photographed landscape in China. It is one of the highlights that is on the bucket list for many travelers who go to China. There are 3 different periods to visit the Li River. 】 \\n\\n【26† 】 \\n\\n【26† Be amazed by the Altai mountains The Atlai mountains are a sparsely populated area that most tourists have not yet discovered. Definitely bucket list material for the adventurous travelers among us. 】 \\n\\n【27† 】 \\n\\n【27† Hike tiger leaping Gorge In the South-West of China you will find a beautiful gorge which is a must for your bucket list, the name of this gap is Tiger Leaping Gorge. We recommend to visit the Tiger Leaping Gorge from March til May and from October til November. 】 \\n\\n【28† 】 \\n\\n【28† Go to Xinjiang in China In Xinjiang you get to see a mix of East and Central Asia. An experience that you might want to add to your bucket list. You can travel through the province as if you are following an ancient silk route. 】 \\n\\n### About This Website\\n\\nDiscover this weather-focused website, providing precise forecasts and climate data for popular destinations worldwide. Sourced from Foreca, it offers insights spanning 1990 to 2020. Explore with confidence, armed with accurate weather information at your fingertips. \\n\\n【2† Weather Planner Not sure where to go? 】 【1† Bucket list Unique places on the globe 】 \\n\\nSubscribe to our newsletter for the latest updates!\\n\\nConnect with us on: 【29† Facebook †web.facebook.com】 【30† Instagram †instagram.com】 \\n\\n[Image 13: Weather and climate homepage] \\n\\n© 2010-2024 World Weather & Climate Information   \\nAll rights reserved.\\n\\n【31†About】 【32†Contact】 【33†Terms】 【34†Privacy】\\nVisible: 0% - 100%\",\n" +
//                "                        \"pub_date\": null,\n" +
//                "                        \"extra\": {\n" +
//                "                            \"cited_message_idx\": 9,\n" +
//                "                            \"search_result_idx\": null,\n" +
//                "                            \"evidence_text\": \"source\"\n" +
//                "                        }\n" +
//                "                    }\n" +
//                "                }\n" +
//                "            ],\n" +
//                "            \"gizmo_id\": null,\n" +
//                "            \"finish_details\": {\n" +
//                "                \"type\": \"stop\",\n" +
//                "                \"stop_tokens\": [\n" +
//                "                    200002\n" +
//                "                ]\n" +
//                "            },\n" +
//                "            \"is_complete\": true,\n" +
//                "            \"message_type\": \"next\",\n" +
//                "            \"model_slug\": \"gpt-4o\",\n" +
//                "            \"default_model_slug\": \"gpt-4o\",\n" +
//                "            \"pad\": \"AAAAAAAAAAAA\",\n" +
//                "            \"parent_id\": \"9c2216b5-adc6-4362-97c9-e213075349c3\",\n" +
//                "            \"model_switcher_deny\": [\n" +
//                "                {\n" +
//                "                    \"slug\": \"text-davinci-002-render-sha\",\n" +
//                "                    \"context\": \"regenerate\",\n" +
//                "                    \"reason\": \"unsupported_tool_use\",\n" +
//                "                    \"description\": \"This model doesn't support using tools.\"\n" +
//                "                },\n" +
//                "                {\n" +
//                "                    \"slug\": \"text-davinci-002-render-sha\",\n" +
//                "                    \"context\": \"conversation\",\n" +
//                "                    \"reason\": \"unsupported_tool_use\",\n" +
//                "                    \"description\": \"This model doesn't support using tools.\"\n" +
//                "                }\n" +
//                "            ]\n" +
//                "        },\n" +
//                "        \"recipient\": \"all\"\n" +
//                "    },\n" +
//                "    \"conversation_id\": \"06196e40-8bc9-446d-b77f-67ae7a9d74e4\",\n" +
//                "    \"error\": null\n" +
//                "}";
//
//        // 解析 JSON 数据
//        JSONObject jsonObject = JSONUtil.parseObj(jsonString);
//        JSONObject message = jsonObject.getJSONObject("message");
//        JSONObject content = message.getJSONObject("content");
//        JSONArray parts = content.getJSONArray("parts");
//        String text = parts.getStr(0);
//
//        // 解析引用信息
//        JSONObject metadata = message.getJSONObject("metadata");
//        JSONArray citations = metadata.getJSONArray("citations");
//
//        // 构建带引用标识符的文本
//        StringBuilder result = new StringBuilder(text);
//
//        // 倒序插入引用标识符，以避免索引问题
//        for (int i = citations.size() - 1; i >= 0; i--) {
//            JSONObject citation = citations.getJSONObject(i);
//            int startIx = citation.getInt("start_ix");
//            int endIx = citation.getInt("end_ix");
//            String citationText = "[" + (i + 1) + "†source]";
//
//            // 确保索引不超出文本长度
//            if (endIx <= result.length()) {
//                result.insert(endIx, citationText);
//            } else {
//                System.err.println("Invalid end index: " + endIx);
//            }
//        }
//
//        // 打印带引用标识符的文本
//        System.out.println(result.toString());
//
//        // 打印引用的详细信息
//        System.out.println("\nReferences:");
//        for (int i = 0; i < citations.size(); i++) {
//            JSONObject citation = citations.getJSONObject(i);
//            JSONObject citationMetadata = citation.getJSONObject("metadata");
//            String title = citationMetadata.getStr("title");
//            String url = citationMetadata.getStr("url");
//
//            System.out.println("[" + (i + 1) + "†source]: " + title + " - " + url);
//        }
//    }

//    public static void main(String[] args) {
//        final String[] messageId = {UUID.randomUUID().toString()};
//        System.out.println(messageId[0]);
//    }
}
