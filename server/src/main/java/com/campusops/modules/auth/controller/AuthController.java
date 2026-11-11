package com.campusops.modules.auth.controller;

import com.campusops.common.Result;
import com.campusops.config.LoginUser;
import com.campusops.config.SecurityUtils;
import com.campusops.modules.auth.dto.LoginRequest;
import com.campusops.modules.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证接口
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** 登录，返回 token 与用户信息 */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }

    /** 当前登录用户信息 */
    @GetMapping("/profile")
    public Result<LoginUser> profile() {
        return Result.ok(SecurityUtils.current());
    }
}
