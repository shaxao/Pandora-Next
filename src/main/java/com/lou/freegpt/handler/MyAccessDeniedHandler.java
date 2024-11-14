package com.lou.freegpt.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.io.PrintWriter;

public class MyAccessDeniedHandler implements AccessDeniedHandler {
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
        if (request.getRequestURI().equals("/pandora/share")){
            System.out.println("requestURI:" + request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Unauthorized");
            response.getWriter().flush();
        }
        // 检查是否是 AJAX 请求
        if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
            // AJAX 请求
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            PrintWriter out = response.getWriter();
            String jsonResponse = "{\"message\": \"权限不足\", \"code\": 401}";
            out.print(jsonResponse);
            out.flush();
        } else {
            // 非 AJAX 请求，执行重定向
            response.sendRedirect("/noPermission.html");
        }
    }
}
