package com.geofile.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.geofile.entity.DownloadLimit;
import com.geofile.entity.File;
import com.geofile.entity.FileVO;
import com.geofile.mapper.FileMapper;
import com.geofile.service.DownloadLimitService;
import com.geofile.service.FileLocationService;
import com.geofile.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 文件地理位置服务实现
 */
@Slf4j
@Service
public class FileLocationServiceImpl extends ServiceImpl<FileMapper,File> implements FileLocationService {

    @Autowired
    private FileService fileService;

    @Autowired
    private DownloadLimitService downloadLimitService;

    /**
     * 更新文件地理位置信息
     */
    @Override
    public void updateFileLocation(Long fileId, Double lat, Double lng, Integer radius) {
        File file = fileService.getById(fileId);
        if (file == null) {
            throw new RuntimeException("文件不存在: " + fileId);
        }

        file.setLocationLat(lat);
        file.setLocationLng(lng);
        file.setLocationRadius(radius);
        fileService.updateById(file);

        log.info("更新文件地理位置: fileId={}, lat={}, lng={}, radius={}", fileId, lat, lng, radius);
    }

    /**
     * 计算两点之间的距离（米）
     * 使用Haversine公式
     */
    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
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

        // 计算距离（如果提供了用户位置）
        // 这个方法在调用时会被覆盖，所以这里返回null
        vo.setDistance(null);

        // 设置下载次数上限
        if (file.getDownloadLimitId() != null) {
            DownloadLimit downloadLimit = downloadLimitService.getById(file.getDownloadLimitId());
            if (downloadLimit != null) {
                vo.setMaxDownloads(downloadLimit.getMaxDownloads());
            }
        }

        return vo;
    }

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
                return Arrays.asList("png", "jpg", "jpeg", "gif", "bmp", "webp", "svg", "ico", "tiff", "tif");
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
            default:
                return new ArrayList<>();
        }
    }
}
