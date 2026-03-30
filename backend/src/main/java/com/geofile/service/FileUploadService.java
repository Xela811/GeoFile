package com.geofile.service;

import com.geofile.entity.File;
import com.geofile.entity.FileVO;
import com.geofile.entity.UploadInfo;
import com.geofile.entity.UploadProgress;
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
    FileVO uploadFile(MultipartFile file, Integer maxDownloads, Integer validMinutes);

    @Transactional
    FileVO uploadFile(MultipartFile file, Double lat, Double lng, Integer radius,
                      Integer maxDownloads, Integer validMinutes);

    List<FileVO> uploadFilesWithLocation(List<MultipartFile> files, Double lat, Double lng, Integer radius, Integer maxDownloads, Integer validMinutes);
    /**
     * 上传多个文件
     * @param files 上传的文件列表
     * @return 文件信息列表
     */
    List<FileVO> uploadFiles(List<MultipartFile> files);

    /**
     * 分片上传初始化
     * @param fileName 文件名
     * @param fileSize 文件大小
     * @param chunkSize 分片大小
     * @param chunkIndex 当前分片索引
     * @return 上传信息
     */
    UploadInfo initChunkUpload(String fileName, long fileSize, int chunkSize, int chunkIndex);

    /**
     * 分片上传
     * @param chunk 分片文件
     * @param fileName 文件名
     * @param chunkIndex 分片索引
     * @param totalChunks 总分片数
     * @param fileHash 文件哈希值（用于秒传）
     * @return 上传进度
     */
    UploadProgress uploadChunk(MultipartFile chunk, String fileName, int chunkIndex, int totalChunks, String fileHash);

    /**
     * 合并分片
     * @param fileName 文件名
     * @param totalChunks 总分片数
     * @param fileHash 文件哈希值
     * @return 合并后的文件信息
     */
    FileVO mergeChunks(String fileName, int totalChunks, String fileHash);

    /**
     * 下载文件
     * @param fileId 文件ID
     * @param downloadToken 下载令牌
     * @return 文件信息
     */
    File downloadFile(Long fileId, String downloadToken);

    /**
     * 生成下载令牌
     * @param fileId 文件ID
     * @return 下载令牌
     */
    String generateDownloadToken(Long fileId);
}
