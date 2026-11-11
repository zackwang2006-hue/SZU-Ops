package com.campusops.modules.dashboard.controller;

import com.campusops.common.Result;
import com.campusops.modules.dashboard.service.DashboardService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 数据看板接口
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /** 看板总览：工单量/完成率/SLA/趋势/热力/品类分布/绩效 */
    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    public Result<Map<String, Object>> overview() {
        return Result.ok(dashboardService.overview());
    }
}
