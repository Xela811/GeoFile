# 🗺️ GeoFile

<div align="center">

![Vue 3](https://img.shields.io/badge/Vue-3.x-4FC08D?style=flat-square&logo=vue.js)
![Vite](https://img.shields.io/badge/Vite-7.x-646CFF?style=flat-square&logo=vite)
![TypeScript](https://img.shields.io/badge/TypeScript-5.x-3178C6?style=flat-square&logo=typescript)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-6DB33F?style=flat-square&logo=spring-boot)
![OpenJDK](https://img.shields.io/badge/OpenJDK-21-DD0031?style=flat-square&logo=openjdk)
![MyBatis-Plus](https://img.shields.io/badge/MyBatis--Plus-3.5.x-8A0D9F?style=flat-square)
![Redis](https://img.shields.io/badge/Redis-7.x-DC382D?style=flat-square&logo=redis)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=flat-square&logo=mysql)

**基于地理位置的免登录全栈文件边缘分享利器**

[在线文档](https://your-domain.com) · [功能演示](https://your-domain.com) · [Issue 反馈](https://github.com/your-repo/issues)

</div>

---

## 🌟 项目简介

GeoFile 是一款**基于空间地理位置的、免登录、时效性全栈文件边缘分享工具**。它巧妙地结合了地理围栏技术与分布式存储策略，为用户提供了一种安全、高效、无需注册的文件分享方案。

### 核心定位

- 🌐 **地理围栏分发**：公开文件仅在 1km 范围内可见
- 🔒 **零登录设计**：通过 localStorage 特异性凭证区分分享者与访客
- ⚡ **秒传黑科技**：SHA-256 哈希指纹快速识别重复文件
- 🎯 **灵活分享矩阵**：支持单文件、批量、私有/公开多种分享模式
- 🧹 **自动时效清理**：Redis TTL + 定时任务双重保障资源自动销毁

---

## 🧱 项目核心目录结构树

```
GeoFile/
├── 📁 backend/                          # 后端服务 (Spring Boot 3 + OpenJDK 21)
│   └── src/main/java/com/geofile/
│       ├── controller/                  # REST API 控制器层
│       │   ├── FileUploadController.java          # 文件上传/下载/预览核心
│       │   ├── FileNearbyController.java          # 地理位置搜索接口
│       │   ├── LocationController.java            # 位置记录接口
│       │   ├── AmapController.java                # 高德地图逆地理编码
│       │   └── VerificationCodeController.java     # 验证码生成
│       │
│       ├── service/                     # 业务逻辑层
│       │   ├── impl/
│       │   │   ├── FileUploadServiceImpl.java     # 文件上传服务实现（秒传核心）
│       │   │   ├── FileServiceImpl.java           # 文件业务服务
│       │   │   ├── FileHashServiceImpl.java       # 哈希去重服务
│       │   │   ├── FileLifecycleTask.java         # 定时清理任务
│       │   │   └── VerificationCodeServiceImpl.java # 验证码服务
│       │   └── *.java                    # 服务接口定义
│       │
│       ├── entity/                      # 数据实体 (MyBatis 映射)
│       │   ├── File.java                # 文件主表实体
│       │   ├── FileHash.java            # 哈希去重表实体
│       │   ├── FileBatch.java           # 批次管理实体
│       │   ├── VerificationCode.java    # 验证码实体
│       │   └── DownloadLimit.java       # 下载限制实体
│       │
│       ├── mapper/                      # MyBatis Mapper 接口
│       ├── config/                      # 配置类
│       ├── interceptor/                 # 请求拦截器
│       └── task/                        # 定时任务
│
├── 📁 frontend/                          # 前端应用 (Vue 3 + Vite + TypeScript)
│   └── src/
│       ├── views/                       # 页面组件
│       │   ├── HomeView.vue             # 首页 - 附近文件列表
│       │   ├── UploadView.vue           # 上传页 - 批次管理 + 分享
│       │   ├── DownloadRedirectView.vue # 下载中转页（安全校验入口）
│       │   └── ErrorView.vue            # 错误提示页
│       │
│       ├── components/                  # 通用组件
│       │   └── FileUpload.vue           # 可复用文件上传组件
│       │
│       ├── services/                    # 业务服务层
│       │   ├── locationService.ts       # 地理定位服务 (Haversine 公式)
│       │   ├── amapService.ts           # 高德地图服务
│       │   └── reconcileService.ts      # 数据对账服务
│       │
│       ├── stores/                      # 状态管理 (Pinia)
│       │   └── fileStore.ts             # 文件状态管理
│       │
│       ├── workers/                     # Web Worker
│       │   └── hash.worker.ts           # 前端哈希计算（并行）
│       │
│       ├── router/                      # 路由配置
│       │   └── index.ts                 # 路由定义
│       │
│       ├── api/                         # API 请求层
│       │   └── file.ts                  # 文件相关接口
│       │
│       └── utils/                       # 工具函数
│           ├── constants.ts             # 常量定义
│           └── request.ts               # Axios 封装
│
├── 📁 uploads/                           # 文件物理存储目录
├── 📁 public/                            # 静态资源
└── 📁 .git/                              # Git 仓库

```

---

## 🗺️ 系统架构与核心数据流

### 完整数据闭环流程图

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           文件上传与秒传完整流程                                  │
└─────────────────────────────────────────────────────────────────────────────────┘

[用户发起上传]
         │
         ▼
┌──────────────────────────────────────────────┐
│ 1️⃣ 前端计算 SHA-256 哈希指纹                 │
│    • JS-Sha256 库 (并行计算)                 │
│    • Web Worker 避免阻塞 UI                  │
│                                              │
└────────────┬─────────────────────────────────┘
             │
             ▼
┌──────────────────────────────────────────────┐
│ 2️⃣ 发送快速校验请求                          │
│    POST /api/file/quick-check                │
│    {      fileSize, sampleHash }              │
└────────────┬─────────────────────────────────┘
             │
     ┌───────┴────────┐
     │                │
     ▼                ▼
  [命中]           [未命中]
     │                │
     ▼                ▼
┌─────────────┐  ┌──────────────┐
│ 3️⃣ 触发秒传   │  │ 3️⃣ 完整上传   │
│  SecUpload  │  │ (FST逻辑)    │
└──────┬──────┘  └───────┬───────┘
       │                │
       │    ┌───────────┴──────────┐
       │    │                      │
       ▼    ▼                      ▼
┌───────────────────────┐  ┌──────────────────────────┐
│ 4️⃣ 后端原子性检查      │  │ 4️⃣ 后端物理存盘           │
│    FileHash.increment  │  │    • SHA256 哈希校验      │
│      Reference()       │  │    • 文件流写入磁盘       │
└───────┬───────────────┘  │    • 生成复杂存储路径     │
        │                  └──────────┬─────────────┘
        │                             │
        ▼                             ▼
┌───────────────────────┐  ┌──────────────────────────┐
│ 5️⃣ 哈希表记录已存在    │  │ 5️⃣ 插入新哈希记录         │
│    • referenceCount+1  │  │    FileHash (status=1)    │
│    • status=1          │  │    • 索引优化 (双重 Hash) │
│    • update_time=now   │  │    • 乐观锁防止冲突       │
└───────┬───────────────┘  └───────┬────────────────────┘
        │                         │
        └──────────┬──────────────┘
                   ▼
┌──────────────────────────────────────────────┐
│ 6️⃣ 业务记录写入 MySQL                        │
│    • t_file: 文件主表                        │
│    • t_file_batch: 批次管理                  │
│    • t_download_limit: 下载限制              │
│    • 生成 uploadToken / downloadToken       │
└───────┬──────────────────────────────────────┘
        │
        ▼
┌──────────────────────────────────────────────┐
│ 7️⃣ 地理位置写入 Redis Geo (公开文件)         │
│    ZADD file:locations:public                │
│    { lng, lat, uploadToken }                 │
│    • 集合成员: uploadToken                   │
│    • 位置: 文件坐标                          │
└───────┬──────────────────────────────────────┘
        │
        ▼
┌──────────────────────────────────────────────┐
│ 8️⃣ 前端 localStorage 持久化                  │
│    • myUploadedFiles: { fileId → token }     │
│    • geofile_private_pickup_batches           │
│    • geofile_public_upload_batches            │
│    • 实现"提前撤回"与"持久化管理"             │
└──────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────────┐
│                              附近文件搜索流程                                     │
└─────────────────────────────────────────────────────────────────────────────────┘

[用户进入首页]
         │
         ▼
┌──────────────────────────────────────────────┐
│ 1️⃣ 前端唤起浏览器 Geolocation API            │
│    navigator.geolocation.getCurrentPosition() │
│    • enableHighAccuracy: false (网络模糊定位)│
│    • timeout: 15000ms                        │
└────────────┬─────────────────────────────────┘
             │
             ▼
┌──────────────────────────────────────────────┐
│ 2️⃣ 请求后端保存位置                          │
│    POST /api/location/current                │
│    { lat, lng, radius: 1000 }                │
└────────────┬─────────────────────────────────┘
             │
             ▼
┌──────────────────────────────────────────────┐
│ 3️⃣ Redis Geo 空间检索 (1km 半径)            │
│    GEOSEARCH file:locations:public            │
│    BY BOX (lng-radius, lat-radius)           │
│    • 返回所有活跃 uploadToken 列表            │
│    • 毫秒级响应                              │
└────────────┬─────────────────────────────────┘
             │
             ▼
┌──────────────────────────────────────────────┐
│ 4️⃣ MySQL 批量查询聚合并返回                  │
│    SELECT * FROM t_file                      │
│    WHERE uploadToken IN (tokens)             │
│    AND status = 1                            │
│    • 计算实际距离 (Haversine 公式)            │
│    • 分页、排序、过滤                         │
└────────────┬─────────────────────────────────┘
             │
             ▼
┌──────────────────────────────────────────────┐
│ 5️⃣ 前端过滤渲染                               │
│    • 仅显示 distance < 1km 文件              │
│    • 显示距离值 (米/公里)                     │
└──────────────────────────────────────────────┘
```

---

## 💡 特色功能与硬核技术亮点

### 1. ✨ 去中心化免登录设计

**设计哲学**：利用浏览器 localStorage 存储特异性加密凭证，优雅区分"分享者"与"普通访客"。

```javascript
// 前端 localStorage 存储上传凭证
localStorage.setItem('myUploadedFiles', JSON.stringify({
  [fileId]: uploadToken  // 用户本地上传的文件 → 对应令牌
}))

// 路由鉴权机制
/s/:code        → 私密分享 (取件码)
/b/:token       → 公开分享 (上传令牌)
```

**核心优势**：
- ✅ 无需注册系统，降低准入门槛
- ✅ 上传者自动获得整批管理权
- ✅ 支持"提前撤回"与"持久化管理"
- ✅ 数据零泄露：凭证仅在用户浏览器本地

---

### 2. 🗺️ 基于 Redis Geo 的空间搜寻与时效清理

**技术原理**：
- 上传公开文件时，将 `uploadToken` 作为 **Geo Member**
- 将文件坐标 `(lng, lat)` 作为 **Geo Position**
- 存入 Redis **ZSET**：`ZADD file:locations:public lng lat token`

```java
// 后端写入 Redis Geo
String geoKey = "file:locations:public";
redisTemplate.opsForGeo().add(geoKey, new Point(lng, lat), uploadToken);

// 定时任务清理：当 Token 下所有文件失效时，移除该 Geo 成员
if (aliveCount == 0) {
    redisTemplate.opsForZSet().remove("file:locations:public", token);
}
```

**查询流程**：
```
1. 前端获取用户位置 (lat, lng)
2. 后端调用 Redis GEOSEARCH 指令
3. 获取 1km 范围内所有 uploadToken
4. 批量查询 MySQL 聚合并返回文件列表
```

**优势**：
- 🚀 **毫秒级检索**：Redis Geo 指令天然支持地理位置范围查询
- 🔄 **动态索引**：文件失效自动清理，不产生僵尸数据
- ⚖️ **读写分离**：Redis 读高频，MySQL 写主备

---

### 3. 🌐 公开文件 1km 圈内安全校验

**Haversine 公式物理计算**：

```java
/**
 * 半正矢公式计算两点间距离（单位：米）
 */
private double calculateDistanceInMeters(double lat1, double lng1, double lat2, double lng2) {
    double EARTH_RADIUS = 6371000; // 地球半径（米）
    double radLat1 = Math.toRadians(lat1);
    double radLat2 = Math.toRadians(lat2);
    double a = radLat1 - radLat2;
    double b = Math.toRadians(lng1) - Math.toRadians(lng2);

    double s = 2 * Math.asin(Math.sqrt(
            Math.pow(Math.sin(a / 2), 2) +
            Math.cos(radLat1) * Math.cos(radLat2) *
            Math.pow(Math.sin(b / 2), 2)
    ));
    return s * EARTH_RADIUS;
}
```

**双重校验机制**：

```java
// 第一步：Redis Geo 成员存在性检查
List<Point> posList = redisTemplate.opsForGeo().position("file:locations:public", token);
if (posList == null || posList.isEmpty()) {
    fileVO.setDistanceExceeded(true);
}

// 第二步：Haversine 物理距离计算
double meters = calculateDistanceInMeters(lat, lng, redisPoint.getY(), redisPoint.getX());
if (meters > 1000.0) {
    fileVO.setDistanceExceeded(true);
}
```

**安全优势**：
- 🔒 **双重保障**：数据库记录 ≠ Redis 索引 ≠ 物理距离校验
- ⚡ **无盲区**：任意环节失效，安全校验依然有效
- 🎯 **可扩展**：可轻松调整半径阈值（如 500m/2000m）

---

### 4. 🔑 私有文件取件码穿透

**设计原理**：私有文件通过 `is_private=1` 区分，采用独占式"取件码"校验，自动绕过地理位置限制。

```sql
-- 数据库字段设计
t_file.is_private TINYINT (0-公开, 1-私有)

-- 验证流程
1. 用户访问 /s/:code
2. 后端验证取件码有效性
3. ✅ 跳过地理位置限制
4. 返回完整文件列表
```

**Redis 高并发缓存**：

```java
// 取件码 → 文件列表映射（高并发场景优化）
String codeCacheKey = "code:" + code + ":files";
redisTemplate.opsForValue().set(codeCacheKey, filesJson, 30, TimeUnit.MINUTES);
```

**优势**：
- 🚀 **跨地域精准提取**：取件码全球有效
- 📊 **高并发性能**：Redis 缓存 + 连接池
- 🔐 **硬校验**：取件码失效自动清理映射关系

---

### 5. 📦 灵活的分享矩阵

#### 批次分享（私有）

```
上传模式：私有
生成内容：
  - 取件码 (6位字符)
  - uploadToken (64位令牌)
  - 文件列表

路由格式：/s/:code
访问方式：用户需输入取件码
权限：仅取件码持有者
```

#### 批次分享（公开）

```
上传模式：公开
生成内容：
  - uploadToken (64位令牌)
  - 文件列表

路由格式：/b/:token
访问方式：无需任何凭证
权限：1km 内所有用户
```

#### 单文件分享

```
上传模式：可选私有/公开

路由格式：
  - 私有：/s/:code (需取件码)
  - 公开：/b/:token (地理位置可见)
```

---

### 6. ⚡ 数据零拷贝秒传

**哈希指纹递归去重**：

```java
// 核心逻辑：原子性增加引用计数
boolean isExisting = fileHashService.incrementReference(sha256);

if (isExisting) {
    // 命中秒传：直接返回软引用
    FileHash existingRecord = fileHashService.findByHash(sha256);
    return createFileVO(existingRecord);
} else {
    // 未命中：物理写入 + 记录哈希
    fileStorageService.saveFileCustomPath(stream, fullPath);
    FileHash newHash = new FileHash(sha256, storagePath, size, ...);
    fileHashService.save(newHash);
}
```

**并发安全**：

```java
// 并发上传冲突处理
try {
    fileHashService.save(newHash);
} catch (DuplicateKeyException e) {
    // 极端并发：自动切换为秒传
    fileHashService.incrementReference(sha256);
    FileHash conflictRecord = fileHashService.findByHash(sha256);
    finalRelativePath = conflictRecord.getStoragePath();
}
```

**优势**：
- 💾 **节省存储**：相同文件仅需物理存储一份
- 🚀 **秒级上传**：前端快速校验 + 后端原子计数
- 📈 **流量优化**：去重文件不占用带宽

---

### 7. 🎨 智能文件预览体系

**格式自适应**：

```typescript
// 文本格式强制预览
const TEXT_EXTENSIONS = new Set([
  'txt', 'md', 'log', 'json', 'yaml', 'xml',
  'vue', 'ts', 'js', 'java', 'py', 'go', 'rs', 'c', 'cpp'
])

if (TEXT_EXTENSIONS.has(extension)) {
    contentType = 'text/plain; charset=utf-8'
} else if (extension === 'pdf') {
    contentType = 'application/pdf'
} else {
    contentType = 'application/octet-stream' // 强制下载
}
```

**压缩包内预览**：

```java
// 动态解压 ZIP/7Z 并生成虚拟文件树
ZipFile zipFile = new ZipFile(path);
Enumeration<ZipEntry> entries = zipFile.entries();

// 递归插入虚拟树结构
private void insertToTree(ArchiveNode root, ZipEntry entry) {
    String[] parts = entry.getName().split("/");
    ArchiveNode current = root;

    for (String partName : parts) {
        boolean isDir = !entry.isDirectory();
        ArchiveNode next = current.getChildren().stream()
            .filter(child -> child.getName().equals(partName))
            .findFirst().orElse(null);

        if (next == null) {
            next = new ArchiveNode(partName, isDir, 0);
            current.getChildren().add(next);
        }
        current = next;
    }
}
```

**响应式优化**：

```java
// 预览接口强制 inline 模式
.headers(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
.headers("X-Frame-Options", "ALLOWALL")  // 允许 iframe 嵌入
```

---

### 8. 🔄 高德 API 逆地理编码 & 自动化过期清理

**前后端协同定位**：

```typescript
// 前端：获取粗略位置
navigator.geolocation.getCurrentPosition((position) => {
    const lat = position.coords.latitude
    const lng = position.coords.longitude

    // 后端：调用高德逆地理编码
    fetch(`/api/amap/geocode-from-location?lat=${lat}&lng=${lng}`)
        .then(res => res.json())
        .then(data => {
            console.log(data.formattedAddress)  // "北京市朝阳区..."
        })
})
```

**后端地理编码服务**：

```java
@Service
public class AmapService {
    @Value("${amap.key}")
    private String apiKey;

    public GeocodingResponse reverseGeocode(Double lat, Double lng) {
        String url = String.format(
            "https://restapi.amap.com/v3/geocode/regeo?key=%s&location=%s,%s",
            apiKey, lng, lat
        );
        // HTTP 调用高德 API...
    }
}
```

**自动化过期清理**：

```java
// 每15分钟扫描一次过期文件
@Scheduled(cron = "0 0/15 * * * ?")
public void handleFileExpiration() {
    // 1. 找出即将过期的文件 (status=1 且 expireTime < now)
    // 2. 递减哈希引用计数
    // 3. 更新文件状态 (1 → 2 过期)
    // 4. 清理 Redis Geo 索引
}

// 每天凌晨 3 点物理清理
@Scheduled(cron = "0 0 3 * * ?")
public void physicalFileCleanup() {
    // 1. 查询引用计数为 0 且更新时间早于 24 小时的文件
    // 2. 物理删除文件
    // 3. 删除数据库记录
}
```

---

## 🚀 快速启动

### 环境要求

- **JDK 21+** (OpenJDK 或 Oracle)
- **Maven 3.8+**
- **Node.js 20+** (或 >=22.12.0)
- **MySQL 8.0+**
- **Redis 7.x**
- **高精度 GPS** (可选，测试用)

### 一键拉起

```bash
# 1. 克隆仓库
git clone https://github.com/your-repo/GeoFile.git
cd GeoFile

# 2. 后端启动 (Spring Boot 3)
cd backend
mvn spring-boot:run

# 3. 前端启动 (Vue 3 + Vite)
cd ../frontend
npm install
npm run dev

# 4. 访问服务
# 后端 API: http://localhost:8080/swagger-ui.html
# 前端页面: http://localhost:5173
```

### 手动配置

#### 后端配置

```yaml
# backend/src/main/resources/application.yml

spring:
  data:
    redis:
      host: localhost
      port: 6379
  datasource:
    url: jdbc:mysql://localhost:3306/geofile
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}

file:
  upload:
    path: ${user.home}/Projects/GeoFile/uploads  # 文件物理存储路径

amap:
  key: ${amap_api_key}  # 高德地图 API Key (https://console.amap.com/dev/key/app)
```

#### 前端配置

```typescript
// frontend/src/utils/constants.ts

// 配置后端 API 地址
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

// 测试模式：使用固定坐标（无需 GPS 硬件）
export const USE_FIXED_COORDS = localStorage.getItem('useFixedCoords') === 'true'
export const FIXED_COORDS = { lat: 38.914, lng: 121.614 }
```

### 初始化数据库

```bash
# 1. 创建数据库
mysql -u root -p -e "CREATE DATABASE geofile CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 2. 创建表结构 
导入项目中的 doc/sql/geofile.sql 完成建表

# 3. Redis安装并启动
redis-server
```

### 使用示例

```bash
# 1. 上传文件 (公开)
curl -X POST http://localhost:8080/api/file/upload \
  -F "file=@test.pdf" \
  -F "needCode=false" \
  -F "validMinutes=60"

# 2. 获取附近文件 (1km 半径)
curl "http://localhost:8080/api/file/nearby?lat=39.9042&lng=116.4074&radius=1000"

# 3. 私密分享 (取件码)
curl -X POST http://localhost:8080/api/file/upload \
  -F "file=@test.pdf" \
  -F "needCode=true" \
  -F "validMinutes=1440"  # 1天

# 4. 访问分享链接
# 私密：http://localhost:5173/s/ABCD12
# 公开：http://localhost:5173/b/uploadToken123
```

---

## 📊 数据库表设计

### t_file (文件主表)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| file_name | VARCHAR | 文件名 |
| original_name | VARCHAR | 原始文件名 |
| file_hash | VARCHAR(64) | SHA-256 哈希指纹 |
| file_size | BIGINT | 文件大小(字节) |
| file_path | VARCHAR | 物理存储路径 |
| storage_type | VARCHAR | 存储类型 (LOCAL/OSS) |
| upload_time | DATETIME | 上传时间 |
| expire_time | DATETIME | 有效截止时间 |
| download_count | INT | 下载次数 |
| status | TINYINT | 状态 (1-正常, 2-过期, 3-满额, 4-下架) |
| location_lat | DOUBLE | 地理纬度 |
| location_lng | DOUBLE | 地理经度 |
| location_radius | INT | 地理半径(米) |
| upload_token | VARCHAR(64) | 上传令牌 (公开批次) |
| download_token | VARCHAR(64) | 下载令牌 (下载验证) |
| is_private | TINYINT | 是否私有 (0-公开, 1-私有) |
| deleted | TINYINT | 逻辑删除 (0-未删, 1-已删) |

### t_file_hash (哈希去重表)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| file_hash | VARCHAR(64) | SHA-256 哈希值 (唯一索引) |
| md5 | VARCHAR(32) | MD5 快速校验值 |
| file_size | BIGINT | 文件大小 |
| storage_path | VARCHAR | 物理存储路径 |
| reference_count | INT | 逻辑引用计数 (0-待删) |
| status | TINYINT | 状态 (0-待删, 1-可用) |
| mime_type | VARCHAR | MIME 类型 |
| extension | VARCHAR | 文件后缀 |
| created_time | DATETIME | 创建时间 |

### t_file_batch (批次管理表)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| batch_token | VARCHAR(64) | 批次令牌 (对应 uploadToken) |
| extract_code | VARCHAR(32) | 取件码 (私有模式) |
| client_ip | VARCHAR | 上传者真实 IP |
| file_count | INT | 批次内文件总数 |
| total_size | BIGINT | 批次总大小 |
| is_private | TINYINT | 是否私有 (0-公开, 1-私有) |
| expire_time | DATETIME | 失效时间 (取件码) |
| created_time | DATETIME | 创建时间 |

### t_download_limit (下载限制表)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| file_id | BIGINT | 关联文件 ID |
| max_downloads | INT | 最大下载次数 |
| valid_minutes | INT | 有效时长(分钟) |
| used_count | INT | 已使用次数 |
| created_time | DATETIME | 创建时间 |

---

## 🔒 安全特性

- ✅ **HTTPS 强制**：地理定位 API 仅在 HTTPS 下可用
- ✅ **上传令牌校验**：防止未授权修改/删除他人文件
- ✅ **下载令牌防护**：单文件下载令牌唯一且独立
- ✅ **地理围栏限制**：公开文件仅限 1km 内访问
- ✅ **取件码穿透**：私有文件跳过地理限制，但仍需令牌
- ✅ **文件类型黑名单**：不允许指定格式上传/预览
- ✅ **最大文件大小**：3GB (可配置)
- ✅ **并发安全**：原子性引用计数 + 异常回滚

---

## 📝 开发指南

### 添加新的文件类型支持

```typescript
// frontend/src/utils/constants.ts
const SUPPORTED_FILE_TYPES = [
  'jpg', 'png', 'gif', 'pdf', 'doc', 'docx', 'xls', 'xlsx',
  'txt', 'md', 'json', 'yaml', 'vue', 'ts', 'js', 'java', 'py'
]

// 后端：扩展验证器
public class FileValidator {
    public boolean validate(String filename, String contentType, long size) {
        // 添加新的 MIME 类型映射
    }
}
```

### 调整地理围栏半径

```java
// FileUploadServiceImpl.java
// 修改 Redis Geo 写入半径阈值
if (Boolean.FALSE.equals(needCode)) {
    String geoKey = "file:locations:public";
    redisTemplate.opsForGeo().add(geoKey, new Point(lng, lat), uploadToken);
}

// 修改详情接口校验阈值
if (meters > 1000.0) {  // 改为 500.0 或 2000.0
    fileVO.setDistanceExceeded(true);
}
```

### 添加定时任务

```java
// FileLifecycleTask.java
@Scheduled(cron = "0 0 * * * ?")
public void customCleanupTask() {
    // 自定义清理逻辑
}
```

---

## 🐛 常见问题

**Q: 为什么我无法获取地理位置？**

A: 浏览器地理定位 API 仅在以下环境下可用：
- HTTPS 协议
- `localhost`
- IP 地址 (`127.0.0.1`)

**Q: 秒传何时会失败？**

A: 以下情况会触发完整上传：
- 前端未计算哈希（旧版浏览器）
- 数据库哈希记录被物理删除
- 并发上传冲突（极罕见）

**Q: 如何部署到生产环境？**

A:
1. 配置环境变量 (`.env` 文件)
2. 启用 HTTPS (Let's Encrypt)
3. 设置防火墙规则
4. 配置 MySQL 主从复制
5. 使用 Docker 容器化部署

**Q: 如何扩展存储？**

A:
1. 修改 `storage_type` 字段 (LOCAL → OSS)
2. 实现 `FileStorageService` 接口的 OSS 存储方法
3. 配置阿里云 OSS/MinIO/AWS S3

---

## 📄 许可证

[MIT License](LICENSE)

---

## 🙏 致谢

- [Vue.js](https://vuejs.org/) - 渐进式 JavaScript 框架
- [Spring Boot](https://spring.io/projects/spring-boot) - 企业级 Java 开发框架
- [Redis](https://redis.io/) - 内存数据结构存储
- [Element Plus](https://element-plus.org/) - Vue 3 组件库
- [高德地图 API](https://lbs.amap.com/) - 地理位置服务

---

<div align="center">

**如有问题或建议，欢迎提交 [Issue](https://github.com/your-repo/issues)**

⭐ 如果这个项目对你有帮助，请给个 Star！

</div>
