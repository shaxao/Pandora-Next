package com.lou.freegpt.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AllStatus {

    /**
     * 商品状态
     */
    READY("上架"),

    NOTREADY("下架"),

    CAOGAO("草稿"),

    /**
     * 用户状态
     */
    ADMIN("管理员"),
    NOTVER("未验证"),

    NORMALUSER("普通用户"),

    VIP("会员"),



    EXPIREVIP("已过期"),

    /**
     * 账号状态
     */
    LOSELINE("下线"),

    NORMAL("正常"),

    EXPIREACCOUNT("已过期"),

    ISDELETED("封号"),

    /**
     * TOKEN状态  跟账号一块用
     */

    /**
     * 公告状态
     */
    OPEN("启用"),

    CLOSE("禁用");


    /**
     * 类型
     */
    private final String type;
}
