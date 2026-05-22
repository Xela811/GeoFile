package com.geofile.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.geofile.entity.*;
import com.geofile.service.FileBatchService;
import com.geofile.service.FileHashService;
import com.geofile.service.FileService;
import com.geofile.service.DownloadLimitService;
import com.geofile.mapper.FileMapper;
import com.geofile.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.domain.geo.Metrics;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
* @author xela
* @description 针对表【t_file(文件信息表)】的数据库操作Service实现
* @createDate 2026-02-10 23:30:13
*/
@Slf4j
@Service
public class FileServiceImpl extends ServiceImpl<FileMapper, File>
    implements FileService {

    @Autowired
    private DownloadLimitService downloadLimitService;

    @Autowired
    private RedisUtil redisUtil; // 1. 注入实例

    @Autowired
    private FileBatchService fileBatchService;

    @Autowired
    private FileHashService fileHashService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public List<FileVO> searchNearbyFiles(Double lat, Double lng, Integer radius, Long excludeFileId,
                                         Integer pageNum, Integer pageSize,
                                         String sortBy, String sortOrder, String keyword, String fileType, String extractCode) {
        try {
            Page<File> page = new Page<>(pageNum, pageSize);
            // 排序逻辑保持你原来的部分...
            if (sortBy != null && !sortBy.isEmpty()) {
                boolean isAsc = "ASC".equalsIgnoreCase(sortOrder);
                page.addOrder(isAsc ? OrderItem.asc(sortBy) : OrderItem.desc(sortBy));
            } else {
                page.addOrder(OrderItem.desc("upload_time"));
            }

            LambdaQueryWrapper<File> queryWrapper = new LambdaQueryWrapper<>();

            // --- 核心逻辑：区分有码和无码模式 ---
            if (StringUtils.hasText(extractCode)) {
                // 【有码模式】：忽视距离限制
                //String batchToken = (String) redisUtil.get("code:to:token:" + extractCode);
                String batchToken = getBatchTokenByCode(extractCode);
                if (StringUtils.hasText(batchToken)) {
                    // 只查这个取件码对应的文件包
                    queryWrapper.eq(File::getUploadToken, batchToken);
                    log.info("提取码模式：忽视距离限制，匹配Token: {}", batchToken);
                } else {
                    // 提取码无效
                    throw new IllegalArgumentException("取件码已过期或不存在");
                }
            } else {
                // 【无码模式】：执行1km周边过滤
//                double latDelta = radius / 111000.0;
//                double lngDelta = radius / (111000.0 * Math.cos(lat * Math.PI / 180.0));
//                queryWrapper.between(File::getLocationLat, lat - latDelta, lat + latDelta)
//                        .between(File::getLocationLng, lng - lngDelta, lng + lngDelta)
//                        .eq(File::getIsPrivate, 0) // 没码只能看公开
//                        .isNotNull(File::getLocationLat)
//                        .isNotNull(File::getLocationLng);
                // 【无码模式】：核心修改 -> 使用 Redis 搜索附近 Token
                String geoKey = "file:locations:public";
                // 搜索半径内所有 Member (即 uploadToken)
                Circle circle = new Circle(new Point(lng, lat), new Distance(radius, Metrics.METERS));
                GeoResults<RedisGeoCommands.GeoLocation<Object>> geoResults = redisTemplate.opsForGeo().radius(geoKey, circle);

                if (geoResults == null || geoResults.getContent().isEmpty()) {
                    return new ArrayList<>(); // 附近没东西，直接返回
                }

                // 提取所有在范围内的 Token
                List<String> nearbyTokens = geoResults.getContent().stream()
                        .map(res -> res.getContent().getName().toString())
                        .collect(Collectors.toList());
                log.info("Redis搜索结果：找到 {} 个候选 Token: {}", nearbyTokens.size(), nearbyTokens);

                // 将这些 Token 作为数据库查询条件
                queryWrapper.in(File::getUploadToken, nearbyTokens);
                queryWrapper.eq(File::getIsPrivate, 0); // 无码只能看公开
            }

            // --- 公共过滤逻辑（关键词、类型、状态） ---
            queryWrapper.in(File::getStatus, Arrays.asList(1, 3));
            if (excludeFileId != null) queryWrapper.ne(File::getId, excludeFileId);

            // 任务四：支持对私有列表的关键词筛选
            if (StringUtils.hasText(keyword)) {
                queryWrapper.like(File::getFileName, keyword);
            }

            // 文件类型过滤逻辑（保持你原来的 getFileExtensionsByType 逻辑）
            if (StringUtils.hasText(fileType)) {
                if ("other".equalsIgnoreCase(fileType)) {
                    queryWrapper.notIn(File::getFileType, KNOWN_EXTENSIONS);
                } else {
                    List<String> extensions = getFileExtensionsByType(fileType);
                    if (!extensions.isEmpty()) queryWrapper.in(File::getFileType, extensions);
                    else queryWrapper.eq(File::getFileType, fileType);
                }
            }

            List<File> files = list(page, queryWrapper);

            // 计算距离并封装 VO
            List<FileVO> result = new ArrayList<>();
            for (File file : files) {
                FileVO vo = convertToFileVO(file);
                // 无论哪种模式，如果文件有经纬度，都顺手算一下距离显示出来
                if (file.getLocationLat() != null && file.getLocationLng() != null) {
                    double distance = calculateDistance(lat, lng, file.getLocationLat(), file.getLocationLng());
                    vo.setDistance(distance);
                }
                result.add(vo);
            }
            return result;
        } catch (Exception e) {
            log.error("搜索附近文件失败", e);
            return new ArrayList<>();
        }
    }

    @Override
    public Long countNearbyFiles(Double lat, Double lng, Integer radius, String keyword, String fileType, String extractCode) {
        try {
            LambdaQueryWrapper<File> queryWrapper = new LambdaQueryWrapper<>();

            if (StringUtils.hasText(extractCode)) {
                // 有码模式
                //String batchToken = (String) redisUtil.get("code:to:token:" + extractCode);
                String batchToken = getBatchTokenByCode(extractCode);
                if (!StringUtils.hasText(batchToken)) {throw new IllegalArgumentException("取件码已过期或不存在");};
                queryWrapper.eq(File::getUploadToken, batchToken);
            } else {
                // 【无码模式】：核心修改 -> 使用 Redis 获取附近的 Token 集合
                String geoKey = "file:locations:public";
                // 1. 定义搜索范围（圆形）
                Circle circle = new Circle(new Point(lng, lat), new Distance(radius, Metrics.METERS));
                // 2. 从 Redis 中查出所有在范围内的 uploadToken
                GeoResults<RedisGeoCommands.GeoLocation<Object>> geoResults = redisTemplate.opsForGeo().radius(geoKey, circle);

                if (geoResults == null || geoResults.getContent().isEmpty()) {
                    return 0L; // 附近没有任何 Token，数量直接为 0
                }

                // 3. 提取 Token 列表
                List<String> nearbyTokens = geoResults.getContent().stream()
                        .map(res -> res.getContent().getName().toString())
                        .collect(Collectors.toList());

                // 4. 将 Token 集合作为 MySQL 的 IN 条件
                queryWrapper.in(File::getUploadToken, nearbyTokens);
                queryWrapper.eq(File::getIsPrivate, 0); // 必须是公开文件
            }

            // 公共过滤
            queryWrapper.in(File::getStatus, Arrays.asList(1, 3));
            if (StringUtils.hasText(keyword)) queryWrapper.like(File::getFileName, keyword);
            if (StringUtils.hasText(fileType)) {
                List<String> extensions = getFileExtensionsByType(fileType);
                if (!extensions.isEmpty()) queryWrapper.in(File::getFileType, extensions);
                else queryWrapper.eq(File::getFileType, fileType);
            }

            return count(queryWrapper);
        } catch (Exception e) {
            log.error("统计附近文件数量失败", e);
            return 0L;
        }
    }

    /**
     * 根据取件码获取 BatchToken，具备数据库回源能力
     */
    private String getBatchTokenByCode(String extractCode) {
        if (!StringUtils.hasText(extractCode)) return null;

        String redisKey = "code:to:token:" + extractCode;
        String batchToken = (String) redisUtil.get(redisKey);

        // 如果 Redis 查不到，尝试从 t_file_batch 回源
        if (!StringUtils.hasText(batchToken)) {
            FileBatch batch = fileBatchService.getOne(new LambdaQueryWrapper<FileBatch>()
                    .eq(FileBatch::getExtractCode, extractCode)
                    .gt(FileBatch::getExpireTime, LocalDateTime.now())); // 必须未过期

            if (batch != null) {
                batchToken = batch.getBatchToken();
                // 计算剩余有效期并回填 Redis
                long remainSeconds = Duration.between(LocalDateTime.now(), batch.getExpireTime()).getSeconds();
                if (remainSeconds > 0) {
                    redisUtil.set("code:to:token:" + extractCode, batchToken, remainSeconds, TimeUnit.SECONDS);
                    redisUtil.set(redisKey, batchToken, remainSeconds, TimeUnit.SECONDS);
                    redisUtil.set("file:download:" + batchToken, extractCode, remainSeconds, TimeUnit.SECONDS);
                }
                log.info("提取码 {} Redis 失效，已从数据库回源成功", extractCode);
            }
        }
        return batchToken;
    }

    @Override
    public void updateFileLocation(Long fileId, Double lat, Double lng, Integer radius) {
        try {
            File file = getById(fileId);
            if (file == null) {
                throw new RuntimeException("文件不存在: " + fileId);
            }

            file.setLocationLat(lat);
            file.setLocationLng(lng);
            file.setLocationRadius(radius);
            updateById(file);

            log.info("更新文件地理位置: fileId={}, lat={}, lng={}, radius={}", fileId, lat, lng, radius);
        } catch (Exception e) {
            log.error("更新文件地理位置失败", e);
            throw e;
        }
    }

    /**
     * 计算两点之间的距离（米）
     * 使用Haversine公式
     */
    public double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        final int EARTH_RADIUS = 6371000; // 地球半径(米)

        double latDistance = Math.toRadians(lat2 - lat1);
        double lngDistance = Math.toRadians(lng2 - lng1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS * c;
    }

    /**
     * 转换为FileVO
     */
    private FileVO convertToFileVO(File file) {
        FileVO vo = new FileVO();
        BeanUtils.copyProperties(file, vo);
        //vo.setUploadTime(file.getUploadTime().toString());
        //vo.setExpireTime(file.getExpireTime().toString());
        // 定义中文格式：年-月-日 时:分
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        // 处理上传时间
        if (file.getUploadTime() != null) {
            vo.setUploadTime(sdf.format(file.getUploadTime()));
        }

        // 处理过期时间
        if (file.getExpireTime() != null) {
            vo.setExpireTime(sdf.format(file.getExpireTime()));
        } else {
            vo.setExpireTime(null);
        }

        // 确保下载次数被赋值（如果 BeanUtils 没拷过去的话）
        vo.setDownloadCount(file.getDownloadCount() != null ? file.getDownloadCount() : 0);

        // 设置下载次数上限
        DownloadLimit downloadLimit = downloadLimitService.getOne(
                new LambdaQueryWrapper<DownloadLimit>().eq(DownloadLimit::getFileId, file.getId())
        );

        if (downloadLimit != null) {
            // 填充最大下载次数
            vo.setMaxDownloads(downloadLimit.getMaxDownloads());
            // 如果需要显示有效小时，也可以在这里填充（前提是 FileVO 有这个字段）
        } else {
            vo.setMaxDownloads(0); // 数据库没记录，显式设为 0 表示不限制
        }
        vo.setStatusText(file.getStatus() == 1 ? "正常" : "已删除");
        return vo;
    }

    private static final List<String> KNOWN_EXTENSIONS = Arrays.asList(
            "png", "jpg", "jpeg", "gif", "bmp", "webp", "svg", "ico", "tiff", "tif","heic","m4a",
            "mp4", "avi", "mov", "wmv", "flv", "mkv", "webm", "m4v", "3gp", "mpeg", "mpg",
            "mp3", "wav", "flac", "aac", "ogg", "wma", "m4a", "ape", "amr",
            "pdf",
            "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "rtf", "odt", "ods", "odp", "md",
            "zip", "rar", "7z", "tar", "gz", "bz2", "xz"
    );

    /**
     * 根据文件类型分类获取对应的文件扩展名列表
     * @param fileType 文件类型分类（image, video, audio, pdf, document, zip, other）
     * @return 对应的扩展名列表
     */
    private List<String> getFileExtensionsByType(String fileType) {
        if (fileType == null || fileType.isEmpty()) {
            return new ArrayList<>();
        }

        switch (fileType.toLowerCase()) {
            case "image":
                return Arrays.asList("png", "jpg", "jpeg", "gif", "bmp", "webp", "svg", "ico", "tiff", "tif","heic","m4a");
            case "video":
                return Arrays.asList("mp4", "avi", "mov", "wmv", "flv", "mkv", "webm", "m4v", "3gp", "mpeg", "mpg");
            case "audio":
                return Arrays.asList("mp3", "wav", "flac", "aac", "ogg", "wma", "m4a", "ape", "amr");
            case "pdf":
                return Arrays.asList("pdf");
            case "document":
                return Arrays.asList("doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "rtf", "odt", "ods", "odp", "md");
            case "zip":
                return Arrays.asList("zip", "rar", "7z", "tar", "gz", "bz2", "xz");
            case "other":
                return KNOWN_EXTENSIONS;
            default:
                return new ArrayList<>();
        }
    }


    // 在 FileServiceImpl 中抽取出的公共逻辑
    @Override
    public File processAccess(Long fileId, String downloadToken) {
        // 1. 查询并校验 (直接复用你提供的逻辑)
        File file = this.getById(fileId);
        if (file == null) throw new IllegalArgumentException("文件不存在");

        // 校验物理关联 (新增对 MD5/SHA256 的兼容检查)
        if (file.getFileHash() != null) {
            // 这里的 fileHashService 应该能根据 SHA256 查到物理记录
            FileHash hashRecord = fileHashService.findByHash(file.getFileHash());
            if (hashRecord == null || hashRecord.getStatus() == 0) {
                log.error("物理资源已失效: hash={}", file.getFileHash());
                throw new IllegalArgumentException("该文件已被系统物理清理");
            }
        }

        // 2. 验证下载令牌
        if (file.getDownloadToken() == null || !file.getDownloadToken().equals(downloadToken)) {
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

        // 4. 执行计数逻辑
        DownloadLimit downloadLimit = downloadLimitService.getOne(
                new LambdaQueryWrapper<DownloadLimit>().eq(DownloadLimit::getFileId, fileId)
        );
        int currentCount = (file.getDownloadCount() == null) ? 0 : file.getDownloadCount();
        int maxDownloads = (downloadLimit != null) ? downloadLimit.getMaxDownloads() : 0;

        int nextCount = currentCount + 1;
        file.setDownloadCount(nextCount);

        if (maxDownloads > 0 && nextCount >= maxDownloads) {
            file.setStatus(3); // 满额转残影状态
        }

        // 5. 保存并返回
        this.updateById(file);
        return file;
    }
}




