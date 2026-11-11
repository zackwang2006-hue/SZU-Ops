# SZU Ops Server —— 校园设备智能运维系统后端

基于 **Spring Boot 3.3 + Java 17 + Spring Security + JWT + MyBatis-Plus**，默认使用 **H2 内存库**开箱即跑，可切换 MySQL 8。架构与接口定义见 `../docs/ARCHITECTURE.md`。

## 启动

```bash
# 方式一：Maven（默认 H2 内存库，首次启动自动建表并注入演示数据）
mvn spring-boot:run

# 方式二：IDE 直接运行 com.campusops.SZU OpsApplication

# 使用 MySQL：先建库 CREATE DATABASE campus_ops DEFAULT CHARSET utf8mb4;
# 再改 application-mysql.yml 里的账号密码，然后：
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

服务端口：`8080`。H2 控制台：`http://localhost:18081/h2-console`（JDBC URL：`jdbc:h2:mem:campusops`，用户名 `sa`，空密码）。

## 内置账号（密码均为 `123456`，BCrypt 存储）

| 账号 | 角色 | 说明 |
|------|------|------|
| admin | ADMIN | 管理员 |
| dispatcher01 | DISPATCHER | 调度员（派单/预测/看板） |
| tech01 / tech02 | TECHNICIAN | 维修工 |
| user01 | USER | 师生用户（报修人） |

演示数据：8 个品类、22 台设备、34 条工单（创建时间分布在近 90 天内，覆盖 PENDING/ASSIGNED/PROCESSING/TO_VERIFY/COMPLETED/CANCELLED 全状态），启动时自动完成一次预测重算。

## 示例 curl

```bash
# 1. 登录拿 token
TOKEN=$(curl -s -X POST http://localhost:18081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}' | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')

# 2. 设备分页（按楼栋过滤）
curl "http://localhost:18081/api/assets/devices?pageNum=1&pageSize=5&building=教学楼A" \
  -H "Authorization: Bearer $TOKEN"

# 3. 提交报修（用 user01 登录的 token）
curl -X POST http://localhost:18081/api/repair/orders \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"title":"教室电脑无法开机","deviceId":1,"building":"教学楼A","room":"301室","description":"按电源键无反应","priority":"HIGH"}'

# 4. 派单（dispatcher01/admin）
curl -X POST http://localhost:18081/api/repair/orders/35/assign \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"technicianId":3}'

# 5. 状态机流转：开工 → 完工 → 验收
curl -X POST http://localhost:18081/api/repair/orders/35/start   -H "Authorization: Bearer $TOKEN"
curl -X POST http://localhost:18081/api/repair/orders/35/finish  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" -d '{"report":"已更换电源并测试通过"}'
curl -X POST http://localhost:18081/api/repair/orders/35/verify  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" -d '{"pass":true,"score":5,"comment":"很快修好了"}'

# 6. 催单 / 取消 / SLA 超时列表
curl -X POST http://localhost:18081/api/repair/orders/35/urge    -H "Authorization: Bearer $TOKEN"
curl -X POST http://localhost:18081/api/repair/orders/35/cancel  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" -d '{"reason":"误报"}'
curl http://localhost:18081/api/repair/orders/timeout            -H "Authorization: Bearer $TOKEN"

# 7. 风险预警列表 / 转巡检工单 / 手动重算（admin/dispatcher01）
curl "http://localhost:18081/api/prediction/risks?status=OPEN"   -H "Authorization: Bearer $TOKEN"
curl -X POST http://localhost:18081/api/prediction/risks/1/convert      -H "Authorization: Bearer $TOKEN"
curl -X POST http://localhost:18081/api/prediction/risks/recalculate    -H "Authorization: Bearer $TOKEN"

# 8. 数据看板（admin/dispatcher01）
curl http://localhost:18081/api/dashboard/overview               -H "Authorization: Bearer $TOKEN"
```

## 目录结构

```
src/main/java/com/campusops
├── common        Result 统一返回、BizException、全局异常处理
├── config        Security 配置、JWT 工具与过滤器、MyBatisPlus 分页、演示数据初始化
└── modules
    ├── auth        登录签发 JWT（BCrypt）
    ├── asset       设备品类 + 设备台账 CRUD/分页过滤
    ├── repair      工单状态机（assign/start/finish/verify/urge/cancel）+ 时间线 + SLA
    ├── prediction  规则引擎（每日 @Scheduled 重算）+ 风险转巡检 + MlPredictionClient 预留
    └── dashboard   overview 聚合（工单量/完成率/SLA/趋势/热力/品类/绩效）
```
