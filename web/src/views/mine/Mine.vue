<template>
  <div>
    <div class="page-card">
      <el-descriptions :column="4" border>
        <el-descriptions-item label="姓名">{{ store.realName }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ store.user?.username }}</el-descriptions-item>
        <el-descriptions-item label="角色">
          <el-tag size="small">{{ roleLabel }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ store.user?.phone || '-' }}</el-descriptions-item>
      </el-descriptions>
    </div>

    <div class="page-card">
      <div class="toolbar">
        <h3 class="section-title" style="margin: 0">我的工单</h3>
        <el-select v-model="query.status" placeholder="工单状态" clearable style="width: 150px" @change="load(1)">
          <el-option v-for="(v, k) in ORDER_STATUS" :key="k" :label="v.label" :value="k" />
        </el-select>
        <div class="spacer"></div>
        <el-button type="primary" :icon="Plus" @click="openCreate">新建报修</el-button>
      </div>

      <el-table :data="list" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="orderNo" label="工单号" width="160" show-overflow-tooltip />
        <el-table-column prop="title" label="标题" min-width="160" show-overflow-tooltip />
        <el-table-column label="位置" width="140">
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
        <el-table-column label="维修工" width="100">
          <template #default="{ row }">{{ row.assigneeName || '-' }}</template>
        </el-table-column>
        <el-table-column label="报修时间" width="150">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'TO_VERIFY'" type="warning" link @click="openVerify(row)">
              验收评价
            </el-button>
            <el-button
              v-if="['PENDING', 'ASSIGNED', 'PROCESSING'].includes(row.status)"
              type="danger"
              link
              @click="doUrge(row)"
            >催单</el-button>
            <el-rate
              v-if="row.status === 'COMPLETED' && row.evaluation"
              :model-value="row.evaluation.score"
              disabled
              :max="5"
            />
          </template>
        </el-table-column>
      </el-table>

      <div class="table-footer">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="load()"
        />
      </div>
    </div>

    <!-- 验收评价 -->
    <el-dialog v-model="verifyVisible" title="验收评价" width="480px" destroy-on-close>
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

    <!-- 新建报修 -->
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
import { Plus } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { getOrders, verifyOrder, urgeOrder, createOrder, getDevices } from '@/api'
import { useUserStore } from '@/store/user'
import { ORDER_STATUS, PRIORITY, dictLabel, dictType } from '@/utils/dict'

const store = useUserStore()

const ROLE_LABELS = {
  ADMIN: '管理员',
  DISPATCHER: '调度员',
  TECHNICIAN: '维修工',
  USER: '师生用户'
}
const roleLabel = computed(() => ROLE_LABELS[store.role] || store.role)

const loading = ref(false)
const acting = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, status: '', reporterId: store.user?.id })

const current = ref(null)
const verifyVisible = ref(false)
const verifyForm = reactive({ pass: true, score: 5, comment: '' })

const createVisible = ref(false)
const createRef = ref()
const deviceOptions = ref([])
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
  description: [{ required: true, message: '请填写故障描述', trigger: 'blur' }]
}

const formatTime = (t) => (t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '-')

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

const openVerify = (row) => {
  current.value = row
  Object.assign(verifyForm, { pass: true, score: 5, comment: '' })
  verifyVisible.value = true
}

const doVerify = async () => {
  acting.value = true
  try {
    await verifyOrder(current.value.id, { ...verifyForm })
    ElMessage.success('验收完成，感谢反馈')
    verifyVisible.value = false
    load()
  } finally {
    acting.value = false
  }
}

const doUrge = async (row) => {
  await ElMessageBox.confirm('确认催办该工单？', '催单', { type: 'warning' })
  await urgeOrder(row.id)
  ElMessage.success('已催单')
}

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
