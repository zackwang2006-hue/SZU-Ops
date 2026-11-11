<template>
  <div class="page-card">
    <div class="toolbar">
      <el-select v-model="query.status" placeholder="工单状态" clearable style="width: 150px">
        <el-option v-for="(v, k) in ORDER_STATUS" :key="k" :label="v.label" :value="k" />
      </el-select>
      <el-select v-model="query.priority" placeholder="优先级" clearable style="width: 130px">
        <el-option v-for="(v, k) in PRIORITY" :key="k" :label="v.label" :value="k" />
      </el-select>
      <el-button type="primary" :icon="Search" @click="load(1)">查询</el-button>
      <el-button :icon="Refresh" @click="reset">重置</el-button>
      <div class="spacer"></div>
      <el-button type="primary" :icon="Plus" @click="openCreate">新建报修</el-button>
    </div>

    <el-table :data="list" v-loading="loading" row-key="id" :expand-row-keys="expandedKeys" @expand-change="onExpand" stripe style="width: 100%">
      <el-table-column type="expand">
        <template #default="{ row }">
          <div class="timeline-wrap">
            <div class="desc">
              <b>故障描述：</b>{{ row.description || '无' }}
              <template v-if="row.evaluation">
                <br />
                <b>验收评价：</b>
                <el-rate :model-value="row.evaluation.score" disabled :max="5" style="display: inline-flex; vertical-align: middle" />
                {{ row.evaluation.comment }}
              </template>
            </div>
            <el-timeline v-if="row.timeline && row.timeline.length">
              <el-timeline-item
                v-for="t in row.timeline"
                :key="t.id"
                :timestamp="formatTime(t.createdAt)"
                placement="top"
              >
                <b>{{ t.operatorName || '系统' }}</b> {{ t.content || t.action }}
              </el-timeline-item>
            </el-timeline>
            <el-empty v-else description="暂无流转记录" :image-size="60" />
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="orderNo" label="工单号" width="160" show-overflow-tooltip />
      <el-table-column prop="title" label="标题" min-width="150" show-overflow-tooltip />
      <el-table-column label="位置" width="130">
        <template #default="{ row }">{{ row.building }} {{ row.room }}</template>
      </el-table-column>
      <el-table-column label="优先级" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="dictType(PRIORITY, row.priority)" size="small">
            {{ dictLabel(PRIORITY, row.priority) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="dictType(ORDER_STATUS, row.status)" size="small">
            {{ dictLabel(ORDER_STATUS, row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="维修工" width="90">
        <template #default="{ row }">{{ row.assigneeName || '-' }}</template>
      </el-table-column>
      <el-table-column label="SLA 截止" width="150">
        <template #default="{ row }">
          <span :class="{ overdue: isOverdue(row) }">{{ formatTime(row.slaDeadline) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="报修时间" width="150">
        <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="canAssign(row)"
            type="primary"
            link
            @click="openAssign(row)"
          >派单</el-button>
          <el-button
            v-if="canStart(row)"
            type="success"
            link
            @click="doStart(row)"
          >开工</el-button>
          <el-button
            v-if="canFinish(row)"
            type="success"
            link
            @click="openFinish(row)"
          >完工</el-button>
          <el-button
            v-if="canVerify(row)"
            type="warning"
            link
            @click="openVerify(row)"
          >验收</el-button>
          <el-button
            v-if="canUrge(row)"
            type="danger"
            link
            @click="doUrge(row)"
          >催单</el-button>
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

    <!-- 派单对话框 -->
    <el-dialog v-model="assignVisible" title="派单" width="420px" destroy-on-close>
      <el-form label-width="90px">
        <el-form-item label="工单">
          <span>{{ current?.orderNo }} - {{ current?.title }}</span>
        </el-form-item>
        <el-form-item label="维修工" required>
          <el-select v-model="assignForm.technicianId" placeholder="请选择维修工" style="width: 100%">
            <el-option
              v-for="t in technicians"
              :key="t.id"
              :label="`${t.realName || t.username}`"
              :value="t.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" :loading="acting" @click="doAssign">确定</el-button>
      </template>
    </el-dialog>

    <!-- 完工对话框 -->
    <el-dialog v-model="finishVisible" title="完工登记" width="480px" destroy-on-close>
      <el-form label-width="90px">
        <el-form-item label="维修说明" required>
          <el-input
            v-model="finishForm.report"
            type="textarea"
            :rows="4"
            placeholder="请填写维修过程与结果"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="finishVisible = false">取消</el-button>
        <el-button type="primary" :loading="acting" @click="doFinish">提交</el-button>
      </template>
    </el-dialog>

    <!-- 验收对话框 -->
    <el-dialog v-model="verifyVisible" title="工单验收" width="480px" destroy-on-close>
      <el-form label-width="90px">
        <el-form-item label="验收结果">
          <el-radio-group v-model="verifyForm.pass">
            <el-radio :value="true">验收通过</el-radio>
            <el-radio :value="false">验收不通过</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="满意度评分" v-if="verifyForm.pass">
          <el-rate v-model="verifyForm.score" :max="5" />
        </el-form-item>
        <el-form-item label="评语">
          <el-input v-model="verifyForm.comment" type="textarea" :rows="3" placeholder="请填写评语" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="verifyVisible = false">取消</el-button>
        <el-button type="primary" :loading="acting" @click="doVerify">提交</el-button>
      </template>
    </el-dialog>

    <!-- 新建报修对话框 -->
    <el-dialog v-model="createVisible" title="新建报修" width="560px" destroy-on-close>
      <el-form ref="createRef" :model="createForm" :rules="createRules" label-width="90px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="createForm.title" placeholder="如 空调不制冷" />
        </el-form-item>
        <el-form-item label="关联设备" prop="deviceId">
          <el-select
            v-model="createForm.deviceId"
            placeholder="请选择设备"
            filterable
            style="width: 100%"
            @change="onDeviceChange"
          >
            <el-option
              v-for="d in deviceOptions"
              :key="d.id"
              :label="`${d.name}（${d.building || ''} ${d.room || ''}）`"
              :value="d.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="位置">
          <el-input :model-value="`${createForm.building || ''} ${createForm.room || ''}`" disabled placeholder="选择设备后自动带出" />
        </el-form-item>
        <el-form-item label="故障类型" prop="category">
          <el-select v-model="createForm.category" placeholder="请选择故障类型" style="width: 100%">
            <el-option label="水电故障" value="水电" />
            <el-option label="空调暖通" value="空调" />
            <el-option label="电梯故障" value="电梯" />
            <el-option label="网络故障" value="网络" />
            <el-option label="门窗家具" value="家具" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级" prop="priority">
          <el-radio-group v-model="createForm.priority">
            <el-radio-button v-for="(v, k) in PRIORITY" :key="k" :value="k">{{ v.label }}</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="故障描述" prop="description">
          <el-input v-model="createForm.description" type="textarea" :rows="4" placeholder="请描述故障现象" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="acting" @click="doCreate">提交报修</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import {
  getOrders,
  assignOrder,
  startOrder,
  finishOrder,
  verifyOrder,
  urgeOrder,
  createOrder,
  getDevices,
  getTechnicians
} from '@/api'
import { useUserStore } from '@/store/user'
import { ORDER_STATUS, PRIORITY, dictLabel, dictType } from '@/utils/dict'

const store = useUserStore()

const loading = ref(false)
const acting = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, status: '', priority: '' })
const expandedKeys = ref([])

const current = ref(null)
const technicians = ref([])
const deviceOptions = ref([])

const assignVisible = ref(false)
const finishVisible = ref(false)
const verifyVisible = ref(false)
const createVisible = ref(false)

const assignForm = reactive({ technicianId: null })
const finishForm = reactive({ report: '' })
const verifyForm = reactive({ pass: true, score: 5, comment: '' })
const createRef = ref()
const createForm = reactive({
  title: '',
  deviceId: null,
  building: '',
  room: '',
  category: '',
  priority: 'MEDIUM',
  description: ''
})

const createRules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  deviceId: [{ required: true, message: '请选择设备', trigger: 'change' }],
  category: [{ required: true, message: '请选择故障类型', trigger: 'change' }],
  priority: [{ required: true, message: '请选择优先级', trigger: 'change' }],
  description: [{ required: true, message: '请填写故障描述', trigger: 'blur' }]
}

const formatTime = (t) => (t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '-')
const isOverdue = (row) =>
  row.slaDeadline &&
  !['COMPLETED', 'CANCELLED'].includes(row.status) &&
  dayjs(row.slaDeadline).isBefore(dayjs())

// ===== 操作按钮可见性（按角色 + 状态） =====
const isManager = computed(() => ['ADMIN', 'DISPATCHER'].includes(store.role))
const isMine = (row) => row.reporterId === store.user?.id
const isAssignee = (row) => row.assigneeId === store.user?.id

const canAssign = (row) => isManager.value && row.status === 'PENDING'
const canStart = (row) =>
  (store.role === 'TECHNICIAN' && isAssignee(row) && row.status === 'ASSIGNED') ||
  (isManager.value && row.status === 'ASSIGNED')
const canFinish = (row) =>
  (store.role === 'TECHNICIAN' && isAssignee(row) && row.status === 'PROCESSING') ||
  (isManager.value && row.status === 'PROCESSING')
const canVerify = (row) => row.status === 'TO_VERIFY' && (isMine(row) || isManager.value)
const canUrge = (row) =>
  ['PENDING', 'ASSIGNED', 'PROCESSING'].includes(row.status) && (isMine(row) || isManager.value)

const load = async (page) => {
  if (page) query.page = page
  loading.value = true
  try {
    const data = await getOrders(query)
    list.value = data.list || data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

const reset = () => {
  query.status = ''
  query.priority = ''
  load(1)
}

const onExpand = (row, expanded) => {
  expandedKeys.value = expanded.map((r) => (typeof r === 'object' ? r.id : r))
}

// ===== 派单 =====
const openAssign = async (row) => {
  current.value = row
  assignForm.technicianId = null
  if (!technicians.value.length) {
    try {
      technicians.value = await getTechnicians()
    } catch {
      technicians.value = []
    }
  }
  assignVisible.value = true
}

const doAssign = async () => {
  if (!assignForm.technicianId) {
    ElMessage.warning('请选择维修工')
    return
  }
  acting.value = true
  try {
    await assignOrder(current.value.id, { technicianId: assignForm.technicianId })
    ElMessage.success('派单成功')
    assignVisible.value = false
    load()
  } finally {
    acting.value = false
  }
}

// ===== 开工 =====
const doStart = async (row) => {
  await ElMessageBox.confirm('确认开工处理该工单？', '开工确认', { type: 'info' })
  await startOrder(row.id)
  ElMessage.success('已开工')
  load()
}

// ===== 完工 =====
const openFinish = (row) => {
  current.value = row
  finishForm.report = ''
  finishVisible.value = true
}

const doFinish = async () => {
  if (!finishForm.report.trim()) {
    ElMessage.warning('请填写维修说明')
    return
  }
  acting.value = true
  try {
    await finishOrder(current.value.id, { report: finishForm.report })
    ElMessage.success('完工提交成功')
    finishVisible.value = false
    load()
  } finally {
    acting.value = false
  }
}

// ===== 验收 =====
const openVerify = (row) => {
  current.value = row
  Object.assign(verifyForm, { pass: true, score: 5, comment: '' })
  verifyVisible.value = true
}

const doVerify = async () => {
  acting.value = true
  try {
    await verifyOrder(current.value.id, { ...verifyForm })
    ElMessage.success('验收完成')
    verifyVisible.value = false
    load()
  } finally {
    acting.value = false
  }
}

// ===== 催单 =====
const doUrge = async (row) => {
  await ElMessageBox.confirm('确认催办该工单？', '催单', { type: 'warning' })
  await urgeOrder(row.id)
  ElMessage.success('已催单')
}

// ===== 新建报修 =====
const openCreate = async () => {
  Object.assign(createForm, {
    title: '',
    deviceId: null,
    building: '',
    room: '',
    category: '',
    priority: 'MEDIUM',
    description: ''
  })
  if (!deviceOptions.value.length) {
    try {
      const data = await getDevices({ page: 1, size: 200, status: 'IN_USE' })
      deviceOptions.value = data.list || data.records || []
    } catch {
      deviceOptions.value = []
    }
  }
  createVisible.value = true
}

const onDeviceChange = (id) => {
  const d = deviceOptions.value.find((i) => i.id === id)
  if (d) {
    createForm.building = d.building || ''
    createForm.room = d.room || ''
  }
}

const doCreate = async () => {
  await createRef.value.validate()
  acting.value = true
  try {
    await createOrder({ ...createForm })
    ElMessage.success('报修提交成功')
    createVisible.value = false
    load(1)
  } finally {
    acting.value = false
  }
}

onMounted(() => load())
</script>

<style scoped>
.timeline-wrap {
  padding: 8px 24px;
}

.desc {
  margin-bottom: 12px;
  color: var(--c-text-regular);
  line-height: 1.8;
}

.overdue {
  color: var(--c-danger);
  font-weight: 500;
}
</style>
