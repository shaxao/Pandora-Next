package com.lou.freegpt.utils;

import cn.hutool.core.util.RandomUtil;

/**
 * 验证码生成工具类
 */
public class VerifyCodeUtils {

    //生成验证码   6位数数字
    public static String createCode(){
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0;i < 6;i++){
            int a = RandomUtil.randomInt(0,9);
            stringBuilder.append(a);
        }
        return stringBuilder.toString();
    }

//    public static void main(String[] args) {
//        String code = createCode();
//        //1663829170-1635548810-1190691982190805414-616037547-109886220
//        System.out.println(code);
//    }
}
