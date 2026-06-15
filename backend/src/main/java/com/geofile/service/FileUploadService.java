package com.geofile.service;

import com.geofile.dto.SecUploadDTO;
import com.geofile.entity.File;
import com.geofile.entity.FileVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文件上传服务接口
 */
public interface FileUploadService {

    /**
     * 上传单个文件
     * @param file 上传的文件
     * @param maxDownloads 最大下载次数
     * @param validMinutes 有效时长（分钟）
     * @return 文件信息
     */
    FileVO uploadFile(MultipartFile file, Integer maxDownloads, Integer validMinutes, Boolean needCode);

    @Transactional
    FileVO uploadFile(MultipartFile file, Double lat, Double lng, Integer radius,
                      Integer maxDownloads, Integer validMinutes, Boolean needCode, String providedToken, String sampleHash, String fullHash);

    List<FileVO> uploadFilesWithLocation(List<MultipartFile> files, Double lat, Double lng, Integer radius, Integer maxDownloads, Integer validMinutes, Boolean needCode, String providedToken, List<String> sampleHashes, List<String> fullHashes, HttpServletRequest request);
    /**
     * 上传多个文件
     * @param files 上传的文件列表
     * @return 文件信息列表
     */
    List<FileVO> uploadFiles(List<MultipartFile> files);

    /**
     * 下载文件
     * @param fileId 文件ID
     * @param downloadToken 下载令牌
     * @return 文件信息
     */
    File downloadFile(Long fileId, String downloadToken, Double lat, Double lng, boolean shouldCount);

    /**
     * 生成下载令牌
     * @param fileId 文件ID
     * @return 下载令牌
     */
    String generateDownloadToken(Long fileId);

    List<FileVO> verifyAndGetFiles(String code);

    List<FileVO> getFilesByUploadToken(String uploadToken);

    @Transactional
    FileVO secUpload(SecUploadDTO dto, HttpServletRequest request);

    FileVO convertToFileVO(File file); // 添加这一行
}
