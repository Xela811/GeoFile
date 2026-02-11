package com.geofile.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 文件分片表
 * @TableName t_file_chunk
 */
@TableName(value ="t_file_chunk")
@Data
public class FileChunk {
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 文件ID
     */
    @TableField(value = "file_id")
    private Long fileId;

    /**
     * 分片索引
     */
    @TableField(value = "chunk_index")
    private Integer chunkIndex;

    /**
     * 文件Hash
     */
    @TableField(value = "file_hash")
    private String fileHash;

    /**
     * 分片大小
     */
    @TableField(value = "chunk_size")
    private Integer chunkSize;

    /**
     * 上传时间
     */
    @TableField(value = "upload_time")
    private Date uploadTime;
}