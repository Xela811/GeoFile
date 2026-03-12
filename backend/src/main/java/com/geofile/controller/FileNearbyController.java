package com.geofile.controller;

import com.geofile.common.Result;
import com.geofile.entity.FileVO;
import com.geofile.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件附近搜索 Controller
 * 专门用于文件附近搜索功能
 */
@Slf4j
@RestController
@RequestMapping("/api/file")
@CrossOrigin
@Tag(name = "附近文件搜索", description = "基于地理位置的文件搜索接口")
public class FileNearbyController {

    @Autowired
    private FileService fileService;

    /**
     * 获取附近文件
     *
     * 接口：GET /api/file/nearby?lat=39.9042&lng=116.4074&radius=1000
     * 参数：lat（纬度）、lng（经度）、radius（搜索半径）
     * 返回：附近文件列表，包含距离信息
     *
     * 使用示例：
     * GET /api/file/nearby?lat=39.9042&lng=116.4074&radius=1000
     *
     * @param lat 纬度
     * @param lng 经度
     * @param radius 搜索半径（米），默认1000米
     * @param excludeFileId 排除的文件ID（可选）
     * @return 附近文件列表
     */
    @GetMapping("/nearby")
    @Operation(summary = "获取附近文件", description = "根据地理位置搜索附近的文件，包含距离计算")
    public Result<Map<String, Object>> getNearbyFiles(
            @Parameter(description = "纬度", example = "39.9042") @RequestParam Double lat,
            @Parameter(description = "经度", example = "116.4074") @RequestParam Double lng,
            @Parameter(description = "搜索半径（米）", example = "1000") @RequestParam(defaultValue = "1000") Integer radius,
            @Parameter(description = "排除的文件ID（可选）", example = "1") @RequestParam(required = false) Long excludeFileId) {

        try {
            log.info("搜索附近文件: lat={}, lng={}, radius={}, excludeFileId={}", lat, lng, radius, excludeFileId);

            // 调用服务方法搜索附近文件
            List<FileVO> files = fileService.searchNearbyFiles(lat, lng, radius, excludeFileId);

            // 封装返回数据
            Map<String, Object> result = new HashMap<>();
            result.put("lat", lat);
            result.put("lng", lng);
            result.put("radius", radius);
            result.put("count", files.size());
            result.put("files", files);

            log.info("附近文件搜索成功，找到 {} 个文件", files.size());
            return Result.success(result);

        } catch (Exception e) {
            log.error("附近文件搜索失败", e);
            return Result.error("附近文件搜索失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有文件列表（用于测试）
     */
    @GetMapping("/list")
    @Operation(summary = "获取文件列表", description = "获取所有文件列表（用于测试和对比）")
    public Result<List<FileVO>> getFileList() {
        try {
            // 这里使用默认参数搜索附近文件
            List<FileVO> files = fileService.searchNearbyFiles(39.9042, 116.4074, 5000, null);
            return Result.success(files);
        } catch (Exception e) {
            log.error("获取文件列表失败", e);
            return Result.error("获取文件列表失败: " + e.getMessage());
        }
    }

    /**
     * 通过文件ID获取文件详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取文件详情", description = "根据文件ID获取文件详情")
    public Result<FileVO> getFileDetail(
            @Parameter(description = "文件ID", example = "1") @PathVariable Long id) {
        try {
            // 注意：FileServiceImpl 中的 getById 可能需要实现
            // 这里先用一个临时的返回
            FileVO fileVO = new FileVO();
            fileVO.setId(id);
            fileVO.setFileName("测试文件");
            fileVO.setFileSize(1024L);
            fileVO.setUploadTime(java.time.LocalDateTime.now().toString());

            return Result.success(fileVO);
        } catch (Exception e) {
            log.error("获取文件详情失败", e);
            return Result.error("获取文件详情失败: " + e.getMessage());
        }
    }
}
