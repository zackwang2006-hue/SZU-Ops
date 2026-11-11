# SZU Ops —— 校园设备智能运维系统 · 架构设计

> Author: ZackWang

> 融合两个开源项目的思路：
> - **ZackWang/school-assets**（Vue + SpringBoot）：资产管理、报修工单、权限骨架
> - **ZackWang/hawkeye**：预测性维护、SLA 监控、数据可视化亮点
>
> 技术栈统一为 **Vue 3 + Spring Boot 3 + MySQL**，符合国内校园场景主流选型。

---

## 1. 系统定位

面向高校后勤/信息中心的一站式设备运维平台：

1. **设备资产台账** —— 全校设备建档（品类、位置、采购、供应商、服役年限）
2. **报修工单闭环** —— 提交 → 派单 → 维修 → 验收 → 评价 全流程线上化
3. **预测性维护** —— 基于历史工单频率 + 设备服役年限的故障风险预警（规则引擎版，预留 ML 接口）
4. **运维数据看板** —— SLA 达标率、故障热力分布、维修人员绩效

## 2. 角色与权限（RBAC）

| 角色 | 说明 | 核心权限 |
|------|------|----------|
| 师生用户 USER | 报修发起人 | 提交报修、查看我的工单、催单、验收评价 |
| 维修工 TECHNICIAN | 工单执行人 | 接收工单、更新进度、上传维修记录 |
| 调度员 DISPATCHER | 派单与协调 | 工单分派、调整优先级、SLA 干预 |
| 管理员 ADMIN | 系统管理 | 设备台账、人员、品类、字典、看板全量数据 |

## 3. 模块划分

```
campus-ops
├── 基础平台
│   ├── 用户认证（JWT）与 RBAC 权限
│   └── 数据字典（故障类型、设备品类、楼栋区域）
├── 设备资产 Asset
│   ├── 设备台账（CRUD、二维码、位置）
│   ├── 设备品类 / 供应商
│   └── 设备健康度（由预测模块计算）
├── 报修工单 RepairOrder
│   ├── 工单提交（类型、位置、图片、描述）
│   ├── 状态机流转（待派单→已派单→处理中→待验收→已完成/已取消）
│   ├── 派单（手动 + 按技能/负载建议）
│   ├── 催单与备注时间线
│   └── 验收与满意度评价
├── 预测性维护 Prediction（亮点）
│   ├── 规则引擎：设备故障频率 + 服役年限 + 环境标签 → 风险分
│   ├── 风险等级：LOW / MEDIUM / HIGH / CRITICAL
│   ├── 预警工单：高风险设备自动生成巡检建议
│   └── （预留）ML 服务接口：对接 SZU 自研 Python 推理服务
└── 数据看板 Dashboard
    ├── SLA 监控（超时预警、分类 SLA 阈值）
    ├── 故障热力（按楼栋/品类的分布统计）
    └── 绩效（平均修复时长、完成率、满意度）
```

## 4. 技术选型

| 层 | 技术 | 说明 |
|----|------|------|
| 前端 | Vue 3 + Vite + Element Plus + ECharts + Pinia | school-assets 同系，生态成熟 |
| 后端 | Spring Boot 3 + Spring Security + JWT + MyBatis-Plus | Java 17+ |
| 数据库 | MySQL 8（开发期可 H2 替代） | 关系型，适合台账/工单 |
| 缓存 | Redis（可选） | 工单热点数据、Token |
| 预测 | 内置规则引擎；预留 REST 接口接 ML 服务 | Hawkeye 思路的轻量化落地 |

## 5. 数据库设计（核心表）

```sql
-- 用户
sys_user(id, username, password, real_name, phone, role, status, created_at)

-- 设备品类
asset_category(id, name, code, expected_lifespan_years, sla_hours)

-- 设备台账
asset_device(id, code, name, category_id, building, room,
             purchase_date, supplier, price, status,        -- IN_USE/REPAIRING/SCRAPPED
             health_score, created_at, updated_at)

-- 报修工单
repair_order(id, order_no, title, category, device_id,
             reporter_id, building, room, description, images,
             priority,            -- LOW/MEDIUM/HIGH/URGENT
             status,              -- PENDING/ASSIGNED/PROCESSING/TO_VERIFY/COMPLETED/CANCELLED
             assignee_id, sla_deadline,
             created_at, assigned_at, finished_at, verified_at)

-- 工单时间线（派单/进度/催单/验收记录）
repair_timeline(id, order_id, action, operator_id, content, images, created_at)

-- 工单评价
repair_evaluation(id, order_id, score, comment, created_at)

-- 预测性维护风险记录
prediction_risk(id, device_id, risk_level, risk_score, reason,
                predicted_failure_date, status, created_at)   -- status: OPEN/DISMISSED/CONVERTED

-- 巡检建议工单（由高风险转化，复用 repair_order，category='INSPECTION'）
```

## 6. 工单状态机

```
PENDING(待派单)
  │ 派单 assign(technicianId)
  ▼
ASSIGNED(已派单) ──拒单/改派──▶ PENDING
  │ 接单开工 start()
  ▼
PROCESSING(处理中)
  │ 完成 finish(report)
  ▼
TO_VERIFY(待验收)
  │ 验收 verify(pass)          │ 验收不通过 verify(fail)
  ▼                            ▼
COMPLETED(已完成)          PROCESSING
任意进行中状态 ──取消──▶ CANCELLED
```

SLA 规则：每品类配置 `sla_hours`，工单创建时生成 `sla_deadline`，超时未完成的工单进入超时列表并提升优先级。

## 7. 预测性维护（规则引擎 v1）

借鉴 Hawkeye 的规则预测层，用可解释的规则代替黑盒模型：

```
风险分 risk_score =
    w1 * 近90天同设备故障次数（归一化）
  + w2 * 近90天同楼栋同品类故障密度
  + w3 * 服役年限 / 品类预期寿命
  + w4 * 当前是否处于 REPAIRING 状态

等级映射：≥0.8 CRITICAL，≥0.6 HIGH，≥0.4 MEDIUM，其余 LOW
预测故障日：按历史故障间隔均值外推
```

调度：每日定时任务（@Scheduled）全量重算；高风险设备生成 `prediction_risk` 记录，可一键转为巡检工单。
扩展点：`MlPredictionClient` 接口，后续可接 SZU 自研 Python 模型服务。

## 8. API 概览（REST）

```
POST   /api/auth/login
GET    /api/assets/devices            设备分页（按品类/楼栋/状态过滤）
POST   /api/assets/devices            新建设备
GET    /api/repair/orders             工单分页（多条件）
POST   /api/repair/orders             提交报修
POST   /api/repair/orders/{id}/assign 派单
POST   /api/repair/orders/{id}/start  开工
POST   /api/repair/orders/{id}/finish 完工
POST   /api/repair/orders/{id}/verify 验收（含评分）
POST   /api/repair/orders/{id}/urge   催单
GET    /api/repair/orders/timeout     SLA 超时列表
GET    /api/prediction/risks          风险预警列表
POST   /api/prediction/risks/{id}/convert  转巡检工单
GET    /api/dashboard/overview        看板总览（工单趋势/SLA/热力/绩效）
```

## 9. 里程碑

- **M1（本次交付）**：架构文档 + 后端核心模块（认证/设备/工单状态机/规则预测/看板 API）+ 前端核心页面
- **M2**：Redis 缓存、消息通知（邮件/SZU 校内通知渠道）、文件上传 OSS
- **M3**：ML 预测服务对接、移动端小程序报修入口（可复用 TurboRepair 思路）
