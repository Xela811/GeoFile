# GeoFile 项目详细开发计划

## 一、开发时间线（建议25天）

### Week 1: 项目基础搭建（第1-5天）

#### Day 1: 环境准备和项目初始化
- [ ] 后端环境配置（JDK, Maven, MySQL, Redis）
- [ ] 前端环境配置（Node.js, Vue CLI）
- [ ] 创建后端Spring Boot项目
- [ ] 创建前端Vue项目
- [ ] 配置数据库连接
- [ ] 配置Redis连接

**产出：** 可运行的空项目

#### Day 2: 数据库设计和初始化
- [ ] 创建数据库 `geofile`
- [ ] 创建5张核心表（见README.md）
- [ ] 编写SQL脚本
- [ ] 在MySQL中执行建表语句
- [ ] 初始化测试数据

**产出：** 完整的数据库结构

#### Day 3: 后端基础配置
- [ ] MyBatis-Plus配置和代码生成器
- [ ] Swagger/Knife4j配置
- [ ] Redis配置类
- [ ] 异常处理全局配置
- [ ] 统一返回结果封装

**产出：** 基础框架就绪

#### Day 4: 前端基础配置
- [ ] 路由配置
- [ ] Axios封装和拦截器
- [ ] 全局样式和主题配置
- [ ] 布局组件设计
- [ ] 登录页面

**产出：** 可访问的前端框架

#### Day 5: 用户认证和授权
- [ ] JWT工具类
- [ ] Spring Security配置
- [ ] 拦截器配置
- [ ] 登录API开发
- [ ] 前端登录功能

**产出：** 完整的认证系统

---

### Week 2: 核心功能开发（第6-10天）

#### Day 6: 文件上传功能
- [ ] 接收文件上传接口
- [ ] 文件验证（大小、类型）
- [ ] 本地存储实现
- [ ] 前端上传组件
- [????] 进度条显示

**产出：** 文件上传功能

#### Day 7: 地理位置服务
- [ ] IP地址转地理位置API
- [????] 高德/百度地图API集成
- [ ] 前端获取位置功能
- [ ] 地理位置存储
- [????] 附近文件搜索接口

**产出：** 地理位置服务

#### Day 8: 文件管理（CRUD）
- [ ] 文件列表查询（分页、搜索）
- [ ] 文件详情查询
- [ ] 文件删除功能
- [ ] 文件状态管理
- [ ] 前端文件列表页面

**产出：** 完整的文件管理

#### Day 9: 文件下载和限制
- [ ] 下载接口开发
- [ ] 下载次数限制
- [ ] 有效时长限制
- [ ] 下载记录统计
- [ ] 文件传输记录表
- [ ] 验证码生成和验证

**产出：** 文件下载功能

#### Day 10: 验证码系统
- [ ] 图形验证码实现
- [ ] 短信验证码（可选）
- [ ] 验证码存储（Redis）
- [ ] 验证码验证逻辑
- [ ] 防刷机制

**产出：** 验证码系统

---

### Week 3: 高级功能开发（第11-15天）

#### Day 11: 断点续传
- [ ] 文件分片上传
- [ ] 断点记录存储
- [ ] 从断点继续上传
- [ ] 秒传功能实现
- [ ] 前端断点续传逻辑

**产出：** 断点续传功能

#### Day 12: 阿里云OSS集成
- [ ] OSS SDK配置
- [ ] OSS上传接口
- [ ] OSS断点续传
- [ ] OSS文件管理
- [ ] 文件存储切换（本地/OSS）

**产出：** OSS存储集成

#### Day 13: 在线预览（文本、图片）
- [ ] 文本文件预览
- [ ] 图片预览
- [ ] 视频预览（转码）
- [ ] Office文档预览
- [ ] 前端预览组件

**产出：** 在线预览功能

#### Day 14: 在线解压功能
- [ ] ZIP/RAR解压工具
- [ ] 在线解压接口
- [ ] 压缩包文件列表
- [ ] 批量下载
- [ ] 临时文件清理

**产出：** 在线解压功能

#### Day 15: API文档完善
- [ ] 接口注释补充
- [ ] 请求参数说明
- [ ] 响应格式说明
- [ ] 错误码文档
- [ ] 使用示例

**产出：** 完善的API文档

---

### Week 4: 优化和部署（第16-20天）

#### Day 16: 性能优化
- [ ] 数据库索引优化
- [ ] Redis缓存优化
- [ ] 查询优化
- [ ] 并发处理优化
- [ ] 前端性能优化

**产出：** 性能优化版本

#### Day 17: 安全加固
- [ ] SQL注入防护
- [ ] XSS防护
- [ ] CSRF防护
- [ ] 文件上传安全
- [ ] 接口权限控制

**产出：** 安全加固版本

#### Day 18: 测试和调试
- [ ] 单元测试
- [ ] 接口测试
- [ ] 压力测试
- [ ] Bug修复
- [ ] 功能验证

**产出：** 稳定版本

#### Day 19: 部署配置
- [ ] Nginx配置
- [ ] Docker配置
- [ ] 生产环境部署
- [ ] 域名配置
- [ ] SSL证书配置

**产出：** 生产环境部署

#### Day 20: 文档完善
- [ ] 用户使用手册
- [ ] 开发文档
- [ ] 部署文档
- [ ] 维护手册
- [ ] README更新

**产出：** 完整文档

---

### Week 5: 项目打磨和求职准备（第21-25天）

#### Day 21: 视频录制
- [ ] 功能演示视频
- [ ] 技术亮点展示
- [ ] 代码讲解录制
- [ ] 发布到GitHub

**产出：** 演示视频

#### Day 22: GitHub维护
- [ ] 完善GitHub仓库
- [ ] 添加Issues模板
- [ ] 添加Contribution Guide
- [ ] 代码审查

**产出：** 专业的GitHub仓库

#### Day 23: 简历更新
- [ ] 添加项目经验
- [ ] 技术栈总结
- [ ] 项目亮点提炼
- [ ] 简历模板更新

**产出：** 更新后的简历

#### Day 24: 面试准备
- [ ] 项目技术问题准备
- [ ] SQL面试题准备
- [ ] Spring面试题准备
- [ ] JVM面试题准备
- [ ] LeetCode刷题

**产出：** 面试资料包

#### Day 25: 求职投递
- [ ] 更新Boss直聘/猎聘
- [ ] 发送GitHub链接
- [ ] 撰写作品集
- [ ] 联系内推人员

**产出：** 求职投递完成

---

## 二、技术实现重点

### 1. 断点续传实现

#### 后端实现
```java
// 分片上传接口
@PostMapping("/upload/chunk")
public Result uploadChunk(
    @RequestParam("file") MultipartFile file,
    @RequestParam("chunkIndex") int chunkIndex,
    @RequestParam("totalChunks") int totalChunks,
    @RequestParam("fileHash") String fileHash) {
    // 1. 保存分片到临时目录
    // 2. 记录已上传的分片
    // 3. 返回分片信息
}

// 合并分片接口
@PostMapping("/upload/merge")
public Result mergeChunks(
    @RequestParam("fileHash") String fileHash,
    @RequestParam("fileName") String fileName,
    @RequestParam("totalChunks") int totalChunks) {
    // 1. 检查所有分片是否完整
    // 2. 合并分片
    // 3. 保存到OSS或本地
    // 4. 清理临时文件
}
```

#### 前端实现
```typescript
// 分片上传逻辑
async function uploadChunk(file: File, chunkIndex: number, totalChunks: number) {
  const formData = new FormData()
  formData.append('file', file.slice(start, end))
  formData.append('chunkIndex', chunkIndex.toString())
  formData.append('totalChunks', totalChunks.toString())
  formData.append('fileHash', fileHash)

  await uploadFile(formData)
}

// 进度计算
const progress = Math.round((uploadedChunks / totalChunks) * 100)
```

### 2. 在线解压实现

```java
// 解压工具类
public class ZipUtil {
    public static List<FileVO> unzipFile(Path zipFilePath, Path extractPath) throws IOException {
        List<FileVO> fileInfos = new ArrayList<>();

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFilePath.toFile()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path filePath = extractPath.resolve(entry.getName());

                if (entry.isDirectory()) {
                    Files.createDirectories(filePath);
                } else {
                    Files.copy(zis, filePath, StandardCopyOption.REPLACE_EXISTING);

                    FileVO vo = new FileVO()
                        .setFileName(entry.getName())
                        .setFileSize(Files.size(filePath))
                        .setFileType(FileTypeUtil.getFileType(entry.getName()));
                    fileInfos.add(vo);
                }

                zis.closeEntry();
            }
        }

        return fileInfos;
    }
}
```

### 3. 地理位置服务实现

```java
// IP地址转地理位置
public class GeoLocationService {

    // 使用高德地图API
    public LocationInfo getLocationByIp(String ip) {
        RestTemplate restTemplate = new RestTemplate();
        String url = "https://restapi.amap.com/v3/ip?key=YOUR_KEY&ip=" + ip;

        Map<String, Object> response = restTemplate.getForObject(url, Map.class);

        if ("1".equals(response.get("status"))) {
            Map<String, Object> regeocode = (Map<String, Object>) response.get("regeocode");
            Map<String, Object> addressComponent = (Map<String, Object>) regeocode.get("addressComponent");

            return LocationInfo.builder()
                .province(addressComponent.get("province").toString())
                .city(addressComponent.get("city").toString())
                .district(addressComponent.get("district").toString())
                .build();
        }

        return null;
    }

    // 获取附近文件
    public List<FileVO> searchNearbyFiles(Double lat, Double lng, Integer radius) {
        // 使用MyBatis-Plus的Spatial查询
        return fileMapper.selectList(new LambdaQueryWrapper<File>()
            .between(File::getLocationLat, lat - radius/111000, lat + radius/111000)
            .between(File::getLocationLng, lng - radius/(111000*Math.cos(lat*Math.PI/180)),
                     lng + radius/(111000*Math.cos(lat*Math.PI/180)))
            .le(File::getExpireTime, LocalDateTime.now())
            .eq(File::getStatus, 1));
    }
}
```

### 4. 阿里云OSS集成

```java
@Configuration
public class OssConfig {

    @Value("${aliyun.oss.endpoint}")
    private String endpoint;

    @Value("${aliyun.oss.access-key-id}")
    private String accessKeyId;

    @Value("${aliyun.oss.access-key-secret}")
    private String accessKeySecret;

    @Value("${aliyun.oss.bucket-name}")
    private String bucketName;

    @Bean
    public OSS ossClient() {
        return new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
    }

    @Bean
    public String uploadFile(MultipartFile file) {
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        String objectName = "uploads/" + fileName;

        ossClient.putObject(bucketName, objectName, file.getInputStream());
        ossClient.setObjectAcl(bucketName, objectName, CannedAccessControlList.PublicRead);

        return "https://" + bucketName + "." + endpoint + "/" + objectName;
    }
}
```

---

## 三、面试亮点准备

### 1. 项目难点及解决方案

**难点1：断点续传的实现**
- 问题：大文件上传需要断点续传
- 解决：
  - 客户端将文件分片上传
  - 服务端记录每个分片的上传状态
  - 支持从断点继续上传
  - 使用Redis存储上传进度

**难点2：地理位置搜索**
- 问题：如何高效搜索附近的文件
- 解决：
  - 使用MySQL的空间索引
  - 估算半径范围内的坐标范围
  - 使用MyBatis-Plus的LambdaQueryWrapper

**难点3：文件安全**
- 问题：防止恶意文件上传和下载滥用
- 解决：
  - 文件类型白名单验证
  - 文件大小限制
  - 下载次数和时间限制
  - 文件名加密存储

### 2. 技术亮点

1. **MyBatis-Plus代码生成器**：自动生成CRUD代码，提高开发效率
2. **Redis缓存**：缓存热点数据，提升性能
3. **阿里云OSS**：分布式存储，扩展性强
4. **断点续传**：支持大文件上传，提升用户体验
5. **在线预览**：支持多种文件格式在线查看
6. **地理位置服务**：实现基于位置的文件搜索

### 3. 数据库优化

```sql
-- 复合索引优化查询
CREATE INDEX idx_file_upload_expire ON t_file(upload_time, expire_time);
CREATE INDEX idx_file_location_radius ON t_file(location_lat, location_lng, location_radius);

-- 使用Spatial索引
ALTER TABLE t_file ADD SPATIAL INDEX idx_location_spatial (location_lat, location_lng);
```

---

## 四、常见面试题准备

### MySQL相关

1. **索引优化**
   - 如何设计合适的索引？
   - 联合索引的使用场景？
   - 覆盖索引是什么？

2. **事务隔离级别**
   - SpringBoot默认的事务隔离级别？
   - 幻读、不可重复读的区别？

3. **数据库锁**
   - 行锁、表锁、间隙锁的区别？

### Spring相关

1. **IOC和AOP**
   - IOC的原理是什么？
   - AOP的实现方式？
   - 动态代理和静态代理的区别？

2. **Spring Boot自动配置**
   - 自动配置原理？
   - 条件注解的使用？

3. **事务管理**
   - 事务传播行为有哪些？
   - 事务失效的场景？

### Java基础

1. **集合框架**
   - ArrayList和LinkedList的区别？
   - HashMap的底层实现？

2. **多线程**
   - 线程池参数配置？
   - 线程安全问题如何解决？

3. **JVM**
   - 内存模型？
   - 垃圾回收算法？

---

## 五、学习资源推荐

### 前端
- Vue官方文档：https://vuejs.org/
- Element Plus文档：https://element-plus.org/
- TypeScript手册：https://www.typescriptlang.org/

### 后端
- Spring官方文档：https://spring.io/projects/spring-framework
- MyBatis-Plus文档：https://baomidou.com/
- Spring Boot官方文档：https://spring.io/projects/spring-boot

### 工具
- Postman：接口测试
- Navicat：数据库管理
- IDEA：开发工具
- Docker：容器化部署

---

## 六、预期成果

完成开发后，你将拥有：

1. ✅ 一个完整的企业级文件传输项目
2. ✅ 符合MyBatis-Plus规范的数据库设计
3. ✅ 完善的API文档（Swagger/Knife4j）
4. ✅ 漂亮的演示视频（5-10分钟）
5. ✅ 专业的GitHub仓库
6. ✅ 更新后的简历
7. ✅ 充分的面试准备

这个项目将帮助你在大连的外企求职中脱颖而出！
