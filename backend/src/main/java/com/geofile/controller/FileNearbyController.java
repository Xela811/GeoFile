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
     * 获取附近文件（支持分页）
     *
     * 接口：GET /api/file/nearby?lat=39.9042&lng=116.4074&radius=1000&pageNum=1&pageSize=10
     * 参数：
     * - lat: 纬度
     * - lng: 经度
     * - radius: 搜索半径（米），默认1000米
     * - pageNum: 当前页码，默认1
     * - pageSize: 每页大小，默认10
     * - excludeFileId: 排除的文件ID（可选）
     * - sortBy: 排序字段（可选）
     * - sortOrder: 排序方向（可选，默认DESC）
     * - keyword: 搜索关键词（可选）
     * - fileType: 文件类型（可选）
     *
     * 使用示例：
     * GET /api/file/nearby?lat=39.9042&lng=116.4074&radius=1000&pageNum=1&pageSize=10
     * GET /api/file/nearby?lat=39.9042&lng=116.4074&radius=1000&pageNum=1&pageSize=10&sortBy=upload_time&fileType=pdf
     *
     * @param lat 纬度
     * @param lng 经度
     * @param radius 搜索半径（米），默认1000米
     * @param pageNum 当前页码，默认1
     * @param pageSize 每页大小，默认10
     * @param excludeFileId 排除的文件ID（可选）
     * @param sortBy 排序字段（可选）
     * @param sortOrder 排序方向（可选）
     * @param keyword 搜索关键词（可选）
     * @param fileType 文件类型（可选）
     * @param extractCode 取件码（可选）
     * @return 附近文件列表（包含分页信息和距离）
     */
    @GetMapping("/nearby")
    @Operation(summary = "获取附近文件", description = "根据地理位置搜索附近的文件，支持分页查询（包含距离计算）")
    public Result<Map<String, Object>> getNearbyFiles(
            @Parameter(description = "纬度", example = "39.9042") @RequestParam Double lat,
            @Parameter(description = "经度", example = "116.4074") @RequestParam Double lng,
            @Parameter(description = "搜索半径（米）", example = "1000") @RequestParam(defaultValue = "1000") Integer radius,
            @Parameter(description = "当前页码", example = "1") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小", example = "10") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "排除的文件ID（可选）", example = "1") @RequestParam(required = false) Long excludeFileId,
            @Parameter(description = "排序字段（可选）", example = "upload_time") @RequestParam(required = false) String sortBy,
            @Parameter(description = "排序方向（可选）", example = "DESC") @RequestParam(required = false) String sortOrder,
            @Parameter(description = "搜索关键词（可选）") @RequestParam(required = false) String keyword,
            @Parameter(description = "文件类型（可选）") @RequestParam(required = false) String fileType,
            @Parameter(description = "取件码（可选）") @RequestParam(required = false) String extractCode) {

//        try {
            log.info("搜索附近文件: lat={}, lng={}, radius={}, pageNum={}, pageSize={}, keyword={}, fileType={}, extractCode={}",
                    lat, lng, radius, pageNum, pageSize, keyword, fileType, extractCode);

            // 调用服务方法搜索附近文件（使用MyBatis-Plus分页）
            List<FileVO> files = fileService.searchNearbyFiles(lat, lng, radius, excludeFileId, pageNum, pageSize, sortBy, sortOrder, keyword, fileType, extractCode);

            // 计算总记录数
            Long total = fileService.countNearbyFiles(lat, lng, radius, keyword, fileType, extractCode);

            // 计算总页数
            int totalPages = (int) ((total + pageSize - 1) / pageSize);
            boolean hasPrevious = pageNum > 1;
            boolean hasNext = pageNum < totalPages || (totalPages == 0 && total > 0);

            // 封装返回数据
            Map<String, Object> result = new HashMap<>();
            result.put("lat", lat);
            result.put("lng", lng);
            result.put("radius", radius);
            result.put("pageNum", pageNum);
            result.put("pageSize", pageSize);
            result.put("total", total);
            result.put("totalPages", totalPages);
            result.put("hasPrevious", hasPrevious);
            result.put("hasNext", hasNext);
            result.put("count", files.size());
            result.put("files", files);

            log.info("附近文件搜索成功，找到 {} 个文件", files.size());
            return Result.success(result);

    }
}
