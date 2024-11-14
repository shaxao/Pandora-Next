package com.lou.freegpt.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

public class UserDetailsNow {
    public static String getUsername(){
        SecurityContext context = SecurityContextHolder.getContext();
        Authentication authentication = context.getAuthentication();

        if (authentication != null && authentication.getPrincipal() != null) {
            Object principal = authentication.getPrincipal();
            // 检查 principal 是否为 UserDetails 类型
            if (principal instanceof UserDetails) {
                UserDetails userDetails = (UserDetails) principal;
                System.out.println("获取登录用户信息：" + userDetails.getUsername());
                return userDetails.getUsername();
            } else if (principal instanceof String) {
                // 主体可能只是一个字符串（例如用户名）
                System.out.println("获取用户名：" + principal);
                return (String) principal;
            }
        }
        return null;
    }
}
