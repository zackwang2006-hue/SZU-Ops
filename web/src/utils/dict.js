// 工单状态
export const ORDER_STATUS = {
  PENDING: { label: '待派单', type: 'warning' },
  ASSIGNED: { label: '已派单', type: 'primary' },
  PROCESSING: { label: '处理中', type: 'primary' },
  TO_VERIFY: { label: '待验收', type: 'success' },
  COMPLETED: { label: '已完成', type: 'info' },
  CANCELLED: { label: '已取消', type: 'info' }
}

// 优先级
export const PRIORITY = {
  LOW: { label: '低', type: 'info' },
  MEDIUM: { label: '中', type: 'primary' },
  HIGH: { label: '高', type: 'warning' },
  URGENT: { label: '紧急', type: 'danger' }
}

// 风险等级
export const RISK_LEVEL = {
  CRITICAL: { label: '严重', type: 'danger' },
  HIGH: { label: '高', type: 'warning' },
  MEDIUM: { label: '中', type: 'primary' },
  LOW: { label: '低', type: 'info' }
}

// 设备状态
export const DEVICE_STATUS = {
  IN_USE: { label: '在用', type: 'success' },
  REPAIRING: { label: '维修中', type: 'warning' },
  SCRAPPED: { label: '已报废', type: 'info' }
}

// 风险处理状态
export const RISK_STATUS = {
  OPEN: { label: '待处理', type: 'warning' },
  DISMISSED: { label: '已忽略', type: 'info' },
  CONVERTED: { label: '已转巡检', type: 'success' }
}

export const dictLabel = (dict, value) => dict[value]?.label || value || '-'
export const dictType = (dict, value) => dict[value]?.type || 'info'
