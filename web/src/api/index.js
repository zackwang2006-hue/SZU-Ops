import request from './request'

// ================= 认证 =================
// POST /api/auth/login
// 入参: { username, password }
// 返回: { token, user: { id, username, realName, role } }
export const login = (data) => request.post('/auth/login', data)

// ================= 看板 =================
// GET /api/dashboard/overview
// 返回(假定): {
//   todayOrders, pendingOrders, slaRate, avgRepairHours,
//   trend: [{ date, count }],                 // 近30天工单趋势
//   buildingHeat: [{ building, count }],      // 楼栋故障热力
//   categoryDist: [{ category, count }],      // 品类分布
//   technicianPerf: [{ technicianId, technicianName, finishedCount, avgRepairHours, satisfaction }]
// }
export const getDashboardOverview = () => request.get('/dashboard/overview')

// ================= 设备资产 =================
// GET /api/assets/devices 分页返回(假定): { total, list: [device] }
// device 字段(camelCase): id, code, name, categoryId, categoryName,
//   building, room, purchaseDate, supplier, price,
//   status(IN_USE/REPAIRING/SCRAPPED), healthScore, createdAt, updatedAt
export const getDevices = (params) => request.get('/assets/devices', { params })
export const createDevice = (data) => request.post('/assets/devices', data)

// ================= 报修工单 =================
// GET /api/repair/orders 分页返回(假定): { total, list: [order] }
// order 字段(camelCase, 参考 repair_order 表):
//   id, orderNo, title, category, deviceId,
//   reporterId, reporterName, building, room, description, images,
//   priority(LOW/MEDIUM/HIGH/URGENT),
//   status(PENDING/ASSIGNED/PROCESSING/TO_VERIFY/COMPLETED/CANCELLED),
//   assigneeId, assigneeName, slaDeadline,
//   createdAt, assignedAt, finishedAt, verifiedAt,
//   timeline: [{ id, orderId, action, operatorId, operatorName, content, images, createdAt }]
//   evaluation: { score, comment }
export const getOrders = (params) => request.get('/repair/orders', { params })
export const createOrder = (data) => request.post('/repair/orders', data)
// 派单: { technicianId }
export const assignOrder = (id, data) => request.post(`/repair/orders/${id}/assign`, data)
export const startOrder = (id) => request.post(`/repair/orders/${id}/start`)
// 完工: { report }
export const finishOrder = (id, data) => request.post(`/repair/orders/${id}/finish`, data)
// 验收: { pass, score, comment }
export const verifyOrder = (id, data) => request.post(`/repair/orders/${id}/verify`, data)
export const urgeOrder = (id) => request.post(`/repair/orders/${id}/urge`)
export const getTimeoutOrders = (params) => request.get('/repair/orders/timeout', { params })

// ================= 预测预警 =================
// GET /api/prediction/risks 返回(假定): { total, list: [risk] } 或直接数组
// risk 字段: id, deviceId, deviceName, deviceCode, building, room,
//   riskLevel(LOW/MEDIUM/HIGH/CRITICAL), riskScore, reason,
//   predictedFailureDate, status(OPEN/DISMISSED/CONVERTED), createdAt
export const getRisks = (params) => request.get('/prediction/risks', { params })
export const convertRisk = (id) => request.post(`/prediction/risks/${id}/convert`)

// ================= 维修工列表（派单选择用，假定接口） =================
// 返回: [{ id, username, realName, role }]
export const getTechnicians = () => request.get('/repair/technicians')
