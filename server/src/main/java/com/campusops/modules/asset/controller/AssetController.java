package com.campusops.modules.asset.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campusops.common.Result;
import com.campusops.modules.asset.dto.CategoryRequest;
import com.campusops.modules.asset.dto.DeviceRequest;
import com.campusops.modules.asset.entity.AssetCategory;
import com.campusops.modules.asset.entity.AssetDevice;
import com.campusops.modules.asset.service.AssetService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 设备资产接口：品类 + 设备台账
 */
@Validated
@RestController
@RequestMapping("/api/assets")
public class AssetController {

    private final AssetService assetService;

    public AssetController(AssetService assetService) {
        this.assetService = assetService;
    }

    // ---------- 品类 ----------

    @GetMapping("/categories")
    public Result<List<AssetCategory>> listCategories() {
        return Result.ok(assetService.listCategories());
    }

    @PostMapping("/categories")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<AssetCategory> createCategory(@Valid @RequestBody CategoryRequest request) {
        return Result.ok(assetService.createCategory(request));
    }

    @PutMapping("/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<AssetCategory> updateCategory(@PathVariable Long id,
                                                @Valid @RequestBody CategoryRequest request) {
        return Result.ok(assetService.updateCategory(id, request));
    }

    @DeleteMapping("/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        assetService.deleteCategory(id);
        return Result.ok();
    }

    // ---------- 设备台账 ----------

    /** 设备分页：支持品类/楼栋/状态/关键字过滤 */
    @GetMapping("/devices")
    public Result<Page<AssetDevice>> pageDevices(
            @RequestParam(defaultValue = "1") @Min(1) int pageNum,
            @RequestParam(defaultValue = "10") @Min(1) @Max(200) int pageSize,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String building,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        return Result.ok(assetService.pageDevices(pageNum, pageSize, categoryId, building, status, keyword));
    }

    @GetMapping("/devices/{id}")
    public Result<AssetDevice> getDevice(@PathVariable Long id) {
        return Result.ok(assetService.getDevice(id));
    }

    @PostMapping("/devices")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    public Result<AssetDevice> createDevice(@Valid @RequestBody DeviceRequest request) {
        return Result.ok(assetService.createDevice(request));
    }

    @PutMapping("/devices/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    public Result<AssetDevice> updateDevice(@PathVariable Long id,
                                            @Valid @RequestBody DeviceRequest request) {
        return Result.ok(assetService.updateDevice(id, request));
    }

    @DeleteMapping("/devices/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> deleteDevice(@PathVariable Long id) {
        assetService.deleteDevice(id);
        return Result.ok();
    }
}
