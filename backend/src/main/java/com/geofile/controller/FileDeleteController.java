package com.geofile.controller;

import com.geofile.common.Result;
import com.geofile.entity.File;
import com.geofile.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
            @Parameter(description = "上传令牌", required = true) @RequestParam String uploadToken) {

        try {
            log.info("请求删除文件: fileId={}, uploadToken={}", fileId, uploadToken);

            // 1. 查询文件
            File file = fileService.getById(fileId);
            if (file == null) {
                log.warn("文件不存在: fileId={}", fileId);
                return Result.error("文件不存在");
            }

            // 2. 验证上传令牌
            if (file.getUploadToken() == null || !file.getUploadToken().equals(uploadToken)) {
                log.warn("上传令牌验证失败: fileId={}, token={}", fileId, uploadToken);
                return Result.error("无权限删除此文件，上传令牌无效");
            }

            // 3. 检查文件是否已被删除
            if (file.getStatus() != null && file.getStatus() == 0 && file.getDeleted() == 1) {
                return Result.error("文件已被删除");
            }

            // 4. 执行软删除（更新状态为已删除）
            file.setStatus(0);
            file.setDeleted(1);
            fileService.updateById(file);

            log.info("文件删除成功: fileId={}", fileId);
            return Result.success("文件删除成功");

        } catch (Exception e) {
            log.error("删除文件失败: fileId={}", fileId, e);
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
}
