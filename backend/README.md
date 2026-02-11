# GeoFile 后端项目

## 快速开始

### 1. 环境准备
```bash
# Java
java -version  # 需要 JDK 17+

# Maven
mvn -version   # 需要 Maven 3.8+

# MySQL
mysql --version # 需要 MySQL 8.0+

# Redis
redis-server --version # 需要 Redis 7.0+
```

### 2. 项目初始化

#### 使用 Spring Initializr 创建项目
1. 访问 https://start.spring.io/
2. 选择配置：
   - Project: Maven
   - Language: Java
   - Spring Boot: 3.2.x
   - Packaging: Jar
   - Java: 17
3. 添加依赖：
   - Spring Web
   - Spring Security
   - MyBatis-Plus
   - MySQL Driver
   - Redis
   - Validation
   - Lombok
   - Knife4j

### 3. 配置文件

```yaml
# application.yml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/geofile?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: your_password

  data:
    redis:
      host: localhost
      port: 6379
      password:
      database: 0

# 阿里云OSS配置
aliyun:
  oss:
    endpoint: your_oss_endpoint
    access-key-id: your_access_key_id
    access-key-secret: your_access_key_secret
    bucket-name: your_bucket_name

# JWT配置
jwt:
  secret: your_secret_key_at_least_256_bits
  expiration: 7200000  # 2小时

# 文件配置
file:
  upload:
    path: /tmp/uploads
  max-size: 104857600  # 100MB
  allowed-types: jpg,jpeg,png,pdf,zip,rar,txt,doc,docx
```

### 4. MyBatis-Plus 代码生成器

创建 `CodeGenerator.java`:

```java
package com.geofile.generator;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;

import java.util.Collections;

public class CodeGenerator {
    public static void main(String[] args) {
        FastAutoGenerator.create("jdbc:mysql://localhost:3306/geofile", "root", "password")
            .globalConfig(builder -> {
                builder.author("geofile")
                    .outputDir(System.getProperty("user.dir") + "/src/main/java")
                    .enableSwagger()
                    .fileOverride();
            })
            .packageConfig(builder -> {
                builder.parent("com.geofile")
                    .moduleName("system")
                    .pathInfo(Collections.singletonMap(OutputFile.xml, System.getProperty("user.dir") + "/src/main/resources/mapper"));
            })
            .strategyConfig(builder -> {
                builder.addInclude("t_file", "t_user_location", "t_file_transfer", "t_verification_code", "t_download_limit")
                    .addTablePrefix("t_")
                    .entityBuilder()
                        .enableLombok()
                        .enableTableFieldAnnotation()
                        .controllerBuilder()
                        .enableRestStyle();
            })
            .templateEngine(new FreemarkerTemplateEngine())
            .execute();
    }
}
```

### 5. 核心模块结构

```
com.geofile
├── controller      # 控制器层
│   ├── FileController.java
│   ├── UploadController.java
│   ├── DownloadController.java
│   ├── PreviewController.java
│   └── GeoLocationController.java
├── service          # 服务层
│   ├── IFileService.java
│   ├── IUploadService.java
│   ├── IDownloadService.java
│   ├── IPreviewService.java
│   └── IGeoLocationService.java
├── service.impl     # 服务实现
│   ├── FileServiceImpl.java
│   ├── UploadServiceImpl.java
│   ├── DownloadServiceImpl.java
│   ├── PreviewServiceImpl.java
│   └── GeoLocationServiceImpl.java
├── mapper           # 数据访问层
├── entity           # 实体类
├── dto              # 数据传输对象
├── vo               # 视图对象
├── config           # 配置类
│   ├── MybatisPlusConfig.java
│   ├── RedisConfig.java
│   ├── SwaggerConfig.java
│   └── WebMvcConfig.java
├── security          # 安全配置
│   ├── SecurityConfig.java
│   ├── JwtAuthenticationTokenFilter.java
│   └── JwtUtil.java
├── util              # 工具类
│   ├── FileUtil.java
│   ├── ZipUtil.java
│   ├── ImageUtil.java
│   └── VideoUtil.java
└── exception         # 异常处理
    ├── GlobalExceptionHandler.java
    └── BusinessException.java
```

### 6. 部署步骤

```bash
# 1. 编译打包
mvn clean package

# 2. 运行jar包
java -jar geofile-backend-1.0.0.jar

# 3. 或者使用Docker
docker build -t geofile-backend .
docker run -p 8080:8080 geofile-backend
```

### 7. 常见问题

1. **数据库连接失败**：检查MySQL服务是否启动
2. **Redis连接失败**：检查Redis服务是否启动
3. **OSS上传失败**：检查OSS配置和账号权限
4. **跨域问题**：配置CORS

### 8. 访问地址

- Swagger文档: http://localhost:8080/doc.html
- Knife4j文档: http://localhost:8080/doc.html
