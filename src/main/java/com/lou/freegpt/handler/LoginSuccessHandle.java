package com.lou.freegpt.handler;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lou.freegpt.common.anntation.LogInterface;
import com.lou.freegpt.domain.ChatUser;
import com.lou.freegpt.enums.BusinessType;
import com.lou.freegpt.mapper.ChatUserMapper;
import com.lou.freegpt.utils.JwtUtil;
import com.lou.freegpt.utils.UserDetailsNow;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class LoginSuccessHandle implements AuthenticationSuccessHandler {

    private static final ObjectMapper mapper = new ObjectMapper();
    private ChatUserMapper chatUserMapper;

    public LoginSuccessHandle(ChatUserMapper chatUserMapper) {
        this.chatUserMapper = chatUserMapper;
    }


    @Override
    @LogInterface(title = "登录", businessType = BusinessType.GRANT)
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        handleLogin(request,response);
    }

    private void responseError(HttpServletRequest request, HttpServletResponse response, Integer code, String message) {
        // 存储错误信息到会话中
        HttpSession session = request.getSession();
        session.setAttribute("errorMessage", message);
    }


    private void handleLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {
//        ObjectMapper mapper = new ObjectMapper();
        // 从 request 输入流中读取数据并转换为 Java 对象
        String username = UserDetailsNow.getUsername();
        log.info("{}，登录成功", username);
        ChatUser chatUser = chatUserMapper.selectOne(new QueryWrapper<ChatUser>().eq("username", username));

        String token = JwtUtil.token(username);
        JSONObject data = JSONUtil.createObj()
                .set("status", "登录成功")
                .set("token", token)
                .set("username", username)
                .set("email", chatUser.getEmail())
                .set("avatar", getImgBaseUrl(chatUser.getAvatar() == null ? "" : chatUser.getAvatar()));

        // 创建响应 JSON 对象
        JSONObject responseData = JSONUtil.createObj()
                .set("code", 200)
                .set("data", data);

        log.info("生成token={}", token);
        response.setHeader("Authorization", "Bearer " + token);
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        String jsonResponse = responseData.toString();
        try (PrintWriter out = response.getWriter()) {
            out.write(jsonResponse);
        } catch (IOException e) {
            // Handle the exception
            e.printStackTrace();
        }
    }

    private String getImgBaseUrl(String imageUrl) {
        try {
            // 创建URL对象
            URL url = new URL(imageUrl);
            // 打开连接
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            // 获取输入流
            InputStream inputStream = connection.getInputStream();
            // 创建字节数组输出流
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

            // 读取输入流中的数据并写入字节数组输出流
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, bytesRead);
            }

            // 关闭输入流
            inputStream.close();

            // 获取字节数组
            byte[] imageBytes = byteArrayOutputStream.toByteArray();

            // 将字节数组编码为Base64字符串
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            // 添加MIME类型前缀
            String base64ImageWithMimeType = "data:image/jpeg;base64," + base64Image;

            // 输出Base64编码字符串
            //System.out.println(base64ImageWithMimeType);
            return base64ImageWithMimeType;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
        }
}

class UserLogin {
    private String username;
    private String password;

    // getter 和 setter 方法
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
