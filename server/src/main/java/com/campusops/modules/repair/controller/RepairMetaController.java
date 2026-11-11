package com.campusops.modules.repair.controller;

import com.campusops.common.Result;
import com.campusops.modules.auth.entity.SysUser;
import com.campusops.modules.repair.service.RepairService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 工单相关元数据接口（派单候选人等）
 */
@RestController
@RequestMapping("/api/repair")
public class RepairMetaController {

    private final RepairService repairService;

    public RepairMetaController(RepairService repairService) {
        this.repairService = repairService;
    }

    /** 维修工列表（派单选择用） */
    @GetMapping("/technicians")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    public Result<List<SysUser>> technicians() {
        return Result.ok(repairService.listTechnicians());
    }
}
