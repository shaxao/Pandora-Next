package com.lou.freegpt.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
public class LoginUserVo implements Serializable {
    private String token;
    private String username;
    private String email;
    private String avatar;

}
