package com.lou.freegpt.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lou.freegpt.common.anntation.LogInterface;
import com.lou.freegpt.domain.ChatUser;
import com.lou.freegpt.domain.MailMessages;
import com.lou.freegpt.enums.AjaxResult;
import com.lou.freegpt.enums.AllStatus;
import com.lou.freegpt.enums.BusinessType;
import com.lou.freegpt.enums.RedisStatus;
import com.lou.freegpt.service.ChatUserService;
import com.lou.freegpt.utils.JwtUtil;
import com.lou.freegpt.vo.RegisterVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisAccessor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/user")
@Slf4j
public class UserController {
    @Autowired
    private ChatUserService chatUserService;
    @Autowired
    private StringRedisTemplate redisTemplate;

    @PostMapping ("/logout")
    public AjaxResult logOut(@RequestHeader("Authorization") String token) {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
            Date expirationDate = JwtUtil.getExpirationDate(token);
            Date now = new Date();
            long dur = expirationDate.getTime() - now.getTime();
            if(dur > 0) {
                redisTemplate.opsForValue().set(RedisStatus.LOG_OUT_KEY + token, token, dur, TimeUnit.MILLISECONDS);
                return AjaxResult.success("退出成功!");
            }
        }
        return AjaxResult.fail("退出失败，请求无效!");
    }

    @GetMapping("/username")
    @LogInterface(title = "查询用户名", businessType = BusinessType.IMPORT)
    public AjaxResult checkUsername(@RequestParam("username") String username) {
        Optional<ChatUser> chatUser = Optional.ofNullable(chatUserService.getOne(new QueryWrapper<ChatUser>().eq("username", username)));
        if(chatUser.isPresent()) {
            return AjaxResult.error("用户名已存在或者已经封禁");
        }
        return AjaxResult.success();
    }

    @GetMapping("/email")
    public AjaxResult checkEmail(@RequestParam("email") String email) {
        Optional<ChatUser> chatUser = Optional.ofNullable(chatUserService.getOne(new QueryWrapper<ChatUser>().eq("email", email)));
        if(chatUser.isPresent()) {
            return AjaxResult.error("邮箱已经注册，请返回重新登录");
        }
        return AjaxResult.success();
    }


    @GetMapping("/email/code")
    public AjaxResult sendCode(@RequestParam("email") @NonNull String email) {
        return chatUserService.sendCode(email);
    }

    @PostMapping("/register")
    public AjaxResult register(@NonNull  @RequestBody RegisterVo registerVo) {
        log.info("处理注册请求:{}", registerVo.toString());
        int result = chatUserService.register(registerVo);
        return AjaxResult.toAjax(result);
    }


}
