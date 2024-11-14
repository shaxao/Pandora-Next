package com.lou.freegpt;

import com.lou.freegpt.dao.MessageDao;
import com.lou.freegpt.domain.ChatUser;
import com.lou.freegpt.enums.AllStatus;
import com.lou.freegpt.mapper.ChatUserMapper;
import okhttp3.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.IOException;
import java.util.Date;

@SpringBootTest
public class UserTest {
    @Autowired
    private MessageDao messageDao;
//    @Autowired
//    private ChatUserMapper chatUserMapper;
//
//    @Test
//    void testUser() {
//        ChatUser chatUser = new ChatUser();
//        chatUser.setUserLevel(1);
//        chatUser.setUserStatus(AllStatus.ADMIN.getType());
//        chatUser.setEmail("415240147@qq.com");
//        chatUser.setUsername("admin");
//        chatUser.setIsDeleted(0);
//        chatUser.setCreatedAt(new Date());
//        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
//        String password = bCryptPasswordEncoder.encode("mhchat2000314..");
//        chatUser.setPassword(password);
//        chatUserMapper.insert(chatUser);
//    }

//    @Test
//    void testSearch() {
//
//    }

//    @Test
//    void deleteConver() {
//        int i = messageDao.deleteConverById("75819f69-9a94-4bf3-a326-42fc3229a614");
//        System.out.println(i);
//    }

//    @Test
//    void updateConver() {
//        int i = messageDao.updateTitleById("5773e21c-7d74-4821-842f-480f22eb2b56", "更新以后");
//        System.out.println(i);
//    }
}
