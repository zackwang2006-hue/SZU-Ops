# SZU Ops Web 前端

校园设备智能运维系统前端（Vue 3 + Vite 5 + Element Plus + Pinia + ECharts）。

## 启动

```bash
npm install
npm run dev
```

开发服务器默认 http://localhost:5173 ，`/api` 已代理到后端 http://localhost:18081 （见 `vite.config.js`）。

构建：

```bash
npm run build
```

## 内置测试账号

| 账号 | 密码 | 角色 |
|------|------|------|
| admin | 123456 | 管理员（全部菜单） |
| dispatcher01 | 123456 | 调度员（看板/设备/工单/预警） |
| tech01 | 123456 | 维修工（工单管理，仅派给自己的工单） |
| user01 | 123456 | 师生用户（个人中心 + 新建报修） |

## 主题换肤

遵循 `../DESIGN.md`（google-labs-code/design.md 协议，YAML tokens + Markdown rationale）实现三套可切换主题：

| 主题 | 标识 | 说明 |
|------|------|------|
| 清透企业风 | `arco`（默认） | 灰底白卡、小圆角、字节蓝 #165DFF |
| Material 3 明亮风 | `material` | 大圆角、浅紫侧栏、动态紫 #6750A4 |
| 深色科技风 | `dark` | GitHub Dark 配色，运维驾驶舱质感 |

- 切换入口：主布局顶栏右侧的画笔图标（el-dropdown），登录页右上角同享。
- 选择持久化到 localStorage（键 `szu_theme`），启动时由 `src/store/theme.js` 的 `initTheme` 恢复。
- 实现机制：`src/styles/themes.css` 按 `html[data-theme]` 定义语义变量 `--c-*` 并覆盖 Element Plus `--el-*` 变量（dark 基于 `element-plus/theme-chalk/dark/css-vars.css` 微调）；ECharts 配色由 `src/styles/chart-theme.js` 提供三主题色板，切换主题时图表重设 option 联动换肤。

## 页面

- `/login` 登录
- `/dashboard` 数据看板（统计卡片 + 趋势/热力/品类图表 + 绩效表）
- `/assets` 设备管理
- `/orders` 工单管理（派单/开工/完工/验收/催单 + 时间线展开）
- `/prediction` 预测预警（风险等级、转巡检工单）
- `/mine` 个人中心（我的工单）
