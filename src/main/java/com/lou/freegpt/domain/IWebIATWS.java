package com.lou.freegpt.domain;

import com.google.gson.JsonObject;
import okhttp3.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.lou.freegpt.controller.WebIATWS.*;

public class IWebIATWS extends WebSocketListener {
    // 类变量定义省略...
    private static final String hostUrl = "https://iat-api.xfyun.cn/v2/iat"; //中英文，http url 不支持解析 ws/wss schema
    // private static final String hostUrl = "https://iat-niche-api.xfyun.cn/v2/iat";//小语种
    private static final String appid = "8da5d157"; //在控制台-我的应用获取
    private static final String apiSecret = "YTY3MGYzYmZlYjQzZTQxZjM5ZWMwMjhm"; //在控制台-我的应用-语音听写（流式版）获取
    private static final String apiKey = "c7ff5345b9823c9b45d1e679b632ddbc"; //在控制台-我的应用-语音听写（流式版）获取
    String endAudio = "";

    private WebSocket webSocket;
    Decoder decoder = new Decoder();
    // 开始时间
    private static Date dateBegin = new Date();
    // 结束时间
    private static Date dateEnd = new Date();
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyy-MM-dd HH:mm:ss.SSS");
    private int status = StatusFirstFrame;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public void connect() throws Exception {
        System.out.println("XUNFEI Connection opened.");
        OkHttpClient client = new OkHttpClient.Builder().build();
        String url = getAuthUrl(hostUrl, apiKey, apiSecret).replace("http://", "ws://").replace("https://", "wss://");
        Request request = new Request.Builder().url(url).build();
        this.webSocket = client.newWebSocket(request, this);
    }

    public void sendAudioData(String file) {
        if (webSocket == null) {
            System.err.println("WebSocket is not connected.");
            return;
        }
        System.err.println("WebSocket is connected.");
        executor.submit(() -> {
            //连接成功，开始发送数据
            int frameSize = 1280; //每一帧音频的大小,建议每 40ms 发送 122B
            int intervel = 40;
            int status = 0;  // 音频的状态
            try (FileInputStream fs = new FileInputStream(file)) {
                byte[] buffer = new byte[frameSize];
                // 发送音频
                end:
                while (true) {
                    int len = fs.read(buffer);
                    if (len == -1) {
                        status = StatusLastFrame;  //文件读完，改变status 为 2
                    }
                    switch (status) {
                        case StatusFirstFrame:   // 第一帧音频status = 0
                            JsonObject frame = new JsonObject();
                            JsonObject business = new JsonObject();  //第一帧必须发送
                            JsonObject common = new JsonObject();  //第一帧必须发送
                            JsonObject data = new JsonObject();  //每一帧都要发送
                            // 填充common
                            common.addProperty("app_id", appid);
                            //填充business
                            business.addProperty("language", "zh_cn");
                            //business.addProperty("language", "en_us");//英文
                            //business.addProperty("language", "ja_jp");//日语，在控制台可添加试用或购买
                            //business.addProperty("language", "ko_kr");//韩语，在控制台可添加试用或购买
                            //business.addProperty("language", "ru-ru");//俄语，在控制台可添加试用或购买
                            business.addProperty("domain", "iat");
                            business.addProperty("accent", "mandarin");//中文方言请在控制台添加试用，添加后即展示相应参数值
                            //business.addProperty("nunum", 0);
                            //business.addProperty("ptt", 0);//标点符号
                            //business.addProperty("rlang", "zh-hk"); // zh-cn :简体中文（默认值）zh-hk :繁体香港(若未授权不生效，在控制台可免费开通)
                            //business.addProperty("vinfo", 1);
                            business.addProperty("dwa", "wpgs");//动态修正(若未授权不生效，在控制台可免费开通)
                            //business.addProperty("nbest", 5);// 句子多候选(若未授权不生效，在控制台可免费开通)
                            //business.addProperty("wbest", 3);// 词级多候选(若未授权不生效，在控制台可免费开通)
                            //填充data
                            data.addProperty("status", StatusFirstFrame);
                            data.addProperty("format", "audio/L16;rate=16000");
                            data.addProperty("encoding", "raw");
                            data.addProperty("audio", Base64.getEncoder().encodeToString(Arrays.copyOf(buffer, len)));
                            //填充frame
                            frame.add("common", common);
                            frame.add("business", business);
                            frame.add("data", data);
                            webSocket.send(frame.toString());
                            status = StatusContinueFrame;  // 发送完第一帧改变status 为 1
                            break;
                        case StatusContinueFrame:  //中间帧status = 1
                            JsonObject frame1 = new JsonObject();
                            JsonObject data1 = new JsonObject();
                            data1.addProperty("status", StatusContinueFrame);
                            data1.addProperty("format", "audio/L16;rate=16000");
                            data1.addProperty("encoding", "raw");
                            data1.addProperty("audio", Base64.getEncoder().encodeToString(Arrays.copyOf(buffer, len)));
                            frame1.add("data", data1);
                            webSocket.send(frame1.toString());
                            // System.out.println("send continue");
                            break;
                        case StatusLastFrame:    // 最后一帧音频status = 2 ，标志音频发送结束
                            JsonObject frame2 = new JsonObject();
                            JsonObject data2 = new JsonObject();
                            data2.addProperty("status", StatusLastFrame);
                            data2.addProperty("audio", "");
                            data2.addProperty("format", "audio/L16;rate=16000");
                            data2.addProperty("encoding", "raw");
                            frame2.add("data", data2);
                            webSocket.send(frame2.toString());
                            System.out.println("sendlast");
                            break end;
                    }
                    Thread.sleep(intervel); //模拟音频采样延时
                }
                System.out.println("all data is send");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    private void sendFirstFrame(byte[] audioData) {
        System.out.println("发送第一帧");
        // 创建JSON对象用于发送第一帧
        JsonObject frame = new JsonObject();
        JsonObject business = new JsonObject();  // 第一帧必须发送
        JsonObject common = new JsonObject();    // 第一帧必须发送
        JsonObject data = new JsonObject();      // 每一帧都要发送

        // 填充common
        common.addProperty("app_id", appid);

        // 填充business
        business.addProperty("language", "zh_cn");
        business.addProperty("domain", "iat");
        business.addProperty("accent", "mandarin");
        business.addProperty("dwa", "wpgs"); // 动态修正

        // 填充data
        data.addProperty("status", StatusFirstFrame);
        data.addProperty("format", "audio/L16;rate=16000");
        data.addProperty("encoding", "raw");
        // 直接使用传入的audioData数组，将其转换为Base64字符串
        data.addProperty("audio", Base64.getEncoder().encodeToString(audioData));

        // 填充frame
        frame.add("common", common);
        frame.add("business", business);
        frame.add("data", data);
        System.out.println("第一帧音频内容：" + frame.toString());
        // 发送构建的JSON字符串
        webSocket.send(frame.toString());
    }

    private void sendContinueFrame(byte[] audioData) {
        System.out.println("发送中间帧");
        if (audioData.length == 0) {
            sendLastFrame();
            return;
        }
        JsonObject frame = new JsonObject();
        JsonObject data = new JsonObject();
        data.addProperty("status", StatusContinueFrame);
        data.addProperty("format", "audio/L16;rate=16000");
        data.addProperty("encoding", "raw");
        data.addProperty("audio", Base64.getEncoder().encodeToString(audioData));
        frame.add("data", data);
        System.out.println("中间帧音频内容：" + frame.toString());
        webSocket.send(frame.toString());
    }

    private void sendLastFrame() {
        System.out.println("发送最后一帧");
        JsonObject frame = new JsonObject();
        JsonObject data = new JsonObject();
        data.addProperty("status", StatusLastFrame);
        data.addProperty("audio", "");
        frame.add("data", data);
        System.out.println("最后一帧音频内容：" + frame.toString());
        webSocket.send(frame.toString());
        status = StatusFirstFrame; // Reset status for next connection
    }

    @Override
    public void onMessage(WebSocket webSocket, String text) {
        super.onMessage(webSocket, text);
        //System.out.println(text);
        ResponseData resp = json.fromJson(text, ResponseData.class);
        System.out.println("code=>" + resp.getCode() + " error=>" + resp.getMessage() + " sid=" + resp.getSid());
        if (resp != null) {
            if (resp.getCode() != 0) {
                System.out.println( "code=>" + resp.getCode() + " error=>" + resp.getMessage() + " sid=" + resp.getSid());
                System.out.println( "错误码查询链接：https://www.xfyun.cn/document/error-code");
                return;
            }
            if (resp.getData() != null) {
                if (resp.getData().getResult() != null) {
                    Text te = resp.getData().getResult().getText();
                    //System.out.println(te.toString());
                    try {
                        decoder.decode(te);
                        System.out.println("中间识别结果 ==》" + decoder.toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (resp.getData().getStatus() == 2) {
                    // todo  resp.data.status ==2 说明数据全部返回完毕，可以关闭连接，释放资源
                    System.out.println("session end ");
                    dateEnd = new Date();
                    System.out.println(sdf.format(dateBegin) + "开始");
                    System.out.println(sdf.format(dateEnd) + "结束");
                    System.out.println("耗时:" + (dateEnd.getTime() - dateBegin.getTime()) + "ms");
                    endAudio = decoder.toString();
                    System.out.println("最终识别结果 ==》" + decoder.toString());
                    System.out.println("本次识别sid ==》" + resp.getSid());
                    decoder.discard();
                    // webSocket.close(1000, "");
                } else {
                    // todo 根据返回的数据处理
                }
            }
        }
    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
        super.onFailure(webSocket, t, response);
        try {
            if (null != response) {
                int code = response.code();
                System.out.println("onFailure code:" + code);
                System.out.println("onFailure body:" + response.body().string());
                if (101 != code) {
                    System.out.println("connection failed");
                    System.exit(0);
                }
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void disconnect() {
        System.out.println("最终识别结果:" + endAudio);
        if (webSocket != null) {
            sendLastFrame(); // Ensure to send last frame
            webSocket.close(1000, "Closing Connection");
        }
        webSocket.close(1000,"Closing Connection");
        System.out.println("WebSocket closed");
    }


}
