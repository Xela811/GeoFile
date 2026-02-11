# 快速开始指南

## 方式一：一键初始化（推荐）

### 1. 环境要求

- **JDK 17+**
- **Maven 3.8+**
- **Node.js 18+**
- **MySQL 8.0+**
- **Redis 7.0+**

### 2. 一键安装脚本

```bash
# 赋予脚本执行权限
chmod +x setup.sh
chmod +x init_backend.sh
chmod +x init_frontend.sh

# 运行初始化脚本
./setup.sh
```

### 3. 手动创建后端项目

```bash
# 进入后端目录
cd backend

# 使用Spring Initializr创建项目
# 访问: https://start.spring.io/
# 选择配置:
#   - Project: Maven
#   - Language: Java
#   - Spring Boot: 3.2.5
#   - Packaging: Jar
#   - Java: 17
#   - 添加依赖: Spring Web, Spring Security, MyBatis-Plus, MySQL Driver, Redis, Validation, Lombok, Knife4j

# 或使用IDE创建项目
# IntelliJ IDEA: File -> New -> Project -> Spring Initializr
```

### 4. 手动创建前端项目

```bash
# 进入前端目录
cd frontend

# 使用Vue CLI创建项目
npm create vue@latest
# 选择: TypeScript, Router, Pinia, Vuex(不选), ESLint, Prettier

# 安装依赖
npm install

# 安装UI组件库
npm install element-plus @element-plus/icons-vue

# 安装其他依赖
npm install axios pinia vue-router echarts
npm install -D sass
```

## 方式二：IDE创建（推荐新手）

### 后端项目创建（IntelliJ IDEA）

1. **File -> New -> Project**
2. **选择 Spring Initializr**
3. **配置项目信息：**
   - Project SDK: Java 17
   - Project name: geofile-backend
   - Group: com.geofile
   - Artifact: geofile-backend
   - Type: Maven
4. **添加依赖：**
   - Spring Web
   - Spring Security
   - Spring Validation
   - MyBatis Framework
   - MyBatis-Plus Driver
   - MySQL Driver
   - Redis
   - Lombok
   - Knife4j
   - Hutool
5. **Finish** 创建项目

### 前端项目创建（VS Code）

1. **创建文件夹** `frontend`
2. **打开终端** `npm create vue@latest`
3. **选择：**
   - TypeScript: Yes
   - Vue Router: Yes
   - Pinia: Yes
   - ESLint: Yes
   - Prettier: Yes
4. **进入项目目录** `cd frontend`
5. **安装依赖** `npm install`

## 数据库初始化

### 1. 创建数据库

```bash
mysql -u root -p

CREATE DATABASE geofile DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
EXIT;
```

### 2. 导入表结构

```bash
mysql -u root -p geofile < backend/sql/schema.sql
```

### 3. 修改数据库配置

编辑 `backend/src/main/resources/application.yml`，修改MySQL密码：

```yaml
spring:
  datasource:
    username: root
    password: your_mysql_password  # 修改这里
```

### 4. 启动Redis

```bash
# Linux
sudo systemctl start redis
sudo systemctl enable redis

# Windows
redis-server

# Docker
docker run -d -p 6379:6379 redis:7-alpine
```

## 启动项目

### 后端启动

```bash
cd backend

# 方式1: 使用Maven
mvn spring-boot:run

# 方式2: 打包后运行
mvn clean package
java -jar target/geofile-backend-1.0.0.jar

# 方式3: IDE直接运行
# 右键 GeoFileApplication.java -> Run
```

访问：
- 应用地址: http://localhost:8080
- API文档: http://localhost:8080/doc.html

### 前端启动

```bash
cd frontend

# 开发环境
npm run dev

# 生产构建
npm run build

# 预览构建结果
npm run preview
```

访问：http://localhost:3000

## 项目结构说明

```
GeoFile/
├── README.md                     # 项目说明
├── PROJECT_DESIGN.md             # 详细设计文档
├── GETTING_STARTED.md            # 快速开始指南
├── setup.sh                      # 一键初始化脚本
├── init_backend.sh               # 后端初始化脚本
├── init_frontend.sh              # 前端初始化脚本
├── backend/                      # 后端项目
│   ├── src/main/java/com/geofile/
│   │   ├── controller/           # 控制器层
│   │   ├── service/              # 服务层
│   │   ├── service/impl/         # 服务实现
│   │   ├── mapper/               # 数据访问层
│   │   ├── entity/               # 实体类
│   │   ├── config/               # 配置类
│   │   ├── security/             # 安全配置
│   │   ├── util/                 # 工具类
│   │   └── exception/            # 异常处理
│   ├── src/main/resources/
│   │   ├── mapper/               # MyBatis映射文件
│   │   └── application.yml       # 配置文件
│   └── sql/
│       └── schema.sql            # 数据库表结构
├── frontend/                     # 前端项目
│   ├── src/
│   │   ├── api/                  # API接口
│   │   ├── components/           # 公共组件
│   │   ├── layouts/              # 布局组件
│   │   ├── router/               # 路由配置
│   │   ├── stores/               # 状态管理
│   │   ├── utils/                # 工具函数
│   │   └── views/                # 页面组件
│   └── package.json
└── logs/                         # 日志目录
```

## 常见问题

### 1. Maven依赖下载失败

```bash
# 配置阿里云Maven镜像
# 编辑 ~/.m2/settings.xml
<mirrors>
    <mirror>
        <id>aliyun</id>
        <mirrorOf>central</mirrorOf>
        <url>https://maven.aliyun.com/repository/public</url>
    </mirror>
</mirrors>
```

### 2. Node模块安装失败

```bash
# 使用淘宝镜像
npm config set registry https://registry.npmmirror.com

# 或使用cnpm
npm install -g cnpm --registry=https://registry.npmmirror.com
cnpm install
```

### 3. MySQL连接失败

检查防火墙设置，确保MySQL端口3306开放。

### 4. Redis连接失败

确保Redis服务正在运行：

```bash
redis-cli ping
# 应该返回 PONG
```

## 下一步

1. 📖 阅读详细设计文档：`PROJECT_DESIGN.md`
2. 🛠️ 按照开发计划逐步实现功能
3. 📝 参考已创建的示例代码
4. 💡 遇到问题查看FAQ或提Issue

## 技术支持

- 查看详细文档：`PROJECT_DESIGN.md`
- API文档：`backend/README.md`
- 前端文档：`frontend/README.md`

祝开发顺利！🚀
