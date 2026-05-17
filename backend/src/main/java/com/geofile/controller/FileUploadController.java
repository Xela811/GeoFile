package com.geofile.controller;

import com.geofile.dto.QuickCheckDTO;
import com.geofile.dto.SecUploadDTO;
import com.geofile.entity.*;
import com.geofile.exception.DownloadException;
import com.geofile.service.FileHashService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.util.CollectionUtils;
import com.geofile.common.Result;
import com.geofile.service.FileUploadService;
import com.geofile.service.DownloadLimitService;
import com.geofile.service.FileService;
import com.geofile.util.RedisUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.springframework.core.io.ByteArrayResource;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

/**
 * 文件上传Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/file")
@CrossOrigin
@Tag(name = "文件上传", description = "文件上传相关接口")
public class FileUploadController {

    @Autowired
    private FileUploadService fileUploadService;

    @Autowired
    private FileService fileService;

    @Autowired
    private FileHashService fileHashService;

    @Value("${file.upload.path}")
    private String uploadPath;
    /**
     * 上传单个文件
     */
    @PostMapping("/upload")
    @Operation(summary = "上传单个文件", description = "支持大文件上传，包含进度显示")
    public Result<FileVO> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false, defaultValue = "0") Integer maxDownloads,
            @RequestParam(required = false, defaultValue = "0") Integer validMinutes,
            @RequestParam(required = false, defaultValue = "true") Boolean needCode) {
        try {
            log.info("文件上传开始: {}, 下载限制: {}次, 有效时长: {}分钟",
                    file.getOriginalFilename(), maxDownloads, validMinutes);

            FileVO result = fileUploadService.uploadFile(file, maxDownloads, validMinutes, needCode);

            return Result.success(result);

        } catch (Exception e) {
            log.error("文件上传失败: {}", file.getOriginalFilename(), e);
            return Result.error("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 上传单个文件并记录地理位置
     *
     * @param file 文件
     * @param lat 纬度
     * @param lng 经度
     * @param radius 搜索半径（米）
     * @param maxDownloads 最大下载次数
     * @param validMinutes 有效时长（分钟）
     * @return 文件信息
     */
    @PostMapping("/upload-with-location")
    @Operation(summary = "上传单个文件并记录位置", description = "上传文件时同时记录地理位置信息")
    public Result<FileVO> uploadFileWithLocation(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Integer radius,
            @RequestParam(required = false, defaultValue = "0") Integer maxDownloads,
            @RequestParam(required = false, defaultValue = "0") Integer validMinutes,
            @RequestParam(required = false, defaultValue = "true") Boolean needCode
    ) {
        try {
            log.info("文件上传并记录位置: {}, lat={}, lng={}, radius={}, 下载限制: {}次, 有效时长: {}分钟",
                    file.getOriginalFilename(), lat, lng, radius, maxDownloads, validMinutes);

            FileVO result = fileUploadService.uploadFile(file, lat, lng, radius, maxDownloads, validMinutes, needCode, null, null,null);

            return Result.success(result);

        } catch (Exception e) {
            log.error("文件上传失败: {}", file.getOriginalFilename(), e);
            return Result.error("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 上传多个文件
     */
    @PostMapping("/upload/batch")
    @Operation(summary = "批量上传文件", description = "一次上传多个文件")
    public Result<List<FileVO>> uploadFiles(@RequestParam("files") MultipartFile[] files) {
        try {
            log.info("批量文件上传开始: {} 个文件", files.length);

            List<FileVO> results = fileUploadService.uploadFiles(List.of(files));

            return Result.success(results);

        } catch (Exception e) {
            log.error("批量文件上传失败", e);
            return Result.error("批量文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 批量上传文件并记录位置
     *
     * @param files 文件列表
     * @param lat 纬度
     * @param lng 经度
     * @param radius 搜索半径（米）
     * @return 文件信息列表
     */
    @PostMapping("/upload/batch-with-location")
    @Operation(summary = "批量上传文件并记录位置", description = "批量上传文件时同时记录地理位置信息")
    public Result<List<FileVO>> uploadFilesWithLocation(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Integer radius,
            @RequestParam(required = false, defaultValue = "1") Integer maxDownloads,
            @RequestParam(required = false, defaultValue = "0") Integer validMinutes,
            @RequestParam(required = false, defaultValue = "true") Boolean needCode,
            @RequestParam(required = false) String providedToken,
            @RequestParam(required = false) List<String> sampleHashes,
            @RequestParam(required = false) List<String> fullHashes,
            HttpServletRequest request) {
        try {
            log.info("批量文件上传并记录位置: {} 个文件, lat={}, lng={}, radius={}, 下载限制: {}次, 有效时长: {}分钟",
                    files.length, lat, lng, radius, maxDownloads, validMinutes);

            List<FileVO> results = fileUploadService.uploadFilesWithLocation(List.of(files), lat, lng, radius, maxDownloads, validMinutes, needCode, providedToken, sampleHashes, fullHashes, request);

            return Result.success(results);

        } catch (Exception e) {
            log.error("批量文件上传失败", e);
            return Result.error("批量文件上传失败: " + e.getMessage());
        }
    }

    @PostMapping("/sec-upload")
    @Operation(summary = "文件秒传", description = "根据指纹校验实现秒传，支持加入现有批次")
    public Result<FileVO> secUpload(@RequestBody SecUploadDTO dto, HttpServletRequest request) {
        try {
            log.info("接收到秒传请求: fileName={}, hash={}", dto.getFileName(), dto.getHash());
            FileVO result = fileUploadService.secUpload(dto, request);

            if (result != null) {
                return Result.success(result);
            } else {
                // 404 状态码告诉前端：指纹不存在，请走普通上传逻辑
                return Result.error(404, "未命中秒传，请执行完整上传");
            }
        } catch (Exception e) {
            log.error("秒传处理失败", e);
            return Result.error("秒传失败: " + e.getMessage());
        }
    }

    @PostMapping("/quick-check")
    public Result<Map<String, Object>> quickCheck(@RequestBody QuickCheckDTO dto) {
        // 核心逻辑：查询是否存在大小和采样哈希同时匹配的记录
        boolean exists = fileHashService.count(new LambdaQueryWrapper<FileHash>()
                .eq(FileHash::getFileSize, dto.getSize())
                .eq(FileHash::getSampleHash, dto.getSampleHash())
                .last("LIMIT 1") // 只要找到一个就立刻停止，提升性能
        ) > 0;

        Map<String, Object> response = new HashMap<>();
        response.put("canPotentiallySecUpload", exists);

        return Result.success(response);
    }

    /**
     * 分片上传初始化
     */
    @PostMapping("/upload/init")
    @Operation(summary = "分片上传初始化", description = "初始化大文件分片上传")
    public Result<UploadInfo> initChunkUpload(
            @RequestParam("fileName") String fileName,
            @RequestParam("fileSize") long fileSize,
            @RequestParam(value = "chunkSize", defaultValue = "2097152") int chunkSize,
            @RequestParam(value = "chunkIndex", defaultValue = "0") int chunkIndex) {
        try {
            log.info("分片上传初始化: {}, 文件名: {}, 大小: {}", chunkIndex, fileName, fileSize);

            UploadInfo info = fileUploadService.initChunkUpload(fileName, fileSize, chunkSize, chunkIndex);

            return Result.success(info);

        } catch (Exception e) {
            log.error("分片上传初始化失败: {}", fileName, e);
            return Result.error("分片上传初始化失败: " + e.getMessage());
        }
    }

    /**
     * 分片上传
     */
    @PostMapping("/upload/chunk")
    @Operation(summary = "分片上传", description = "上传文件分片")
    public Result<UploadProgress> uploadChunk(
            @RequestParam("file") MultipartFile file,
            @RequestParam("fileName") String fileName,
            @RequestParam("chunkIndex") int chunkIndex,
            @RequestParam("totalChunks") int totalChunks,
            @RequestParam(value = "fileHash", required = false) String fileHash) {
        try {
            log.info("分片上传: {}, 索引: {}/{}", fileName, chunkIndex, totalChunks);

            UploadProgress progress = fileUploadService.uploadChunk(file, fileName, chunkIndex, totalChunks, fileHash);

            return Result.success(progress);

        } catch (Exception e) {
            log.error("分片上传失败: {}", fileName, e);
            return Result.error("分片上传失败: " + e.getMessage());
        }
    }

    /**
     * 合并分片
     */
    @PostMapping("/upload/merge")
    @Operation(summary = "合并分片", description = "合并所有分片为一个文件")
    public Result<FileVO> mergeChunks(
            @RequestParam("fileName") String fileName,
            @RequestParam("totalChunks") int totalChunks,
            @RequestParam(value = "fileHash", required = false) String fileHash) {
        try {
            log.info("合并分片开始: {}", fileName);

            FileVO result = fileUploadService.mergeChunks(fileName, totalChunks, fileHash);

            return Result.success(result);

        } catch (Exception e) {
            log.error("合并分片失败: {}", fileName, e);
            return Result.error("合并分片失败: " + e.getMessage());
        }
    }

    /**
     * 下载文件
     */
    @GetMapping("/download/{fileId}")
    @Operation(summary = "下载文件", description = "通过下载令牌验证后下载文件")
    public ResponseEntity<?> downloadFile(
            @PathVariable Long fileId,
            @RequestParam String token) {

        try {
            log.info("请求下载文件: fileId={}, token={}", fileId, token);

            // 1. 验证并获取文件
            com.geofile.entity.File file = fileUploadService.downloadFile(fileId, token);

            // 2. 构建文件路径
            java.nio.file.Path fullPath = java.nio.file.Paths.get(uploadPath)
                    .resolve(file.getFilePath())
                    .normalize();

            if (!java.nio.file.Files.exists(fullPath)) {
                log.error("物理文件丢失，数据库记录路径: {}, 拼接后的绝对路径: {}", file.getFilePath(), fullPath);
                return ResponseEntity.notFound().build();
            }

            // 3. 读取文件内容
            FileSystemResource resource = new FileSystemResource(fullPath.toFile());

            // 4. 设置响应头
            String encodedFileName = URLEncoder.encode(file.getOriginalName(), StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");

            HttpHeaders headers = new HttpHeaders();
            // 根据文件后缀动态探测 Content-Type
            String contentType = java.nio.file.Files.probeContentType(fullPath);
            headers.setContentType(contentType != null ? MediaType.parseMediaType(contentType) : MediaType.APPLICATION_OCTET_STREAM);
            //headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentLength(file.getFileSize());
            headers.setContentDispositionFormData("attachment", encodedFileName);

            // 增加 SHA-256 响应头，方便前端校验文件完整性
            if (file.getFileHash() != null) {
                headers.add("X-File-Hash-Sha256", file.getFileHash());
            }

            log.info("文件下载成功: {}, 大小: {} bytes", file.getFileName(), file.getFileSize());

            // 5. 返回文件内容
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);

        } catch (DownloadException e) {
            log.warn("拦截下载请求: {}", e.getMessage());
            return redirectToErrorPage(e.getMessage());
        } catch (Exception e) {
            log.error("下载接口崩溃", e);
            return redirectToErrorPage("服务器繁忙，请稍后再试");
        }
    }

    @GetMapping("/detail/{fileId}")
    @Operation(summary = "获取文件最新详情")
    public Result<FileVO> getFileDetail(@PathVariable Long fileId) {
        com.geofile.entity.File file = fileService.getById(fileId);
        if (file == null) return Result.error("文件不存在");
        return Result.success(fileUploadService.convertToFileVO(file));
    }

    private ResponseEntity<?> redirectToErrorPage(String message) {
        try {
            // 对中文消息进行编码，防止 URL 乱码
            String encodedMsg = URLEncoder.encode(message, StandardCharsets.UTF_8);
            String targetUrl = "http://localhost:5173/error?msg=" + encodedMsg;

            log.info("重定向至错误页: {}", targetUrl);

            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(targetUrl))
                    .build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 生成下载令牌（用于前端获取下载链接）
     */
    @GetMapping("/generate-download-token/{fileId}")
    @Operation(summary = "生成下载令牌", description = "为文件生成下载令牌")
    public Result<String> generateDownloadToken(@PathVariable Long fileId) {
        try {
            log.info("生成下载令牌: fileId={}", fileId);

            String token = fileUploadService.generateDownloadToken(fileId);

            return Result.success(token);

        } catch (Exception e) {
            log.error("生成下载令牌失败: fileId={}", fileId, e);
            return Result.error("生成下载令牌失败: " + e.getMessage());
        }
    }
    /**
     * 通过取件码提取文件列表（支持批量）
     */
    @GetMapping("/extract/{code}")
    public Result<List<FileVO>> extractFilesByCode(@PathVariable String code) {
        if (code == null || code.trim().isEmpty()) {
            return Result.error("请输入取件码");
        }

        try {
            // 修改为返回 List
            List<FileVO> fileVOs = fileUploadService.verifyAndGetFiles(code);
            return Result.success(fileVOs);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 通过上传令牌获取文件列表（用于公开批次管理刷新/对账）
     */
    @GetMapping("/list-by-token")
    @Operation(summary = "通过Token获取文件列表", description = "用于前端公开批次管理的自动刷新和状态校验")
    public Result<List<FileVO>> getFilesByToken(@RequestParam String uploadToken) {
        if (uploadToken == null || uploadToken.trim().isEmpty()) {
            return Result.error("uploadToken不能为空");
        }

        try {
            // 调用 Service 层新方法
            List<FileVO> fileVOs = fileUploadService.getFilesByUploadToken(uploadToken);

            // 修改点：如果 list 为空，说明 Token 无效，显式告知前端
            if (fileVOs == null || fileVOs.isEmpty()) {
                return Result.error("分享链接已失效或不包含有效文件");
            }

            return Result.success(fileVOs);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/reconcile")
    public Result<List<Long>> reconcileFiles(@RequestBody List<Long> fileIds) {
        if (CollectionUtils.isEmpty(fileIds)) {
            return Result.success(new ArrayList<>());
        }

        // 只查询数据库中 status 为 1 (正常) 或 3 (满额残影)
        // 且未被逻辑删除的文件 ID
        List<com.geofile.entity.File> validFiles = fileService.list(new LambdaQueryWrapper<com.geofile.entity.File>()
                .select(com.geofile.entity.File::getId)
                .in(com.geofile.entity.File::getId, fileIds)
                .in(com.geofile.entity.File::getStatus, 1, 3)
                .eq(com.geofile.entity.File::getDeleted, 0));

        List<Long> validIds = validFiles.stream()
                .map(com.geofile.entity.File::getId)
                .collect(Collectors.toList());

        return Result.success(validIds);
    }

    private static final Set<String> TEXT_EXTENSIONS = Stream.of(
            "txt", "md", "log", "csv", "json", "xml", "yaml", "yml",
            "ini", "env", "ts", "js", "vue", "html", "htm", "css",
            "scss", "less", "java", "py", "go", "rs", "c", "cpp",
            "h", "sql", "properties", "conf"
    ).collect(Collectors.toSet());

    @GetMapping("/preview/{fileId}")
    public ResponseEntity<Resource> previewFile(@PathVariable Long fileId, @RequestParam String token) {
        try {
            // 1. 业务逻辑校验（包含计数+1）
            com.geofile.entity.File fileEntity = fileService.processAccess(fileId, token);

            java.nio.file.Path fullPath = java.nio.file.Paths.get(uploadPath, fileEntity.getFilePath()).normalize();
            java.io.File diskFile = fullPath.toFile();
            if (!diskFile.exists()) {
                return ResponseEntity.notFound().build();
            }
            Resource resource = new UrlResource(diskFile.toURI());

            String originalFileName = fileEntity.getFileName();
            String extension = "";

            // 提取后缀（不含点）
            int lastDotIndex = originalFileName.lastIndexOf(".");
            if (lastDotIndex > 0) {
                extension = originalFileName.substring(lastDotIndex + 1).toLowerCase();
            }

            // 核心 MIME 识别逻辑
            //String contentType = Files.probeContentType(diskFile.toPath());
            String contentType = java.nio.file.Files.probeContentType(fullPath);

            // 满足你的要求：针对列表中的所有文本格式进行兜底
            if (TEXT_EXTENSIONS.contains(extension)) {
                // 关键安全点：无论 Files.probe 结果如何，强制设为 text/plain 避免 XSS 攻击
                // 并通过 charset=utf-8 解决预览乱码问题
                contentType = "text/plain; charset=utf-8";
            } else if ("pdf".equals(extension)) {
                contentType = "application/pdf";
            } else if (contentType == null) {
                contentType = "application/octet-stream";
            }

            // 3. 构建响应头
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    // 关键优化：inline 模式
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" +
                            URLEncoder.encode(originalFileName, "UTF-8") + "\"")
                    // 关键修复：移除或配置 X-Frame-Options，允许 iframe 嵌入
                    // 注意：如果项目中开启了 Spring Security，还需要在配置类中设置 .frameOptions().disable()
                    .header("X-Frame-Options", "ALLOWALL")
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .body(resource);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            log.error("预览失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/archive/list/{fileId}")
    public ResponseEntity<List<ArchiveNode>> listArchiveContent(@PathVariable Long fileId, @RequestParam String token) {
        try {
            // 1. 查询并校验
            com.geofile.entity.File file = fileService.getById(fileId);
            if (file == null) throw new IllegalArgumentException("文件不存在");

            // 2. 验证下载令牌
            if (file.getDownloadToken() == null || !file.getDownloadToken().equals(token)) {
                throw new IllegalArgumentException("无效的下载令牌");
            }

            // 3. 检查文件状态 (status=2过期, status=3耗尽)
            if (file.getDeleted() == 1) throw new IllegalArgumentException("文件已被删除");
            if (file.getStatus() != null) {
                if (file.getStatus() == 2 || (file.getExpireTime() != null && file.getExpireTime().before(new Date()))) {
                    throw new IllegalArgumentException("文件已过期");
                }
                if (file.getStatus() == 3) throw new IllegalArgumentException("文件下载次数已达上限");
            }


            java.nio.file.Path fullPath = java.nio.file.Paths.get(uploadPath, file.getFilePath()).normalize();
            java.io.File diskFile = fullPath.toFile();
            if (!diskFile.exists()) {
                return ResponseEntity.notFound().build();
            }
            // 4. 初始化虚拟根节点
            ArchiveNode root = new ArchiveNode("root", true, 0);

            try (java.util.zip.ZipFile zipFile = new java.util.zip.ZipFile(diskFile)) {
                java.util.Enumeration<? extends java.util.zip.ZipEntry> entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    java.util.zip.ZipEntry entry = entries.nextElement();
                    // 5. 将扁平路径插入树中
                    insertToTree(root, entry);
                }
            }
            // 返回根节点的子集（即压缩包第一层）
            return ResponseEntity.ok(root.getChildren());
        } catch (Exception e) {
            log.error("解析压缩包失败", e);
            return ResponseEntity.status(500).build();
        }
    }

    private void insertToTree(ArchiveNode root, java.util.zip.ZipEntry entry) {
        String[] parts = entry.getName().split("/");
        ArchiveNode current = root;

        for (int i = 0; i < parts.length; i++) {
            String partName = parts[i];
            if (partName.isEmpty()) continue;

            boolean isLastPart = (i == parts.length - 1);
            // 判断当前 part 是不是目录：
            // 如果不是最后一个 part，那它一定是目录；如果是最后一个 part，看 ZipEntry 标记
            boolean isDir = !isLastPart || entry.isDirectory();

            // 在当前节点的子节点中查找
            ArchiveNode next = null;
            for (ArchiveNode child : current.getChildren()) {
                if (child.getName().equals(partName)) {
                    next = child;
                    break;
                }
            }

            // 如果不存在则创建
            if (next == null) {
                next = new ArchiveNode(partName, isDir, isLastPart ? entry.getSize() : 0);
                current.getChildren().add(next);
            }
            current = next;
        }
    }
}
