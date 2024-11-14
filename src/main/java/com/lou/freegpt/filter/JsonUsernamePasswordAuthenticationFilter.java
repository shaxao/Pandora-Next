package com.lou.freegpt.filter;

import cn.hutool.json.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lou.freegpt.enums.RedisStatus;
import com.lou.freegpt.service.AppConfigService;
import com.lou.freegpt.utils.JwtUtil;
import com.lou.freegpt.utils.VerUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;


@Slf4j
@Component
public class JsonUsernamePasswordAuthenticationFilter extends OncePerRequestFilter {
    private UserDetailsService userDetailsService;
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String[] PERMITS = new String[]{"/login","/login.html", "/index.html","/api/user/register", "/api/user/username", "/ws/api/audio", "/api/user/email/code","/api/user/phone","/api/user/email"};
    public static final List<String> PERMITS_LIST;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private AppConfigService appConfigService;

    static {
        PERMITS_LIST = Arrays.asList(PERMITS);
    }

    public JsonUsernamePasswordAuthenticationFilter(UserDetailsService userDetailsService){
        this.userDetailsService = userDetailsService;
    }


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String requestURI = request.getRequestURI();
        log.info("请求URI：{}", requestURI);
        if (requestURI.startsWith("/api")){
            requestURI = requestURI.substring(4);
        }
        log.info("分割后请求URI：{}", requestURI);
        try {
            // 对于其他需要验证的请求
            if ( !(requestURI.startsWith("/css") || requestURI.startsWith("/svg") || requestURI.startsWith("/js") || requestURI.startsWith("/share")) && !PERMITS_LIST.contains(requestURI) && !handleTokenValidation(request, response, requestURI)) {
                // 如果token验证失败，handleTokenValidation 方法已设置响应并返回false
                return;
            }

        } catch (Exception e) {
            e.printStackTrace();
            SecurityContextHolder.clearContext();
            responseError(request, response,50012,"后台管理请求处理错误");
        }
        log.info("通过验证，进入下一个过滤链");
        // 继续过滤链处理（对于登录和有效token的请求）
        if (request.getRequestURI().startsWith("/api")) {
            HttpServletRequestWrapper wrapper = new HttpServletRequestWrapper(request) {
                @Override
                public String getRequestURI() {
                    return request.getRequestURI().substring(4); // Remove /api prefix
                }

                @Override
                public String getServletPath() {
                    return request.getServletPath().substring(4);
                }
            };
            filterChain.doFilter(wrapper, response);
        } else {
            filterChain.doFilter(request, response);
        }
    }

    private void responseError(HttpServletRequest request, HttpServletResponse response, Integer code, String message) {
        // 存储错误信息到会话中
        JSONObject jsonObject = new JSONObject();
        jsonObject.putOnce("code", code);
        jsonObject.putOnce("msg", message);
        String responseJson = jsonObject.toStringPretty();

        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        try (PrintWriter out = response.getWriter()) {
            out.write(responseJson);
        } catch (IOException e) {
            // Handle the exception
            e.printStackTrace();
        }
    }



    private boolean handleTokenValidation(HttpServletRequest request, HttpServletResponse response, String requestURI) {
        String token = request.getHeader("Authorization");
        log.info("请求中token={}", token);
        if (token != null && token.startsWith("Bearer ")) {
            String jwtToken = token.substring(7);
            String username = JwtUtil.getUsernameFromToken(jwtToken);
            Boolean isValToken = redisTemplate.hasKey(RedisStatus.LOG_OUT_KEY + jwtToken);
            try {
                if (!isValToken && JwtUtil.verify(jwtToken) && username != null) {
                    if(requestURI.startsWith("/api")) {
                        String turnstile = request.getHeader("turnstile");
                        //log.info("turnstile: {}", turnstile);
                        boolean pass = VerUtils.turnstile(turnstile, appConfigService.getSecretKey());
                        if(!pass) {
                            responseError(request, response,400,"验证未通过");
                            return false;
                        }
                        log.info("turnstile验证通过");
                    }
                    UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                    UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    usernamePasswordAuthenticationToken
                            .setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
                    return true;
                }
            } catch (Exception e) {
                log.error("Token验证失败", e);
            }
        }
        responseError(request, response,400,"无效的token");
        return false;
    }


}

