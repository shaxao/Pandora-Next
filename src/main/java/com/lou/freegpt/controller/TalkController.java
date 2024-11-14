package com.lou.freegpt.controller;

import cn.hutool.core.codec.Base64Encoder;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import com.google.common.collect.PeekingIterator;
import com.lou.freegpt.api.xunfei.AudioStream;
import com.lou.freegpt.domain.AudioRequest;
import com.lou.freegpt.domain.MessageEntity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import com.lou.freegpt.dao.MessageDao;
import com.lou.freegpt.vo.MessageVo;
import com.lou.freegpt.enums.AjaxResult;
import com.lou.freegpt.enums.ModelEnum;
import com.lou.freegpt.service.ChatService;
import com.lou.freegpt.utils.RequestUtils;
import com.lou.freegpt.vo.TitleVo;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.*;
import java.lang.annotation.Annotation;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api")
@Slf4j
public class TalkController {

    @Autowired
    private ChatService chatService;
    @Autowired
    private RequestUtils requestUtils;
    @Autowired
    private MessageDao messageDao;

    @Autowired
    private ExecutorService executorService;

//    @PostMapping("/base")
//    public Mono<ServerResponse> talkTest(
//            @RequestPart(value = "file", required = false) Mono<FilePart> fileMono,
//            @RequestParam("messageJson") String messageJson,
//            @RequestParam("globalSetJson") String globalSetJson) {
//        fileMono = (fileMono == null) ? Mono.empty() : fileMono;
//        MessageVo messageVo = JSONUtil.toBean(messageJson, MessageVo.class);
//        System.out.println("接收到的消息内容: " + messageVo.getContent() + ", 模型: " + messageVo.getModel() + ", 全局设置: " + globalSetJson);
//
//        return fileMono.flatMap(file -> {
//            try {
//                return file.content()
//                        .map(dataBuffer -> {
//                            byte[] bytes = new byte[dataBuffer.readableByteCount()];
//                            dataBuffer.read(bytes);
//                            return bytes;
//                        })
//                        .reduce((bytes1, bytes2) -> {
//                            byte[] combined = new byte[bytes1.length + bytes2.length];
//                            System.arraycopy(bytes1, 0, combined, 0, bytes1.length);
//                            System.arraycopy(bytes2, 0, combined, bytes1.length, bytes2.length);
//                            return combined;
//                        })
//                        .flatMap(bytes -> {
//                            String fileEncoder = Base64.getEncoder().encodeToString(bytes);
//                            return processMessageAndRespond(messageVo, fileEncoder, globalSetJson);
//                        });
//            } catch (Exception e) {
//                e.printStackTrace();
//                return Mono.error(new RuntimeException("图片加载失败"));
//            }
//        }).switchIfEmpty(processMessageAndRespond(messageVo, "", globalSetJson));
//    }
//
//    private Mono<ServerResponse> processMessageAndRespond(MessageVo messageVo, String fileEncoder, String globalSetJson) {
//        if (messageVo.getContent() != null && !messageVo.getContent().isEmpty()) {
//            return ServerResponse.ok()
//                    .contentType(MediaType.TEXT_EVENT_STREAM)
//                    .body(BodyInserters.fromServerSentEvents(processMessage(messageVo, fileEncoder, globalSetJson)
//                            .map(content -> ServerSentEvent.builder(content).build())));
//        }
//        return ServerResponse.ok().build();
//    }
//
//    private Flux<ServerSentEvent<String>> processMessage(MessageVo messageVo, String fileEncoder, String globalSetJson) {
//        if (messageVo.getModel().equals(ModelEnum.PERSON_ONE.getModels()) || messageVo.getModel().equals(ModelEnum.PERSON_TWO.getModels())) {
//            return Flux.empty();
//        } else {
//            return requestUtils.streamRequestTest(messageVo, fileEncoder, globalSetJson)
//                    .map(data -> ServerSentEvent.builder(data).build());
//        }
//    }



    /**
     * 接受消息内容并回复
     * @param file
     * @return
     */
    @PostMapping("/base")
    public void talkTest(@RequestParam(value = "file", required = false) MultipartFile file,
                         @RequestParam("messageJson")String messageJson,
                         @RequestParam("globalSetJson") String globalSetJson,
                         HttpServletResponse response,
                         HttpServletRequest request){
        MessageVo messageVo = JSONUtil.toBean(messageJson, MessageVo.class);
        System.out.println("接收到的消息内容: " + messageVo.getContent() + ",模型:" + messageVo.getModel() + "文件:" + file + ",全局设置:" + globalSetJson);
        String fileEncoder = "";
        if(file != null && (!file.isEmpty() || file.getSize() != 0)) {
            try {
                fileEncoder = Base64Encoder.encode(file.getBytes());
                System.out.println(fileEncoder);
            } catch (IOException e) {
               log.error("图片加载失败: {}", e.getMessage());
              //  return AjaxResult.fail("图片加载失败");4015efcd15b84d7ea5c17f9d27b0ef8a
            }
        }
        if (messageVo.getContent() != null && !messageVo.getContent().isEmpty()) {
            String reply = "";
            try {
                // 提交任务1
                Runnable task1 = () -> {
                    messageDao.insertMessage(messageVo, "user", "text");
                };
                executorService.submit(task1);

                // 提交任务2
                Runnable task2 = () -> {
                    Map<String, String> params = new HashMap<>();
                    params.put("message.updateTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    params.put("children", messageVo.getMessageId());
                    messageDao.updateById(messageVo.getParentId(), params);
                };
                executorService.submit(task2);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if(messageVo.getModel().equals(ModelEnum.PERSON_ONE.getModels()) || messageVo.getModel().equals(ModelEnum.PERSON_TWO.getModels())) {
                try {
                    requestUtils.textRequest(messageVo.getContent(), messageVo.getModel(), response, fileEncoder);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }else {
                //reply = chatService.processMessage(message,model,fileEncoder);
                requestUtils.streamRequestTest(messageVo,request, response, fileEncoder, globalSetJson);
                // return AjaxResult.success("消息顺利返回", reply);
            }
        } else {
            try {
                response.setCharacterEncoding("UTF-8"); // 设置字符编码为UTF-8
                response.setContentType("application/json; charset=UTF-8"); // 设置内容类型为JSON，并指定字符编
                response.getWriter().write("消息不能为空" + "\n\n");
                response.flushBuffer();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            System.out.println("消息为空");
           // return AjaxResult.fail("消息不能为空");
        }
    }


    /**
     * 生成图片
     * @return
     */
    @PostMapping("/gen")
    public void imageGen(@RequestParam("messageJson")String messageJson,
                         @RequestParam("globalSetJson") String globalSetJson,
                         HttpServletResponse response){
        MessageVo messageVo = JSONUtil.toBean(messageJson, MessageVo.class);
        System.out.println("接收到的消息内容: " + messageVo.getContent() + ",模型:" + messageVo.getModel() + ",全局设置:" + globalSetJson);
        String fileEncoder = "";
        if (messageVo.getContent() != null && !messageVo.getContent().isEmpty()) {
            String reply = "";
            try {
                // 提交任务1
                Runnable task1 = () -> {
                    messageDao.insertMessage(messageVo, "user", "text");
                };
                executorService.submit(task1);

                // 提交任务2
                Runnable task2 = () -> {
                    Map<String, String> params = new HashMap<>();
                    params.put("message.updateTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    params.put("children", messageVo.getMessageId());
                    messageDao.updateById(messageVo.getParentId(), params);
                };
                executorService.submit(task2);
            } catch (Exception e) {
                e.printStackTrace();
            }
            List<String> models = Arrays.asList(ModelEnum.STABLE_DIFFUSION.getModels(), ModelEnum.DALLE.getModels(), ModelEnum.LINK_MJ.getModels());
            if(models.contains(messageVo.getModel())){
                reply = chatService.imageGenera(messageVo, globalSetJson);
                response.setCharacterEncoding("UTF-8"); // 设置字符编码为UTF-8
                response.setContentType("application/json; charset=UTF-8"); // 设置内容类型为JSON，并指定字符编
                try {
                    response.getWriter().write(reply);
                    response.flushBuffer();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        } else {
            try {
                response.setCharacterEncoding("UTF-8"); // 设置字符编码为UTF-8
                response.setContentType("application/json; charset=UTF-8"); // 设置内容类型为JSON，并指定字符编
                response.getWriter().write("消息不能为空" + "\n\n");
                response.flushBuffer();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            System.out.println("消息为空");
            // return AjaxResult.fail("消息不能为空");
        }
    }

    @PostMapping("/gen_title")
    public AjaxResult genTitle(@RequestBody TitleVo titleVo) {
        log.info("titleVo:{}", titleVo.toString());
        if(!titleVo.getIsFirstFlag()){
            AjaxResult.error();
        }
        String title = chatService.genTitle(titleVo);
        return AjaxResult.success("标题生成成功", title);
    }

    @GetMapping("/findTitle/{offset}/{limit}")
    public AjaxResult findTitles(@PathVariable int offset, @PathVariable int limit) {
        return AjaxResult.success(messageDao.findTitles(offset, limit));
    }

    @GetMapping("/findConver/{id}")
    public AjaxResult findConverByTitleId(@PathVariable String id) {
        return AjaxResult.success(messageDao.findConverByTitleId(id));
    }


    /**
     * 翻译
     * @return
     */
    @PostMapping("/tran")
    public void talkTest(@RequestParam("messageJson")String messageJson,
                         @RequestParam("globalSetJson") String globalSetJson,
                         HttpServletResponse response,
                         HttpServletRequest request){
        MessageVo messageVo = JSONUtil.toBean(messageJson, MessageVo.class);
        System.out.println("接收到的消息内容: " + messageVo.getContent() + ",模型:" + messageVo.getModel() + ",全局设置:" + globalSetJson);
        if (messageVo.getContent() != null && !messageVo.getContent().isEmpty()) {
             // reply = chatService.processMessage(message,model,fileEncoder);
             requestUtils.streamRequestTest(messageVo, request, response, null, globalSetJson);
             // return AjaxResult.success("消息顺利返回", reply);
        } else {
            try {
                response.setCharacterEncoding("UTF-8"); // 设置字符编码为UTF-8
                response.setContentType("application/json; charset=UTF-8"); // 设置内容类型为JSON，并指定字符编
                response.getWriter().write("消息不能为7空" + "\n\n");
                response.flushBuffer();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            System.out.println("消息为空");
            // return AjaxResult.fail("消息不能为空");
        }
    }

    @PostMapping("/audio")
    public ResponseEntity<InputStreamResource> audioWith(@RequestBody AudioRequest audioRequest) {
        try {
            // 从外部服务获取音频数据
            byte[] audioData = requestUtils.fetchAudioDataFromService(audioRequest.getMessage(), audioRequest.getSelectVoice());

            if (audioData != null) {
                InputStream inputStream = new ByteArrayInputStream(audioData);
                InputStreamResource inputStreamResource = new InputStreamResource(inputStream);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.parseMediaType("audio/ogg"));
                headers.setContentDispositionFormData("attachment", "audio.ogg");

                return ResponseEntity.ok()
                        .headers(headers)
                        .contentLength(audioData.length)
                        .body(inputStreamResource);
            } else {
                log.error("音频数据为空");
                return ResponseEntity.status(500).build();
            }
        } catch (Exception e) {
            log.error("音频数据处理异常:{}", e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping("/talk")
    public AjaxResult handleAudio(@RequestParam("audio") MultipartFile audioFile) {
        log.info("音频数据：{}", audioFile.getSize() + "，类型：" + audioFile.getContentType() + ",文件名：" + audioFile.getOriginalFilename());

        return chatService.convertAudioToText(audioFile);
    }

    @GetMapping("/search")
    public AjaxResult search(@RequestParam("q")String value,@RequestParam("max_results")Integer maxResults) {
        log.info("搜索参数:{},搜索返回数：{}", value, maxResults);
        String searchResponse = requestUtils.search(value, maxResults);
        return searchResponse == null ? AjaxResult.fail("搜索失败") : AjaxResult.success("搜索成功", searchResponse);
    }

    /**
     * 删除会话
     * @param conversationId
     * @return
     */
    @GetMapping("/deleteConverById")
    public AjaxResult deleteConverById(@RequestParam("conversationId") String conversationId) {
        return AjaxResult.toAjax(messageDao.deleteConverById(conversationId));
    }

    /**
     * 更新标题
     * @param conversationId
     * @return
     */
    @GetMapping("/updateTitle")
    public AjaxResult updateTitle(@RequestParam String conversationId, @RequestParam String title) {
        return AjaxResult.toAjax(messageDao.updateTitleById(conversationId, title));
    }

    /**
     * 清除会话记录
     * @return
     */
    @GetMapping("/clearConver/{conversationId}")
    public AjaxResult clearConver(@PathVariable String conversationId, HttpServletRequest request) {
        HttpSession session = request.getSession();
        try {
            session.setAttribute(conversationId, "");
            return AjaxResult.success("已清除!!");
        } catch (Exception e) {
            log.error("会话清除异常:{}", e.getMessage());
            return AjaxResult.fail("会话不存在");
        }
    }



}
