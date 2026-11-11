<template>
  <div class="page-card">
    <div class="toolbar">
      <el-select v-model="query.riskLevel" placeholder="风险等级" clearable style="width: 150px">
        <el-option v-for="(v, k) in RISK_LEVEL" :key="k" :label="v.label" :value="k" />
      </el-select>
      <el-select v-model="query.status" placeholder="处理状态" clearable style="width: 150px">
        <el-option v-for="(v, k) in RISK_STATUS" :key="k" :label="v.label" :value="k" />
      </el-select>
      <el-button type="primary" :icon="Search" @click="load(1)">查询</el-button>
      <el-button :icon="Refresh" @click="reset">重置</el-button>
    </div>

    <el-table :data="list" v-loading="loading" stripe style="width: 100%">
      <el-table-column label="设备" min-width="160">
        <template #default="{ row }">
          {{ row.deviceName || '-' }}
          <span style="color: var(--c-text-secondary); font-size: 12px">{{ row.deviceCode }}</span>
        </template>
      </el-table-column>
      <el-table-column label="位置" width="140">
        <template #default="{ row }">{{ row.building }} {{ row.room }}</template>
      </el-table-column>
      <el-table-column label="风险等级" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="dictType(RISK_LEVEL, row.riskLevel)" effect="dark" size="small">
            {{ dictLabel(RISK_LEVEL, row.riskLevel) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="风险分" width="120" align="center">
        <template #default="{ row }">
          <el-progress
            v-if="row.riskScore != null"
            :percentage="Math.round(row.riskScore * 100)"
            :color="riskColor(row.riskLevel)"
            :stroke-width="8"
          />
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="reason" label="风险原因" min-width="220" show-overflow-tooltip />
      <el-table-column label="预测故障日" width="120" align="center">
        <template #default="{ row }">
          <span :style="{ color: 'var(--c-danger)' }">{{ row.predictedFailureDate || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="处理状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="dictType(RISK_STATUS, row.status)" size="small">
            {{ dictLabel(RISK_STATUS, row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right" align="center">
        <template #default="{ row }">
          <el-button
            v-if="row.status === 'OPEN'"
            type="primary"
            size="small"
            @click="doConvert(row)"
          >转巡检工单</el-button>
          <span v-else style="color: var(--c-text-secondary)">-</span>
        </template>
      </el-table-column>
    </el-table>

    <div class="table-footer">
      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="load()"
        @size-change="load(1)"
      />
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import { getRisks, convertRisk } from '@/api'
import { RISK_LEVEL, RISK_STATUS, dictLabel, dictType } from '@/utils/dict'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, riskLevel: '', status: '' })

const riskColor = (level) =>
  ({
    CRITICAL: 'var(--c-danger)',
    HIGH: 'var(--c-warning)',
    MEDIUM: 'var(--c-primary)',
    LOW: 'var(--c-text-secondary)'
  }[level] || 'var(--c-text-secondary)')

const load = async (page) => {
  if (page) query.page = page
  loading.value = true
  try {
    const data = await getRisks(query)
    if (Array.isArray(data)) {
      list.value = data
      total.value = data.length
    } else {
      list.value = data.list || data.records || []
      total.value = data.total || 0
    }
  } finally {
    loading.value = false
  }
}

const reset = () => {
  query.riskLevel = ''
  query.status = ''
  load(1)
}

const doConvert = async (row) => {
  await ElMessageBox.confirm(
    `确认为设备「${row.deviceName || row.deviceCode}」生成巡检工单？`,
    '转巡检工单',
    { type: 'warning' }
  )
  await convertRisk(row.id)
  ElMessage.success('已转为巡检工单')
  load()
}

onMounted(() => load())
</script>
