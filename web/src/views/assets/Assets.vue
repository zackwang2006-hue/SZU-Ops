<template>
  <div class="page-card">
    <div class="toolbar">
      <el-select v-model="query.categoryId" placeholder="设备品类" clearable style="width: 160px">
        <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
      </el-select>
      <el-input v-model="query.building" placeholder="楼栋" clearable style="width: 140px" />
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 140px">
        <el-option v-for="(v, k) in DEVICE_STATUS" :key="k" :label="v.label" :value="k" />
      </el-select>
      <el-button type="primary" :icon="Search" @click="load(1)">查询</el-button>
      <el-button :icon="Refresh" @click="reset">重置</el-button>
      <div class="spacer"></div>
      <el-button type="primary" :icon="Plus" @click="openCreate">新建设备</el-button>
    </div>

    <el-table :data="list" v-loading="loading" stripe style="width: 100%">
      <el-table-column prop="code" label="设备编号" width="120" />
      <el-table-column prop="name" label="设备名称" min-width="150" show-overflow-tooltip />
      <el-table-column prop="categoryName" label="品类" width="110">
        <template #default="{ row }">{{ row.categoryName || row.category || '-' }}</template>
      </el-table-column>
      <el-table-column label="位置" min-width="140">
        <template #default="{ row }">{{ row.building }} {{ row.room }}</template>
      </el-table-column>
      <el-table-column prop="supplier" label="供应商" min-width="120" show-overflow-tooltip />
      <el-table-column prop="purchaseDate" label="采购日期" width="110" />
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="dictType(DEVICE_STATUS, row.status)">
            {{ dictLabel(DEVICE_STATUS, row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="健康度" width="150" align="center">
        <template #default="{ row }">
          <el-progress
            v-if="row.healthScore != null"
            :percentage="Math.round(row.healthScore)"
            :color="healthColor(row.healthScore)"
            :stroke-width="8"
          />
          <span v-else>-</span>
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

    <!-- 新建设备 -->
    <el-dialog v-model="dialogVisible" title="新建设备" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
        <el-form-item label="设备编号" prop="code">
          <el-input v-model="form.code" placeholder="如 AC-301-001" />
        </el-form-item>
        <el-form-item label="设备名称" prop="name">
          <el-input v-model="form.name" placeholder="如 格力中央空调" />
        </el-form-item>
        <el-form-item label="品类" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="请选择品类" style="width: 100%">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="楼栋" prop="building">
          <el-input v-model="form.building" placeholder="如 第一教学楼" />
        </el-form-item>
        <el-form-item label="房间" prop="room">
          <el-input v-model="form.room" placeholder="如 301" />
        </el-form-item>
        <el-form-item label="采购日期" prop="purchaseDate">
          <el-date-picker
            v-model="form.purchaseDate"
            type="date"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="供应商">
          <el-input v-model="form.supplier" />
        </el-form-item>
        <el-form-item label="价格（元）">
          <el-input-number v-model="form.price" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import { getDevices, createDevice } from '@/api'
import { DEVICE_STATUS, dictLabel, dictType } from '@/utils/dict'

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, categoryId: null, building: '', status: '' })

// 品类字典（假定后端字典接口缺失时的常用值，可通过 /api 补充）
const categories = ref([
  { id: 1, name: '空调' },
  { id: 2, name: '电梯' },
  { id: 3, name: '水电设备' },
  { id: 4, name: '网络设备' },
  { id: 5, name: '消防设备' }
])

const dialogVisible = ref(false)
const formRef = ref()
const form = reactive({
  code: '',
  name: '',
  categoryId: null,
  building: '',
  room: '',
  purchaseDate: '',
  supplier: '',
  price: null
})

const formRules = {
  code: [{ required: true, message: '请输入设备编号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入设备名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择品类', trigger: 'change' }],
  building: [{ required: true, message: '请输入楼栋', trigger: 'blur' }],
  room: [{ required: true, message: '请输入房间', trigger: 'blur' }]
}

const healthColor = (score) => {
  if (score >= 80) return 'var(--c-success)'
  if (score >= 60) return 'var(--c-warning)'
  return 'var(--c-danger)'
}

const load = async (page) => {
  if (page) query.page = page
  loading.value = true
  try {
    const data = await getDevices(query)
    list.value = data.list || data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

const reset = () => {
  query.categoryId = null
  query.building = ''
  query.status = ''
  load(1)
}

const openCreate = () => {
  Object.assign(form, {
    code: '',
    name: '',
    categoryId: null,
    building: '',
    room: '',
    purchaseDate: '',
    supplier: '',
    price: null
  })
  dialogVisible.value = true
}

const submit = async () => {
  await formRef.value.validate()
  saving.value = true
  try {
    await createDevice({ ...form })
    ElMessage.success('设备创建成功')
    dialogVisible.value = false
    load(1)
  } finally {
    saving.value = false
  }
}

onMounted(() => load())
</script>
