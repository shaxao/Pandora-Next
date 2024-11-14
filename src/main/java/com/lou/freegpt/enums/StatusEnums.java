package com.lou.freegpt.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum StatusEnums {

    /**
     * 禁用
     */
    DISABLE("2", "禁用"),
    /**
     * 启用
     */
    ENABLE("1", "启用"),;

    private final String code;

    private final String name;
}
