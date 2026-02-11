# GeoFile - 在线文件传输平台

## 项目概述
基于地理位置的免登录在线文件传输系统，支持断点续传、在线解压、在线预览等功能。

## 技术栈

### 前端
- Vue 3 + TypeScript
- ElementPlus UI组件库
- Axios + JWT

### 后端
- Spring Boot 3.x
- Spring Security + JWT
- MyBatis-Plus
- Redis缓存
- MySQL 8.0
- Knife4j文档
- 阿里云OSS

### 中间件
- Nginx
- Tomcat
- Git版本控制

## 核心功能
1. ✅ 地理位置搜索附近的文件传输需求
2. ✅ 免登录验证码验证
3. ✅ 下载次数限制
4. ✅ 有效时长限制
5. ✅ 断点续传
6. ✅ 在线解压压缩包
7. ✅ 在线预览（文本、视频）

## 数据库表结构

### 1. t_file - 文件信息表
```sql
CREATE TABLE t_file (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  file_name VARCHAR(255) NOT NULL COMMENT '文件名',
  file_type VARCHAR(50) COMMENT '文件类型',
  file_size BIGINT COMMENT '文件大小(字节)',
  file_path VARCHAR(500) COMMENT '文件存储路径',
  original_name VARCHAR(255) COMMENT '原始文件名',
  storage_type VARCHAR(20) DEFAULT 'OSS' COMMENT '存储类型',
  upload_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  expire_time DATETIME COMMENT '有效截止时间',
  download_count INT DEFAULT 0 COMMENT '下载次数',
  status TINYINT DEFAULT 1 COMMENT '状态 0-删除 1-正常',
  location_lat DOUBLE COMMENT '地理位置纬度',
  location_lng DOUBLE COMMENT '地理位置经度',
  location_radius INT DEFAULT 1000 COMMENT '地理位置半径(米)',
  region_id VARCHAR(50) COMMENT '区域ID',
  created_by BIGINT COMMENT '创建人',
  created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT COMMENT '更新人',
  updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
  UNIQUE KEY uk_file_name (file_name, deleted),
  KEY idx_upload_time (upload_time),
  KEY idx_expire_time (expire_time),
  KEY idx_location (location_lat, location_lng, location_radius)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件信息表';
```

### 2. t_user_location - 用户地理位置表
```sql
CREATE TABLE t_user_location (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  user_id BIGINT COMMENT '用户ID(匿名用户用UUID)',
  lat DOUBLE NOT NULL COMMENT '纬度',
  lng DOUBLE NOT NULL COMMENT '经度',
  region VARCHAR(100) COMMENT '区域信息',
  city VARCHAR(50) COMMENT '城市',
  province VARCHAR(50) COMMENT '省份',
  location_type VARCHAR(20) DEFAULT 'AUTO' COMMENT '定位方式',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_id (user_id),
  KEY idx_region (province, city)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户地理位置表';
```

### 3. t_file_transfer - 文件传输记录表
```sql
CREATE TABLE t_file_transfer (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  file_id BIGINT NOT NULL COMMENT '文件ID',
  transfer_code VARCHAR(50) NOT NULL COMMENT '传输验证码',
  ip_address VARCHAR(50) COMMENT '下载IP',
  user_agent VARCHAR(500) COMMENT '用户代理',
  download_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '下载时间',
  download_duration INT COMMENT '下载持续时间(秒)',
  is_completed TINYINT DEFAULT 0 COMMENT '是否完成下载',
  UNIQUE KEY uk_transfer_code (transfer_code),
  KEY idx_file_id (file_id),
  KEY idx_download_time (download_time),
  FOREIGN KEY (file_id) REFERENCES t_file(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件传输记录表';
```

### 4. t_verification_code - 验证码表
```sql
CREATE TABLE t_verification_code (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  code VARCHAR(10) NOT NULL COMMENT '验证码',
  type VARCHAR(20) DEFAULT 'DOWNLOAD' COMMENT '验证码类型',
  expire_time DATETIME NOT NULL COMMENT '过期时间',
  max_attempts INT DEFAULT 3 COMMENT '最大尝试次数',
  attempt_count INT DEFAULT 0 COMMENT '当前尝试次数',
  is_used TINYINT DEFAULT 0 COMMENT '是否已使用',
  created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_code (code),
  KEY idx_expire_time (expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='验证码表';
```

### 5. t_download_limit - 下载限制配置表
```sql
CREATE TABLE t_download_limit (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  file_id BIGINT NOT NULL COMMENT '文件ID',
  max_downloads INT DEFAULT 5 COMMENT '最大下载次数',
  valid_hours INT DEFAULT 24 COMMENT '有效时长(小时)',
  UNIQUE KEY uk_file_id (file_id),
  FOREIGN KEY (file_id) REFERENCES t_file(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='下载限制配置表';
```

## 开发步骤

### Phase 1: 项目初始化（第1天）
1. 创建后端Spring Boot项目
2. 配置MyBatis-Plus代码生成器
3. 创建前端Vue项目
4. 配置依赖和基础架构

### Phase 2: 数据库设计（第1-2天）
1. 创建数据库和表结构
2. 初始化数据
3. 配置MyBatis-Plus实体类

### Phase 3: 后端开发（第2-15天）
1. **基础配置**：JWT、Redis、OSS配置
2. **文件上传**：本地存储 + 阿里云OSS
3. **地理位置服务**：IP定位 + 地理编码
4. **验证码服务**：图形验证码 + 短信验证码
5. **文件管理**：CRUD接口
6. **文件下载**：断点续传、次数限制、时间限制
7. **压缩解压**：在线解压、批量下载
8. **在线预览**：文本、图片、视频预览
9. **API文档**：Knife4j配置

### Phase 4: 前端开发（第10-20天）
1. 项目初始化 + ElementPlus配置
2. 登录/验证码页面
3. 文件上传页面
4. 地理位置定位功能
5. 文件列表展示
6. 文件详情和下载
7. 在线预览组件
8. 压缩包在线解压
9. 个人中心

### Phase 5: 部署上线（第20-25天）
1. Nginx配置
2. Docker部署
3. 域名配置
4. SSL证书配置

## 项目亮点（面试加分项）

1. **地理位置服务**：自动定位用户位置，搜索附近文件
2. **断点续传**：大文件传输，支持断点续传和秒传
3. **安全机制**：验证码 + 下载限制 + 逻辑删除
4. **存储优化**：阿里云OSS + 分片上传
5. **在线处理**：压缩解压、视频转码预览
6. **高性能**：Redis缓存 + 数据库索引优化

## 建议的学习路径

1. 先完成基础功能开发（文件上传下载）
2. 再添加高级功能（断点续传、在线预览）
3. 最后优化和部署

## 开发环境要求

- Java 17+
- Node.js 18+
- MySQL 8.0
- Redis 7.0+
- Maven 3.8+
