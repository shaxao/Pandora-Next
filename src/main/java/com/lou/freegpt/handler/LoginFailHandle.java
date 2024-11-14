package com.lou.freegpt.handler;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class LoginFailHandle implements AuthenticationFailureHandler {
    private static final ObjectMapper mapper = new ObjectMapper();
    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {

        JSONObject data = new JSONObject();
         data.putOnce("code", 402);
        if (exception instanceof BadCredentialsException) {
            data.putOnce("msg", "用户名或者密码错误");
        } else if (exception instanceof LockedException) {
            data.putOnce("msg", "账号已被锁定");
        } else if (exception instanceof DisabledException) {
            data.putOnce("msg", "账号已被封禁");
        } else if (exception instanceof AccountExpiredException) {
            data.putOnce("msg", "账号已过期");
        } else if (exception instanceof CredentialsExpiredException) {
            data.putOnce("msg", "认证码已过期");
        } else {
            data.putOnce("msg", "认证失败");
        }
        String responseJson = data.toStringPretty();
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        try (PrintWriter out = response.getWriter()) {
            out.write(responseJson);
        } catch (IOException e) {
            // Handle the exception
            e.printStackTrace();
        }
    }
}
