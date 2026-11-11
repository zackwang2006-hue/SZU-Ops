package com.campusops.modules.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统用户（对应 sys_user 表）
 */
@Data
@TableName("sys_user")
public class SysUser {

    // 角色常量
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_DISPATCHER = "DISPATCHER";
    public static final String ROLE_TECHNICIAN = "TECHNICIAN";
    public static final String ROLE_USER = "USER";

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    /** BCrypt 加密后的密码 */
    private String password;

    private String realName;

    private String phone;

    /** ADMIN/DISPATCHER/TECHNICIAN/USER */
    private String role;

    /** 1启用 0停用 */
    private Integer status;

    private LocalDateTime createdAt;
}
