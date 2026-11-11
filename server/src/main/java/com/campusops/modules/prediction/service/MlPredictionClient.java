package com.campusops.modules.prediction.service;

/**
 * 预留扩展点：ML 预测服务客户端（如对接 SZU 自研 Python 推理服务）
 * v1 默认使用规则引擎；后续提供 HTTP 实现替换 NoOp 即可。
 */
public interface MlPredictionClient {

    /**
     * 预测设备故障概率
     *
     * @param deviceId 设备 ID
     * @return 0-1 的故障概率；返回 null 表示 ML 服务不可用，退回规则引擎结果
     */
    Double predictFailureProbability(Long deviceId);
}
