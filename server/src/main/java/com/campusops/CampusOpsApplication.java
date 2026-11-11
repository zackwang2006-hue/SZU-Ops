package com.campusops;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 校园设备智能运维系统启动类
 */
@EnableScheduling
@MapperScan({
        "com.campusops.modules.auth.mapper",
        "com.campusops.modules.asset.mapper",
        "com.campusops.modules.repair.mapper",
        "com.campusops.modules.prediction.mapper"
})
@SpringBootApplication
public class CampusOpsApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusOpsApplication.class, args);
    }
}
