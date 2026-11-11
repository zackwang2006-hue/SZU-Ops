# SZU Ops · 校园设备智能运维系统

融合两个开源项目的思路从零构建：

- **ZackWang/school-assets**（80⭐）：资产管理 + 报修工单 + RBAC 骨架
- **ZackWang/hawkeye**：预测性维护、SLA 监控、数据可视化亮点

## 功能

| 模块 | 说明 |
|------|------|
| 设备资产 | 设备台账（品类/楼栋/供应商/服役年限）、健康度评分 |
| 报修工单 | 提交 → 派单 → 开工 → 完工 → 验收评价 全流程状态机，催单/取消、SLA 超时自动升级优先级 |
| 预测性维护 | 规则引擎（故障频率 + 楼栋密度 + 服役年限 + 维修状态）每日重算风险，高风险一键转巡检工单；预留 ML 服务接口 |
| 数据看板 | 今日工单、SLA 达标率、30 天趋势、楼栋热力、品类分布、维修人员绩效 |

## 技术栈

后端：Spring Boot 3 + Spring Security(JWT) + MyBatis-Plus + H2（默认，可切 MySQL 8）
前端：Vue 3 + Vite + Element Plus + ECharts + Pinia

## 快速开始

```bash
# 后端（端口 18081，H2 内存库开箱即跑，无需装数据库）
cd server && mvn spring-boot:run

# 前端（端口 5173，/api 已代理到后端）
cd web && npm install && npm run dev
```

打开 http://localhost:5173 ，内置账号（密码均为 `123456`）：

| 账号 | 角色 | 能做什么 |
|------|------|----------|
| admin | 管理员 | 全部功能 + 看板 + 预警 |
| dispatcher01 | 调度员 | 工单分派、SLA 干预 |
| tech01 / tech02 | 维修工 | 接单、开工、完工 |
| user01 | 师生 | 提交报修、验收评价 |

首次启动自动注入演示数据：8 个品类、22 台设备、34 条覆盖各状态的工单（分布在近 90 天内）、2 条风险预警。

## 目录

```
campus-ops/
├── docs/ARCHITECTURE.md   # 完整架构设计（模块/表结构/状态机/预测公式/API）
├── server/                # Spring Boot 后端（详见 server/README.md）
└── web/                   # Vue 3 前端（详见 web/README.md）
```

---

Author: **ZackWang**
