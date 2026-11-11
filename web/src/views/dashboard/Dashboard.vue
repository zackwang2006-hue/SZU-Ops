<template>
  <div v-loading="loading">
    <!-- 统计卡片 -->
    <el-row :gutter="16">
      <el-col v-for="card in cards" :key="card.label" :span="6">
        <div class="stat-card page-card">
          <div class="icon" :style="{ background: card.color }">
            <el-icon :size="24" color="white"><component :is="card.icon" /></el-icon>
          </div>
          <div class="info">
            <div class="value">{{ card.value }}</div>
            <div class="label">{{ card.label }}</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="16">
        <div class="page-card">
          <h3 class="section-title">近30天工单趋势</h3>
          <div ref="trendRef" class="chart" style="height: 300px"></div>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="page-card">
          <h3 class="section-title">故障品类分布</h3>
          <div ref="pieRef" class="chart" style="height: 300px"></div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="12">
        <div class="page-card">
          <h3 class="section-title">楼栋故障热力</h3>
          <div ref="barRef" class="chart" style="height: 320px"></div>
        </div>
      </el-col>
      <el-col :span="12">
        <div class="page-card">
          <h3 class="section-title">维修人员绩效</h3>
          <el-table :data="perfList" size="default" style="width: 100%">
            <el-table-column prop="technicianName" label="维修工" min-width="100" />
            <el-table-column prop="finishedCount" label="完成工单" width="90" align="center" />
            <el-table-column label="平均修复时长" width="110" align="center">
              <template #default="{ row }">{{ formatHours(row.avgRepairHours) }}</template>
            </el-table-column>
            <el-table-column label="满意度" width="160" align="center">
              <template #default="{ row }">
                <el-rate :model-value="row.satisfaction || 0" disabled :max="5" />
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!perfList.length" description="暂无绩效数据" :image-size="80" />
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getDashboardOverview } from '@/api'
import { useThemeStore } from '@/store/theme'
import { getChartPalette } from '@/styles/chart-theme'

const themeStore = useThemeStore()

const loading = ref(false)
const overview = ref({})
const trendRef = ref()
const pieRef = ref()
const barRef = ref()
const chartMap = new Map()

const cards = computed(() => [
  { label: '今日工单', value: overview.value.todayOrders ?? '-', icon: 'Tickets', color: 'var(--c-primary)' },
  { label: '待处理工单', value: overview.value.pendingOrders ?? '-', icon: 'Clock', color: 'var(--c-warning)' },
  {
    label: 'SLA 达标率',
    value: overview.value.slaRate != null ? `${(overview.value.slaRate * 100).toFixed(1)}%` : '-',
    icon: 'CircleCheck',
    color: 'var(--c-success)'
  },
  {
    label: '平均修复时长',
    value: formatHours(overview.value.avgRepairHours),
    icon: 'Timer',
    color: 'var(--c-danger)'
  }
])

const perfList = computed(() => overview.value.technicianPerf || [])

const formatHours = (h) => (h == null ? '-' : `${Number(h).toFixed(1)} 小时`)

// ===== 图表 option 构造（随主题取色） =====
const axisStyle = (p) => ({
  axisLabel: { color: p.text },
  axisLine: { lineStyle: { color: p.axisLine } }
})

const baseTooltip = (p, extra = {}) => ({
  backgroundColor: p.tooltipBg,
  textStyle: { color: p.tooltipText },
  borderColor: p.axisLine,
  ...extra
})

const trendOption = (data, p) => {
  const trend = data.trend || []
  return {
    color: p.color,
    tooltip: baseTooltip(p, { trigger: 'axis' }),
    grid: { left: 40, right: 20, top: 30, bottom: 40 },
    xAxis: { type: 'category', data: trend.map((i) => i.date), axisLabel: { rotate: 45, color: p.text }, axisLine: { lineStyle: { color: p.axisLine } } },
    yAxis: { type: 'value', minInterval: 1, ...axisStyle(p), splitLine: { lineStyle: { color: p.splitLine } } },
    series: [
      {
        name: '工单量',
        type: 'line',
        smooth: true,
        areaStyle: { opacity: 0.15 },
        itemStyle: { color: p.color[0] },
        data: trend.map((i) => i.count)
      }
    ]
  }
}

const barOption = (data, p) => {
  const heat = data.buildingHeat || []
  return {
    color: p.color,
    tooltip: baseTooltip(p, { trigger: 'axis' }),
    grid: { left: 60, right: 20, top: 30, bottom: 40 },
    xAxis: { type: 'category', data: heat.map((i) => i.building), axisLabel: { rotate: 30, color: p.text }, axisLine: { lineStyle: { color: p.axisLine } } },
    yAxis: { type: 'value', minInterval: 1, ...axisStyle(p), splitLine: { lineStyle: { color: p.splitLine } } },
    visualMap: {
      min: 0,
      max: Math.max(...heat.map((i) => i.count), 1),
      orient: 'horizontal',
      left: 'center',
      bottom: 0,
      inRange: { color: [p.color[1], p.color[2], p.color[3]] },
      show: false
    },
    series: [{ name: '故障数', type: 'bar', barWidth: 24, data: heat.map((i) => i.count) }]
  }
}

const pieOption = (data, p) => {
  const dist = data.categoryDist || []
  return {
    color: p.color,
    tooltip: baseTooltip(p, { trigger: 'item', formatter: '{b}：{c}（{d}%）' }),
    legend: { bottom: 0, textStyle: { color: p.text } },
    series: [
      {
        name: '品类分布',
        type: 'pie',
        radius: ['40%', '65%'],
        center: ['50%', '45%'],
        label: { formatter: '{b}\n{c}', color: p.text },
        data: dist.map((i) => ({ name: i.category, value: i.count }))
      }
    ]
  }
}

const buildCharts = () => {
  if (chartMap.size === 0) {
    chartMap.set('trend', echarts.init(trendRef.value))
    chartMap.set('bar', echarts.init(barRef.value))
    chartMap.set('pie', echarts.init(pieRef.value))
  }
}

const renderCharts = () => {
  buildCharts()
  const p = getChartPalette(themeStore.theme)
  chartMap.get('trend').setOption(trendOption(overview.value, p), true)
  chartMap.get('bar').setOption(barOption(overview.value, p), true)
  chartMap.get('pie').setOption(pieOption(overview.value, p), true)
}

const onResize = () => chartMap.forEach((c) => c.resize())

// 主题切换：重设 option 并 resize，联动换肤
watch(
  () => themeStore.theme,
  async () => {
    await nextTick()
    if (chartMap.size) {
      renderCharts()
      onResize()
    }
  }
)

onMounted(async () => {
  loading.value = true
  try {
    overview.value = await getDashboardOverview()
    await nextTick()
    renderCharts()
    window.addEventListener('resize', onResize)
  } finally {
    loading.value = false
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  chartMap.forEach((c) => c.dispose())
})
</script>

<style scoped>
.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-card .icon {
  width: 52px;
  height: 52px;
  border-radius: var(--c-radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-card .value {
  font-size: 24px;
  font-weight: 500;
  color: var(--c-text-primary);
  line-height: 1.2;
}

.stat-card .label {
  font-size: 12px;
  color: var(--c-text-secondary);
  margin-top: 4px;
}
</style>
