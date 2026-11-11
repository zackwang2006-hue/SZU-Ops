package com.campusops.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录用户主体：写入 JWT 并存入 SecurityContext
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginUser {

    private Long id;
    private String username;
    private String realName;
    private String role; // ADMIN/DISPATCHER/TECHNICIAN/USER
}
