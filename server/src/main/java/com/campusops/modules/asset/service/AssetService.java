package com.campusops.modules.asset.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campusops.common.BizException;
import com.campusops.modules.asset.dto.CategoryRequest;
import com.campusops.modules.asset.dto.DeviceRequest;
import com.campusops.modules.asset.entity.AssetCategory;
import com.campusops.modules.asset.entity.AssetDevice;
import com.campusops.modules.asset.mapper.AssetCategoryMapper;
import com.campusops.modules.asset.mapper.AssetDeviceMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 设备资产服务：品类与设备台账 CRUD、分页过滤
 */
@Service
public class AssetService {

    private final AssetCategoryMapper categoryMapper;
    private final AssetDeviceMapper deviceMapper;

    public AssetService(AssetCategoryMapper categoryMapper, AssetDeviceMapper deviceMapper) {
        this.categoryMapper = categoryMapper;
        this.deviceMapper = deviceMapper;
    }

    // ---------- 品类 ----------

    public List<AssetCategory> listCategories() {
        return categoryMapper.selectList(new LambdaQueryWrapper<AssetCategory>().orderByAsc(AssetCategory::getId));
    }

    public AssetCategory createCategory(CategoryRequest request) {
        checkCategoryCodeUnique(request.getCode(), null);
        AssetCategory category = new AssetCategory();
        BeanUtils.copyProperties(request, category);
        categoryMapper.insert(category);
        return category;
    }

    public AssetCategory updateCategory(Long id, CategoryRequest request) {
        AssetCategory category = mustGetCategory(id);
        checkCategoryCodeUnique(request.getCode(), id);
        BeanUtils.copyProperties(request, category);
        categoryMapper.updateById(category);
        return category;
    }

    public void deleteCategory(Long id) {
        mustGetCategory(id);
        Long count = deviceMapper.selectCount(
                new LambdaQueryWrapper<AssetDevice>().eq(AssetDevice::getCategoryId, id));
        if (count != null && count > 0) {
            throw new BizException("该品类下还有 " + count + " 台设备，无法删除");
        }
        categoryMapper.deleteById(id);
    }

    private void checkCategoryCodeUnique(String code, Long excludeId) {
        Long count = categoryMapper.selectCount(new LambdaQueryWrapper<AssetCategory>()
                .eq(AssetCategory::getCode, code)
                .ne(excludeId != null, AssetCategory::getId, excludeId));
        if (count != null && count > 0) {
            throw new BizException("品类编码已存在: " + code);
        }
    }

    public AssetCategory mustGetCategory(Long id) {
        AssetCategory category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BizException("品类不存在，id=" + id);
        }
        return category;
    }

    // ---------- 设备台账 ----------

    /** 设备分页查询：按品类/楼栋/状态过滤，keyword 匹配编号或名称 */
    public Page<AssetDevice> pageDevices(int pageNum, int pageSize, Long categoryId,
                                         String building, String status, String keyword) {
        LambdaQueryWrapper<AssetDevice> wrapper = new LambdaQueryWrapper<AssetDevice>()
                .eq(categoryId != null, AssetDevice::getCategoryId, categoryId)
                .eq(StringUtils.hasText(building), AssetDevice::getBuilding, building)
                .eq(StringUtils.hasText(status), AssetDevice::getStatus, status)
                .and(StringUtils.hasText(keyword), w -> w
                        .like(AssetDevice::getCode, keyword)
                        .or()
                        .like(AssetDevice::getName, keyword))
                .orderByDesc(AssetDevice::getId);
        Page<AssetDevice> page = deviceMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        // 填充品类名称（演示规模逐条查询，量级可控）
        page.getRecords().forEach(d -> {
            if (d.getCategoryId() != null) {
                AssetCategory category = categoryMapper.selectById(d.getCategoryId());
                if (category != null) {
                    d.setCategoryName(category.getName());
                }
            }
        });
        return page;
    }

    public AssetDevice getDevice(Long id) {
        return mustGetDevice(id);
    }

    public AssetDevice createDevice(DeviceRequest request) {
        mustGetCategory(request.getCategoryId());
        checkDeviceCodeUnique(request.getCode(), null);
        AssetDevice device = new AssetDevice();
        BeanUtils.copyProperties(request, device);
        if (!StringUtils.hasText(device.getStatus())) {
            device.setStatus(AssetDevice.STATUS_IN_USE);
        }
        device.setHealthScore(100);
        LocalDateTime now = LocalDateTime.now();
        device.setCreatedAt(now);
        device.setUpdatedAt(now);
        deviceMapper.insert(device);
        return device;
    }

    public AssetDevice updateDevice(Long id, DeviceRequest request) {
        AssetDevice device = mustGetDevice(id);
        mustGetCategory(request.getCategoryId());
        checkDeviceCodeUnique(request.getCode(), id);
        BeanUtils.copyProperties(request, device);
        device.setUpdatedAt(LocalDateTime.now());
        deviceMapper.updateById(device);
        return device;
    }

    public void deleteDevice(Long id) {
        mustGetDevice(id);
        deviceMapper.deleteById(id);
    }

    public AssetDevice mustGetDevice(Long id) {
        AssetDevice device = deviceMapper.selectById(id);
        if (device == null) {
            throw new BizException("设备不存在，id=" + id);
        }
        return device;
    }

    private void checkDeviceCodeUnique(String code, Long excludeId) {
        Long count = deviceMapper.selectCount(new LambdaQueryWrapper<AssetDevice>()
                .eq(AssetDevice::getCode, code)
                .ne(excludeId != null, AssetDevice::getId, excludeId));
        if (count != null && count > 0) {
            throw new BizException("设备编号已存在: " + code);
        }
    }
}
