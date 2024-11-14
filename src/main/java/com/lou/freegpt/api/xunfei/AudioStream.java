package com.lou.freegpt.api.xunfei;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.lou.freegpt.controller.WebIATWS;
import lombok.Getter;
import okhttp3.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;

public class AudioStream extends WebSocketListener {
    private static final String hostUrl = "https://iat-api.xfyun.cn/v2/iat"; // 中英文，http url 不支持解析 ws/wss schema
    private static final String appid = "8da5d157"; // 在控制台-我的应用获取
    private static final String apiSecret = "YTY3MGYzYmZlYjQzZTQxZjM5ZWMwMjhm"; // 在控制台-我的应用-语音听写（流式版）获取
    private static final String apiKey = "c7ff5345b9823c9b45d1e679b632ddbc"; // 在控制台-我的应用-语音听写（流式版）获取
    public static final int StatusFirstFrame = 0;
    public static final int StatusContinueFrame = 1;
    public static final int StatusLastFrame = 2;
    public static final Gson json = new Gson();
    Decoder decoder = new Decoder();
    // 开始时间
    private static Date dateBegin = new Date();
    // 结束时间
    private static Date dateEnd = new Date();
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyy-MM-dd HH:mm:ss.SSS");

    private CountDownLatch latch;
    private InputStream audioStream;

    @Getter
    private String audioResult;

    public void setAudioResult(String audioResult) {
        this.audioResult = audioResult;
    }

    public AudioStream(CountDownLatch latch) {
        this.latch = latch;
    }
    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        System.out.println("ws连接成功");
        super.onOpen(webSocket, response);
        new Thread(() -> {
            // 连接成功，开始发送数据
            int frameSize = 1280; // 每一帧音频的大小,建议每 40ms 发送 122B
            int interval = 40;
            int status = StatusFirstFrame; // 音频的状态
            byte[] buffer = new byte[frameSize];
            int bytesRead;
            try {
                while ((bytesRead = audioStream.read(buffer)) != -1) {
                    System.out.println(bytesRead);
                    JsonObject frame = createFrame(buffer, bytesRead, status);
                    if (bytesRead < frameSize) {
                        status = StatusLastFrame; // 最后一帧
                        frame = createFrame(buffer, bytesRead, status);
                        System.out.println("最后一针");
                    } else if (status == StatusFirstFrame) {
                        status = StatusContinueFrame; // 第二帧开始
                        System.out.println("第二帧");
                    }
                    System.out.println(frame.toString());
                    webSocket.send(frame.toString());
                    if (status == StatusLastFrame) break; // 发送完毕
                    Thread.sleep(interval); // 模拟音频采样延时
                }
                audioStream.close(); // 关闭输入流
                webSocket.close(1000, "Closing after sending data");
                System.out.println("All data is sent and resources are closed.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public void onMessage(WebSocket webSocket, String text) {
        super.onMessage(webSocket, text);
        System.out.println("Received message: " + text);
        try {
            ResponseData resp = json.fromJson(text, ResponseData.class);
            if (resp != null) {
                if (resp.getCode() != 0) {
                    System.out.println("code=>" + resp.getCode() + " error=>" + resp.getMessage() + " sid=" + resp.getSid());
                    System.out.println("错误码查询链接：https://www.xfyun.cn/document/error-code");
                    return;
                }
                if (resp.getData() != null) {
                    if (resp.getData().getResult() != null) {
                        Text te = resp.getData().getResult().getText();
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
                        setAudioResult(decoder.toString());
                        System.out.println("最终识别结果 ==》" + audioResult);
                        System.out.println("本次识别sid ==》" + resp.getSid());
                        decoder.discard();
                        latch.countDown();
                        webSocket.close(1000, "");
                    } else {
                        // todo 根据返回的数据处理
                    }
                }
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
        super.onFailure(webSocket, t, response);
        System.out.println("错误");
        try {
            if (null != response) {
                int code = response.code();
                System.out.println("onFailure code:" + code);
                System.out.println("onFailure body:" + response.body().string());
                if (101 != code) {
                    System.out.println("语音连接失败");
                    System.exit(0);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            latch.countDown();
        }
    }

    public void processAudioStream(InputStream audioStream, long totalBytes) throws Exception {
        this.audioStream = audioStream;
        OkHttpClient client = new OkHttpClient.Builder().build();
        String url = getAuthUrl(hostUrl, apiKey, apiSecret).replace("http://", "ws://").replace("https://", "wss://");
        Request request = new Request.Builder().url(url).build();
        client.newWebSocket(request, this);
    }


    private JsonObject createFrame(byte[] buffer, int length, int status) {
        JsonObject frame = new JsonObject();
        JsonObject business = new JsonObject(); // 第一帧必须发送
        JsonObject common = new JsonObject(); // 第一帧必须发送
        JsonObject data = new JsonObject(); // 每一帧都要发送

        if (status == StatusFirstFrame) {
            common.addProperty("app_id", appid);
            business.addProperty("language", "zh_cn");
            business.addProperty("domain", "iat");
            business.addProperty("accent", "mandarin");
            business.addProperty("dwa", "wpgs");
        }

        data.addProperty("status", status);
        data.addProperty("format", "audio/L16;rate=16000");
        data.addProperty("encoding", "raw");

        if(status == StatusLastFrame) {
            data.addProperty("audio", "");
        }else {
            data.addProperty("audio", Base64.getEncoder().encodeToString(Arrays.copyOf(buffer, length)));
        }
        if (status == StatusFirstFrame) {
            frame.add("common", common);
            frame.add("business", business);
        }
        frame.add("data", data);

        return frame;
    }

    public static String getAuthUrl(String hostUrl, String apiKey, String apiSecret) throws Exception {
        URL url = new URL(hostUrl);
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        String date = format.format(new Date());
        StringBuilder builder = new StringBuilder("host: ").append(url.getHost()).append("\n")
                .append("date: ").append(date).append("\n")
                .append("GET ").append(url.getPath()).append(" HTTP/1.1");

        Charset charset = Charset.forName("UTF-8");
        Mac mac = Mac.getInstance("hmacsha256");
        SecretKeySpec spec = new SecretKeySpec(apiSecret.getBytes(charset), "hmacsha256");
        mac.init(spec);
        byte[] hexDigits = mac.doFinal(builder.toString().getBytes(charset));
        String sha = Base64.getEncoder().encodeToString(hexDigits);

        String authorization = String.format("api_key=\"%s\", algorithm=\"%s\", headers=\"%s\", signature=\"%s\"",
                apiKey, "hmac-sha256", "host date request-line", sha);

        HttpUrl httpUrl = HttpUrl.parse("https://" + url.getHost() + url.getPath()).newBuilder()
                .addQueryParameter("authorization", Base64.getEncoder().encodeToString(authorization.getBytes(charset)))
                .addQueryParameter("date", date)
                .addQueryParameter("host", url.getHost())
                .build();
        return httpUrl.toString();
    }
    public static class ResponseData {
        private int code;
        private String message;
        private String sid;
        private Data data;
        public int getCode() {
            return code;
        }
        public String getMessage() {
            return this.message;
        }
        public String getSid() {
            return sid;
        }
        public Data getData() {
            return data;
        }
    }

    public static class Data {
        private int status;
        private Result result;
        public int getStatus() {
            return status;
        }
        public Result getResult() {
            return result;
        }
    }

    public static class Result {
        int bg;
        int ed;
        String pgs;
        int[] rg;
        int sn;
        Ws[] ws;
        boolean ls;
        JsonObject vad;

        public Text getText() {
            Text text = new Text();
            StringBuilder sb = new StringBuilder();
            for (Ws wsElement : ws) {
                sb.append(wsElement.cw[0].w);
            }
            text.sn = this.sn;
            text.text = sb.toString();
            text.sn = this.sn;
            text.rg = this.rg;
            text.pgs = this.pgs;
            text.bg = this.bg;
            text.ed = this.ed;
            text.ls = this.ls;
            text.vad = this.vad == null ? null : this.vad;
            return text;
        }
    }

    public static class Ws {
        Cw[] cw;
        int bg;
        int ed;
    }

    public static class Cw {
        int sc;
        String w;
    }

    public static class Text {
        int sn;
        int bg;
        int ed;
        String text;
        String pgs;
        int[] rg;
        boolean deleted;
        boolean ls;
        JsonObject vad;

        @Override
        public String toString() {
            return "Text{" +
                    "bg=" + bg +
                    ", ed=" + ed +
                    ", ls=" + ls +
                    ", sn=" + sn +
                    ", text='" + text + '\'' +
                    ", pgs=" + pgs +
                    ", rg=" + Arrays.toString(rg) +
                    ", deleted=" + deleted +
                    ", vad=" + (vad == null ? "null" : vad.getAsJsonArray("ws").toString()) +
                    '}';
        }
    }

    // 解析返回数据，仅供参考
    public static class Decoder {
        private Text[] texts;
        private int defc = 10;

        public Decoder() {
            this.texts = new Text[this.defc];
        }

        public synchronized void decode(Text text) {
            if (text.sn >= this.defc) {
                this.resize();
            }
            if ("rpl".equals(text.pgs)) {
                for (int i = text.rg[0]; i <= text.rg[1]; i++) {
                    this.texts[i].deleted = true;
                }
            }
            this.texts[text.sn] = text;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (Text t : this.texts) {
                if (t != null && !t.deleted) {
                    sb.append(t.text);
                }
            }
            return sb.toString();
        }

        private void resize() {
            int oc = this.defc;
            this.defc <<= 1;
            Text[] old = this.texts;
            this.texts = new Text[this.defc];
            for (int i = 0; i < oc; i++) {
                this.texts[i] = old[i];
            }
        }

        public void discard() {
            for (int i = 0; i < this.texts.length; i++) {
                this.texts[i] = null;
            }
        }
    }
}

