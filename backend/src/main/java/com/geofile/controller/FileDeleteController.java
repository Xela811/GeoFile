package com.geofile.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.geofile.common.Result;
import com.geofile.entity.File;
import com.geofile.service.FileHashService;
import com.geofile.service.FileLogService;
import com.geofile.service.FileService;
import com.geofile.util.IpUtils;
import com.geofile.util.RedisUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件删除 Controller
 * 基于上传令牌的免登录文件删除功能
 */
@Slf4j
@RestController
@RequestMapping("/api/file")
@CrossOrigin
@Tag(name = "文件删除", description = "基于上传令牌的免登录文件删除接口")
public class FileDeleteController {

    @Autowired
    private FileService fileService;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private FileHashService fileHashService;

    @Autowired
    private FileLogService fileLogService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 通过上传令牌删除文件
     *
     * @param fileId 文件ID
     * @param uploadToken 上传令牌
     * @return 删除结果
     */
    @DeleteMapping("/delete/{fileId}")
    @Operation(summary = "删除文件", description = "通过上传令牌验证身份后删除文件")
    public Result<String> deleteFile(
            @Parameter(description = "文件ID", example = "1") @PathVariable Long fileId,
            @Parameter(description = "上传令牌", required = true) @RequestParam String uploadToken,
            HttpServletRequest request) {
        String clientIp = IpUtils.getClientIp(request);
        try {
            log.info("请求删除文件: fileId={}, uploadToken={}", fileId, uploadToken);

            // 1. 查询文件
            File file = fileService.getById(fileId);
            if (file == null) {
                log.warn("文件不存在: fileId={}", fileId);
                fileLogService.recordLog(fileId, "DELETE", 0, "删除失败：文件不存在", null, null, clientIp);
                return Result.error("文件不存在");
            }

            // 2. 验证上传令牌
            if (file.getUploadToken() == null || !file.getUploadToken().equals(uploadToken)) {
                log.warn("上传令牌验证失败: fileId={}, token={}", fileId, uploadToken);
                fileLogService.recordLog(fileId, "DELETE", 0, "删除失败：无权限删除此文件，上传令牌无效", null, null, clientIp);
                return Result.error("无权限删除此文件，上传令牌无效");
            }

            // 3. 检查文件是否已被删除
            if (file.getStatus() != null && file.getStatus() == 0 && file.getDeleted() == 1) {
                fileLogService.recordLog(fileId, "DELETE", 0, "删除失败：文件此前已被删除", null, null, clientIp);
                return Result.error("文件已被删除");
            }

            // --- 核心修改：更新引用计数 ---
            if (file.getFileHash() != null) {
                // 调用你 Service 中的方法：该方法内部应执行 UPDATE t_file_hash SET reference_count = reference_count - 1
                fileHashService.decrementReference(file.getFileHash());
                log.info("物理文件引用计数减1: hash={}", file.getFileHash());
            }

            // 4. 执行软删除（更新状态为已删除）
            file.setStatus(0);
            file.setDeleted(1);
            fileService.updateById(file);

            // ======= 核心修改：同步清理 RedisGEO =======
            String currentToken = file.getUploadToken();
            // 检查该批次是否还有“活着”的文件（status 为 1-正常 或 3-满额残留）
            long aliveCount = fileService.count(new LambdaQueryWrapper<File>()
                    .eq(File::getUploadToken, currentToken)
                    .in(File::getStatus, Arrays.asList(1, 3))
                    .eq(File::getDeleted, 0));

            if (aliveCount == 0) {
//                // 说明该批次（Token）下的所有文件都已进入 status=0 (手动删除) 或被逻辑删除
//                String geoKey = "file:locations:public";
//                redisTemplate.opsForZSet().remove(geoKey, currentToken);
//                log.info("该批次文件已全部删除，同步清理 RedisGEO 索引: token={}", currentToken);
                boolean isPrivate = file.getIsPrivate() != null && file.getIsPrivate() == 1;
                // 1. 【精准清理】判断批次类型
                // 建议查询一下该批次的信息，或者看 file 表记录
                if (isPrivate) {
                    // --- 处理私有文件清理 ---
                    // 先查出 code，再删掉双向索引
                    String codeKey = "file:download:" + currentToken;
                    String downloadCode = (String) redisUtil.get(codeKey);
                    if (downloadCode != null) {
                        redisUtil.del(codeKey);
                        redisUtil.del("code:to:token:" + downloadCode);
                        log.info("该私有批次已全部删除，同步清理 Redis 取件码关系: token={}", currentToken);
                    }
                } else {
                    // --- 处理公开文件清理 ---
                    String geoKey = "file:locations:public";
                    redisTemplate.opsForZSet().remove(geoKey, currentToken);
                    log.info("该公开批次已全部删除，同步清理 RedisGEO 索引: token={}", currentToken);
                }
            }
            // ===========================================

            log.info("文件删除成功: fileId={}", fileId);
            fileLogService.recordLog(fileId, "DELETE", 1, "文件删除成功", null, null, clientIp);
            return Result.success("文件删除成功");

        } catch (Exception e) {
            log.error("删除文件失败: fileId={}", fileId, e);
            fileLogService.recordLog(fileId, "DELETE", 0, "删除异常: " + e.getMessage(), null, null, clientIp);
            return Result.error("删除文件失败: " + e.getMessage());
        }
    }

    /**
     * 验证上传令牌是否有效（检查是否有权限管理该文件）
     *
     * @param fileId 文件ID
     * @param uploadToken 上传令牌
     * @return 验证结果
     */
    @GetMapping("/verify-ownership/{fileId}")
    @Operation(summary = "验证文件所有权", description = "验证上传令牌是否有效，判断是否有权限管理该文件")
    public Result<Boolean> verifyOwnership(
            @Parameter(description = "文件ID", example = "1") @PathVariable Long fileId,
            @Parameter(description = "上传令牌", required = true) @RequestParam String uploadToken) {

        try {
            File file = fileService.getById(fileId);
            if (file == null) {
                return Result.success(false);
            }

            boolean hasOwnership = file.getUploadToken() != null && file.getUploadToken().equals(uploadToken);
            return Result.success(hasOwnership);

        } catch (Exception e) {
            log.error("验证文件所有权失败: fileId={}", fileId, e);
            return Result.error("验证失败: " + e.getMessage());
        }
    }

    /**
     * 通过上传令牌删除同一批次的全部文件（与批量上传共用 uploadToken），并清理取件码相关 Redis 键
     */
    @DeleteMapping("/batch-by-upload-token")
    @Operation(summary = "按批次删除文件", description = "使用批量上传返回的 uploadToken，删除该令牌下全部未删文件，并失效取件码绑定")
    public Result<Map<String, Object>> deleteBatchByUploadToken(
            @Parameter(description = "批量上传返回的上传令牌", required = true) @RequestParam String uploadToken,
            HttpServletRequest request) {
        if (uploadToken == null || uploadToken.isBlank()) {
            return Result.error("uploadToken 不能为空");
        }
        String token = uploadToken.trim();
        String clientIp = IpUtils.getClientIp(request);
        try {
            List<File> files = fileService.list(new LambdaQueryWrapper<File>()
                    .eq(File::getUploadToken, token)
                    .in(File::getStatus, Arrays.asList(1, 3)));

            if (files == null || files.isEmpty()) {
                String redisKey = "file:download:" + token;
                Object codeObj = redisUtil.get(redisKey);
                if (codeObj != null) {
                    redisUtil.del("code:to:token:" + codeObj.toString());
                }
                redisUtil.del(redisKey);
                fileLogService.recordLog(null, "BATCH_DELETE", 0, "批次删除失败：未找到该批次文件或已全部删除", null, null, clientIp);
                return Result.error("未找到该批次文件或已全部删除");
            }

            // 识别批次属性（以第一条文件为准即可）
            boolean isPrivate = files.get(0).getIsPrivate() != null && files.get(0).getIsPrivate() == 1;

            for (File f : files) {
                // --- 核心修改：循环处理每一个文件的哈希引用 ---
                if (f.getFileHash() != null) {
                    fileHashService.decrementReference(f.getFileHash());
                }

                f.setStatus(0);
                f.setDeleted(1);
                fileService.updateById(f);
                fileLogService.recordLog(f.getId(), "BATCH_DELETE", 1, "批次内文件随整批删除成功", null, null, clientIp);
            }
            if(isPrivate) {
                String redisDownloadKey = "file:download:" + token;
                Object codeObj = redisUtil.get(redisDownloadKey);
                if (codeObj != null) {
                    redisUtil.del("code:to:token:" + codeObj.toString());
                }
                redisUtil.del(redisDownloadKey);
            } else {
                // 公开模式：清理 Redis GEO 索引
                String geoKey = "file:locations:public";
                redisTemplate.opsForZSet().remove(geoKey, token);
                log.info("已清理公开批次 RedisGEO 索引: token={}", token);
            }
            log.info("批次删除成功: uploadToken={}, 类型={}, 文件数={}",
                    token, isPrivate ? "私有" : "公开", files.size());
            Map<String, Object> data = new HashMap<>();
            data.put("deletedCount", files.size());
            return Result.success(data);
        } catch (Exception e) {
            log.error("批次删除失败: uploadToken={}", token, e);
            fileLogService.recordLog(null, "BATCH_DELETE", 0, "批次删除崩溃: " + e.getMessage(), null, null, clientIp);
            return Result.error("批次删除失败: " + e.getMessage());
        }
    }
}
