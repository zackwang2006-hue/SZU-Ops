package com.campusops.modules.prediction.service;

import org.springframework.stereotype.Component;

/**
 * MlPredictionClient 默认空实现：不接入 ML 服务时始终返回 null（退回规则引擎）
 */
@Component
public class NoOpMlPredictionClient implements MlPredictionClient {

    @Override
    public Double predictFailureProbability(Long deviceId) {
        return null;
    }
}
