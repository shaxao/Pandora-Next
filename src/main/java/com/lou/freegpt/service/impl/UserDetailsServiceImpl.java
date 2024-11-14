package com.lou.freegpt.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lou.freegpt.domain.ChatUser;
import com.lou.freegpt.mapper.ChatUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 用户认证
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    @Autowired
    private ChatUserMapper chatUsersMapper;


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        System.out.println("处理登录:" + username);
        QueryWrapper<ChatUser> wrapper = new QueryWrapper<ChatUser>().eq("username",username);
        ChatUser chatUsers = chatUsersMapper.selectOne(wrapper);
        if(chatUsers == null || chatUsers.getIsDeleted() == 1){
            return null;
        }
        System.out.println("查询数据库用户:" +chatUsers.toString());
        List<SimpleGrantedAuthority> authorities = Arrays.asList(new SimpleGrantedAuthority("ROLE_" + chatUsers.getUserStatus()));
        //封装为UserDetails对象
        UserDetails userDetails = User.withUsername(username)
                .password(chatUsers.getPassword())
                .authorities(authorities)
                .build();

        return userDetails;
    }
}
