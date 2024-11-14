package com.lou.freegpt.vo;

import lombok.Data;

@Data
public class RegisterVo {
    private String username;

    private String email;

    private String verfityCode;

    private String password;
}
