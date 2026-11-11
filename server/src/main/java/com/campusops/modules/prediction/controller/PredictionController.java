package com.campusops.modules.prediction.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campusops.common.Result;
import com.campusops.modules.prediction.entity.PredictionRisk;
import com.campusops.modules.prediction.service.PredictionService;
import com.campusops.modules.repair.entity.RepairOrder;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 预测性维护接口：风险预警查询/转巡检/忽略/手动重算
 */
@Validated
@RestController
@RequestMapping("/api/prediction/risks")
@PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
public class PredictionController {

    private final PredictionService predictionService;

    public PredictionController(PredictionService predictionService) {
        this.predictionService = predictionService;
    }

    /** 风险预警列表（可按等级/状态过滤） */
    @GetMapping
    public Result<Page<PredictionRisk>> page(
            @RequestParam(defaultValue = "1") @Min(1) int pageNum,
            @RequestParam(defaultValue = "10") @Min(1) @Max(200) int pageSize,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String status) {
        return Result.ok(predictionService.pageRisks(pageNum, pageSize, level, status));
    }

    /** 高风险一键转巡检工单 */
    @PostMapping("/{id}/convert")
    public Result<RepairOrder> convert(@PathVariable Long id) {
        return Result.ok(predictionService.convertToInspection(id));
    }

    /** 忽略风险 */
    @PostMapping("/{id}/dismiss")
    public Result<Void> dismiss(@PathVariable Long id) {
        predictionService.dismiss(id);
        return Result.ok();
    }

    /** 手动触发全量重算（每日定时任务之外的管理入口） */
    @PostMapping("/recalculate")
    public Result<Integer> recalculate() {
        return Result.ok(predictionService.recalculate());
    }
}
