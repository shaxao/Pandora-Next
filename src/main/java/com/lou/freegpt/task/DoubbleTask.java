//package com.lou.freegpt.task;
//
//import ch.qos.logback.core.net.server.Client;
//import com.lou.freegpt.utils.RequestUtils;
//import jakarta.annotation.PostConstruct;
//import okhttp3.*;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//
//import java.io.IOException;
//import java.util.HashMap;
//
///**
// * 动态请求dobble刷新jwt
// */
//@Component
//public class DoubbleTask {
//
//    private RequestUtils requestUtils;
//
//    @Autowired
//    public DoubbleTask(RequestUtils requestUtils){
//        this.requestUtils = requestUtils;
//    }
//
//    @PostConstruct
//    public void init(){
//        // 手动调用一次定时任务的方法
//        refreshJWT();
//    }
//
//    @Scheduled(cron = "0 0/2 * * * ?")
//    public void refreshJWT(){
//        Headers authorization = new Headers.Builder()
//                .add("Content-Type", "application/json")
//                .add("Authorization", "Bearer api_qD4qn7nieKN34VCifxVq").build();
//        String url = "https://api.double.bot/api/auth/refresh";
//        RequestBody body = RequestBody.create(
//                MediaType.parse("application/json; charset=utf-8"),"");
//        OkHttpClient http = new OkHttpClient();
//        Request request = new Request.Builder()
//                .headers(authorization)
//                .post(body)
//                .url(url)
//                .build();
//        OkHttpClient client = new OkHttpClient();
//        try {
//            Response response = client.newCall(request).execute(); // 发送请求并获得响应
//            if (response.isSuccessful()) {
//                String responseData = response.body().string(); // 获取响应的内容
//                System.out.println("JWT" + responseData);
//                // 处理响应数据
//            } else {
//                // 处理错误情况
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//            // 处理异常情况
//        }
//
//    }
//}
