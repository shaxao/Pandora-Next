package com.lou.freegpt.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lou.freegpt.domain.ChatUser;
import com.lou.freegpt.domain.MailMessages;
import com.lou.freegpt.enums.AjaxResult;
import com.lou.freegpt.enums.AllStatus;
import com.lou.freegpt.enums.RedisStatus;
import com.lou.freegpt.service.ChatUserService;
import com.lou.freegpt.mapper.ChatUserMapper;
import com.lou.freegpt.utils.JwtUtil;
import com.lou.freegpt.utils.VerifyCodeUtils;
import com.lou.freegpt.vo.LoginUserVo;
import com.lou.freegpt.vo.RegisterVo;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.parameters.P;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.spec.ECField;
import java.util.Base64;
import java.util.Date;
import java.util.EnumMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
* @author Administrator
* @description 针对表【chat_user(聊天应用用户表)】的数据库操作Service实现
* @createDate 2024-05-30 09:08:32
*/
@Service
@RequiredArgsConstructor
public class ChatUserServiceImpl extends ServiceImpl<ChatUserMapper, ChatUser>
    implements ChatUserService{
    @Autowired
    private ChatUserMapper chatUserMapper;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private MailMessages mailMessages;
    @Autowired
    AuthenticationManager authenticationManager;

    @Override
    public int register(@NonNull RegisterVo registerVo) {
        Optional<ChatUser> chatUser = Optional.ofNullable(chatUserMapper.selectOne(new QueryWrapper<ChatUser>()
                .eq("email", registerVo.getEmail())
                .eq("username", registerVo.getUsername())));
        if(!chatUser.isPresent()) {
            String code = redisTemplate.opsForValue().get(RedisStatus.USER_CODE_KEY + registerVo.getEmail());
            if(code != null && registerVo.getVerfityCode().equals(code)) {
                BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
                String password = passwordEncoder.encode(registerVo.getPassword());
                ChatUser chatUser1 = new ChatUser();
                chatUser1.setUserStatus(AllStatus.NORMALUSER.getType());
                chatUser1.setUsername(registerVo.getUsername());
                chatUser1.setPassword(password);
                chatUser1.setCreatedAt(new Date());
                chatUser1.setAvatar("https://image.qipusong.site/file/823b8c196327a2dbeac3c.jpg");
                chatUser1.setEmail(registerVo.getEmail());
                chatUser1.setIsDeleted(0);
                return chatUserMapper.insert(chatUser1);
            }
        }
        return 0;
    }

    @Override
    public AjaxResult sendCode(String email) {
        String code = VerifyCodeUtils.createCode();
        mailMessages.createMessages(code, email);
        if(mailMessages.send()) {
            redisTemplate.opsForValue().set(RedisStatus.USER_CODE_KEY + email, code, RedisStatus.USER_CODE_TTL, TimeUnit.MINUTES);
            return AjaxResult.success("发送成功");
        }
        return AjaxResult.fail("发送失败");
    }

    @Override
    public AjaxResult login(String username, String password) {
        ChatUser chatUser = chatUserMapper.selectOne(new QueryWrapper<ChatUser>().eq("username", username));
        if (chatUser == null) {
            // 不存在就注册一个
            BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
            String passwords = passwordEncoder.encode(password);
            ChatUser chatUser1 = new ChatUser();
            chatUser1.setUserStatus(AllStatus.NORMALUSER.getType());
            chatUser1.setUsername(username);
            chatUser1.setPassword(passwords);
            chatUser1.setCreatedAt(new Date());
            chatUser1.setEmail("linuxdo@linuxdo.com");
            chatUser1.setIsDeleted(0);
            chatUser1.setAvatar("https://image.qipusong.site/file/823b8c196327a2dbeac3c.jpg");
            chatUserMapper.insert(chatUser1);
            chatUser = chatUser1;
            // return AjaxResult.fail("用户不存在");
        }
        Authentication authenticate = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
        SecurityContextHolder.getContext().setAuthentication(authenticate);
        String token = JwtUtil.token(username);
        LoginUserVo loginUserVo = new LoginUserVo();
        loginUserVo.setToken(token);
        loginUserVo.setAvatar(getImgBaseUrl(chatUser.getAvatar() == null ? "" : chatUser.getAvatar()));
        loginUserVo.setUsername(username);
        loginUserVo.setEmail(chatUser.getEmail());

        return AjaxResult.success("登录成功", loginUserVo);
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




