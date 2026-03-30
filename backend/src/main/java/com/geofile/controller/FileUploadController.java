package com.geofile.controller;

import com.geofile.common.Result;
import com.geofile.entity.FileVO;
import com.geofile.entity.UploadInfo;
import com.geofile.entity.UploadProgress;
import com.geofile.service.FileUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.core.io.ByteArrayResource;

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

    /**
     * 上传单个文件
     */
    @PostMapping("/upload")
    @Operation(summary = "上传单个文件", description = "支持大文件上传，包含进度显示")
    public Result<FileVO> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false, defaultValue = "0") Integer maxDownloads,
            @RequestParam(required = false, defaultValue = "0") Integer validMinutes) {
        try {
            log.info("文件上传开始: {}, 下载限制: {}次, 有效时长: {}分钟",
                    file.getOriginalFilename(), maxDownloads, validMinutes);

            FileVO result = fileUploadService.uploadFile(file, maxDownloads, validMinutes);

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
            @RequestParam(required = false, defaultValue = "0") Integer validMinutes) {
        try {
            log.info("文件上传并记录位置: {}, lat={}, lng={}, radius={}, 下载限制: {}次, 有效时长: {}分钟",
                    file.getOriginalFilename(), lat, lng, radius, maxDownloads, validMinutes);

            FileVO result = fileUploadService.uploadFile(file, lat, lng, radius, maxDownloads, validMinutes);

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
            @RequestParam(required = false, defaultValue = "0") Integer validMinutes) {
        try {
            log.info("批量文件上传并记录位置: {} 个文件, lat={}, lng={}, radius={}, 下载限制: {}次, 有效时长: {}分钟",
                    files.length, lat, lng, radius, maxDownloads, validMinutes);

            List<FileVO> results = fileUploadService.uploadFilesWithLocation(List.of(files), lat, lng, radius, maxDownloads, validMinutes);

            return Result.success(results);

        } catch (Exception e) {
            log.error("批量文件上传失败", e);
            return Result.error("批量文件上传失败: " + e.getMessage());
        }
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
    public ResponseEntity<Resource> downloadFile(
            @PathVariable Long fileId,
            @RequestParam String token) {

        try {
            log.info("请求下载文件: fileId={}, token={}", fileId, token);

            // 1. 验证并获取文件
            com.geofile.entity.File file = fileUploadService.downloadFile(fileId, token);

            // 2. 构建文件路径
            File filePath = new File(file.getFilePath());
            if (!filePath.exists()) {
                log.warn("文件不存在: {}", file.getFilePath());
                return ResponseEntity.notFound().build();
            }

            // 3. 读取文件内容
            FileInputStream fis = new FileInputStream(filePath);
            byte[] fileContent = new byte[(int) filePath.length()];
            fis.read(fileContent);
            fis.close();

            // 4. 设置响应头
            String encodedFileName = URLEncoder.encode(file.getOriginalName(), StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentLength(file.getFileSize());
            headers.setContentDispositionFormData("attachment", encodedFileName);

            log.info("文件下载成功: {}, 大小: {} bytes", file.getFileName(), file.getFileSize());

            // 5. 返回文件内容
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(new ByteArrayResource(fileContent));

        } catch (IllegalArgumentException e) {
            log.error("下载失败: fileId={}, error={}", fileId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("下载文件失败: fileId={}", fileId, e);
            return ResponseEntity.internalServerError().build();
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
}
