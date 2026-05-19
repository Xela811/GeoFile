package com.geofile.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_file_log")
@Schema(description = "文件操作日志")
public class FileLog implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long fileId;

    private String actionType; // UPLOAD, DOWNLOAD, PREVIEW, SEC_UPLOAD

    private Integer status; // 1-成功, 0-失败

    private String errorMsg;

    private String ipAddress;

    private Double lat;

    private Double lng;

    private Date createTime;
}
