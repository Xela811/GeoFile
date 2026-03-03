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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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
    public Result<FileVO> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            log.info("文件上传开始: {}", file.getOriginalFilename());

            FileVO result = fileUploadService.uploadFile(file);

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
}
