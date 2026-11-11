---
name: SZU Ops Design System
author: ZackWang
version: 1.0.0
spec: google-labs-code/design.md (alpha)

# 三套主题通过 CSS 变量切换，html[data-theme] 取值：
#   arco（默认） / material / dark

themes:
  arco:
    label: 清透企业风
    colors:
      primary: "#165DFF"
      primary-light: "#E8F3FF"
      success: "#00B42A"
      warning: "#FF7D00"
      danger: "#F53F3F"
      bg-page: "#F2F3F5"
      bg-card: "#FFFFFF"
      bg-sidebar: "#FFFFFF"
      text-primary: "#1D2129"
      text-regular: "#4E5969"
      text-secondary: "#86909C"
      border: "#E5E6EB"
    shape:
      radius-sm: 2px
      radius-md: 4px
      radius-lg: 8px
    chart: ["#165DFF", "#00B42A", "#FF7D00", "#F53F3F", "#722ED1", "#0FC6C2"]

  material:
    label: Material 3 明亮风
    colors:
      primary: "#6750A4"
      primary-light: "#EADDFF"
      success: "#006A6A"
      warning: "#7D5700"
      danger: "#BA1A1A"
      bg-page: "#FEF7FF"
      bg-card: "#FFFFFF"
      bg-sidebar: "#F3EDF7"
      text-primary: "#1D1B20"
      text-regular: "#49454F"
      text-secondary: "#79747E"
      border: "#CAC4D0"
    shape:
      radius-sm: 8px
      radius-md: 12px
      radius-lg: 16px
    chart: ["#6750A4", "#006A6A", "#7D5700", "#BA1A1A", "#0061A4", "#984061"]

  dark:
    label: 深色科技风
    colors:
      primary: "#58A6FF"
      primary-light: "#1F6FEB33"
      success: "#3FB950"
      warning: "#D29922"
      danger: "#F85149"
      bg-page: "#0D1117"
      bg-card: "#161B22"
      bg-sidebar: "#161B22"
      text-primary: "#C9D1D9"
      text-regular: "#8B949E"
      text-secondary: "#6E7681"
      border: "#21262D"
    shape:
      radius-sm: 4px
      radius-md: 6px
      radius-lg: 8px
    chart: ["#58A6FF", "#3FB950", "#D29922", "#F85149", "#BC8CFF", "#39C5CF"]

typography:
  font-family: "'Helvetica Neue', 'PingFang SC', 'Microsoft YaHei', sans-serif"
  sizes: { xs: 12px, sm: 13px, base: 14px, lg: 16px, xl: 20px, xxl: 24px }
  weights: { regular: 400, medium: 500 }

spacing:
  page-padding: 16px
  card-padding: 16px
  card-gap: 12px

components:
  sidebar: "宽 220px，主题 bg-sidebar；选中项用 primary-light 底 + primary 字"
  stat-card: "bg-card + border + radius-md；标签 text-secondary 12px，数值 xxl/medium"
  table: "表头 bg-page，行 hover 用 primary-light 30% 透明度"
  tag: "status 色用语义色 10% 底 + 语义色字"
---

# SZU Ops 设计规范

> Author: ZackWang · 遵循 google-labs-code/design.md 协议（YAML tokens + Markdown rationale）

## Overview

校园设备智能运维系统，三种角色共用一套后台：师生重「报修提交」的亲和力，调度/管理员重「看板监控」的信息密度与告警可读性。单一主题无法同时讨好两类场景，因此采用**三主题可切换**架构：所有颜色/圆角走 CSS 变量，Element Plus 组件通过覆盖 `--el-*` 变量适配，ECharts 配色随主题联动。

## Themes

- **arco 清透企业风（默认）**：灰底白卡、小圆角、细边框，字节蓝 #165DFF。适合日常管理，耐看、信息密度高。
- **material Material 3 明亮风**：大圆角（12-16px）、胶囊标签、动态紫 #6750A4。适合对外演示与师生端，现代亲和。
- **dark 深色科技风**：GitHub Dark 配色，运维驾驶舱质感。适合 7×24 监控大屏与夜间值班，告警色对比度最高。

## Rules

1. 禁止硬编码色值：一切颜色引用 `var(--c-*)` 语义变量（primary/success/warning/danger/bg-page/bg-card/text-*/border）。
2. Element Plus 适配只在 `styles/themes.css` 中按 `[data-theme]` 覆盖 `--el-color-primary` 等变量，不逐组件写死。
3. ECharts 配色从当前主题 `chart` 数组取，切换主题时重设 option 并 resize。
4. 主题选择持久化到 localStorage（键 `szu_theme`），登录页与主布局共用。
5. 圆角三档：sm（标签/输入框）/ md（按钮/卡片）/ lg（面板），各主题取值不同，禁止第四种圆角。
6. 字重只用 400 / 500；正文字号不小于 12px。

## Do's and Don'ts

- Do: 新页面先引语义变量再写样式；切换三主题自查一遍对比度。
- Don't: 不写渐变色与投影堆叠（dark 主题允许极轻阴影分隔层级）；不引入第四种主题色；不在组件内写死 Element Plus 主色。
