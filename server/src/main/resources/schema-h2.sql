-- H2 内存库建表脚本（与 docs/ARCHITECTURE.md 第 5 节一致，H2 运行于 MODE=MySQL）
CREATE TABLE IF NOT EXISTS sys_user (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(64) NOT NULL UNIQUE,
  password VARCHAR(100) NOT NULL,
  real_name VARCHAR(64),
  phone VARCHAR(32),
  role VARCHAR(32) NOT NULL,           -- ADMIN/DISPATCHER/TECHNICIAN/USER
  status INT DEFAULT 1,                -- 1启用 0停用
  created_at DATETIME
);

CREATE TABLE IF NOT EXISTS asset_category (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(64) NOT NULL,
  code VARCHAR(64) NOT NULL UNIQUE,
  expected_lifespan_years INT,         -- 预期寿命（年）
  sla_hours INT NOT NULL DEFAULT 48    -- 该品类工单 SLA 时限（小时）
);

CREATE TABLE IF NOT EXISTS asset_device (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(64) NOT NULL UNIQUE,
  name VARCHAR(128) NOT NULL,
  category_id BIGINT,
  building VARCHAR(64),
  room VARCHAR(64),
  purchase_date DATE,
  supplier VARCHAR(128),
  price DECIMAL(12,2),
  status VARCHAR(32) DEFAULT 'IN_USE', -- IN_USE/REPAIRING/SCRAPPED
  health_score INT DEFAULT 100,        -- 由预测模块回写
  created_at DATETIME,
  updated_at DATETIME
);

CREATE TABLE IF NOT EXISTS repair_order (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_no VARCHAR(64),
  title VARCHAR(200) NOT NULL,
  category VARCHAR(64),                -- 故障类型；巡检工单固定为 INSPECTION
  device_id BIGINT,
  reporter_id BIGINT,
  building VARCHAR(64),
  room VARCHAR(64),
  description CLOB,
  images CLOB,
  priority VARCHAR(16) DEFAULT 'MEDIUM', -- LOW/MEDIUM/HIGH/URGENT
  status VARCHAR(32) DEFAULT 'PENDING',  -- PENDING/ASSIGNED/PROCESSING/TO_VERIFY/COMPLETED/CANCELLED
  assignee_id BIGINT,
  sla_deadline DATETIME,
  created_at DATETIME,
  assigned_at DATETIME,
  finished_at DATETIME,
  verified_at DATETIME
);

CREATE TABLE IF NOT EXISTS repair_timeline (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  action VARCHAR(32),                  -- CREATE/ASSIGN/START/FINISH/VERIFY/URGE/CANCEL 等
  operator_id BIGINT,
  content VARCHAR(500),
  images CLOB,
  created_at DATETIME
);

CREATE TABLE IF NOT EXISTS repair_evaluation (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  score INT,                           -- 1-5 满意度
  comment VARCHAR(500),
  created_at DATETIME
);

CREATE TABLE IF NOT EXISTS prediction_risk (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  device_id BIGINT NOT NULL,
  risk_level VARCHAR(16),              -- LOW/MEDIUM/HIGH/CRITICAL
  risk_score DOUBLE,
  reason VARCHAR(500),
  predicted_failure_date DATE,
  status VARCHAR(16) DEFAULT 'OPEN',   -- OPEN/DISMISSED/CONVERTED
  created_at DATETIME
);
