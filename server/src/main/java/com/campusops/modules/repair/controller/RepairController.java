package com.campusops.modules.repair.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campusops.common.Result;
import com.campusops.modules.repair.dto.*;
import com.campusops.modules.repair.entity.RepairOrder;
import com.campusops.modules.repair.entity.RepairTimeline;
import com.campusops.modules.repair.service.RepairService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 报修工单接口：提交/查询/状态机流转/催单/取消/SLA 超时列表
 */
@Validated
@RestController
@RequestMapping("/api/repair/orders")
public class RepairController {

    private final RepairService repairService;

    public RepairController(RepairService repairService) {
        this.repairService = repairService;
    }

    /** 工单分页查询（多条件） */
    @GetMapping
    public Result<Page<RepairOrder>> page(
            @RequestParam(defaultValue = "1") @Min(1) int pageNum,
            @RequestParam(defaultValue = "10") @Min(1) @Max(200) int pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String building,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Long reporterId,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(required = false) String keyword) {
        return Result.ok(repairService.page(pageNum, pageSize, status, priority, building,
                category, reporterId, assigneeId, keyword));
    }

    /** SLA 超时列表（注意：需放在 /{id} 之前声明，避免路径冲突） */
    @GetMapping("/timeout")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER','TECHNICIAN')")
    public Result<List<RepairOrder>> timeout() {
        return Result.ok(repairService.timeoutList());
    }

    /** 工单详情（含时间线与评价） */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.ok(repairService.detail(id));
    }

    /** 提交报修 */
    @PostMapping
    public Result<RepairOrder> create(@Valid @RequestBody OrderCreateRequest request) {
        return Result.ok(repairService.create(request));
    }

    /** 派单（调度员/管理员） */
    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    public Result<Void> assign(@PathVariable Long id, @Valid @RequestBody AssignRequest request) {
        repairService.assign(id, request);
        return Result.ok();
    }

    /** 开工（接单维修工/管理角色） */
    @PostMapping("/{id}/start")
    @PreAuthorize("hasAnyRole('TECHNICIAN','ADMIN','DISPATCHER')")
    public Result<Void> start(@PathVariable Long id) {
        repairService.start(id);
        return Result.ok();
    }

    /** 完工（接单维修工/管理角色） */
    @PostMapping("/{id}/finish")
    @PreAuthorize("hasAnyRole('TECHNICIAN','ADMIN','DISPATCHER')")
    public Result<Void> finish(@PathVariable Long id, @Valid @RequestBody FinishRequest request) {
        repairService.finish(id, request);
        return Result.ok();
    }

    /** 验收（报修人/管理角色），含评分 */
    @PostMapping("/{id}/verify")
    public Result<Void> verify(@PathVariable Long id, @Valid @RequestBody VerifyRequest request) {
        repairService.verify(id, request);
        return Result.ok();
    }

    /** 催单（报修人/管理员） */
    @PostMapping("/{id}/urge")
    public Result<Void> urge(@PathVariable Long id) {
        repairService.urge(id);
        return Result.ok();
    }

    /** 取消（报修人/管理角色） */
    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id, @Valid @RequestBody CancelRequest request) {
        repairService.cancel(id, request);
        return Result.ok();
    }

    /** 工单时间线 */
    @GetMapping("/{id}/timeline")
    public Result<List<RepairTimeline>> timeline(@PathVariable Long id) {
        return Result.ok(repairService.listTimeline(id));
    }
}
