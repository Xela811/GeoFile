package com.geofile.controller;


import com.geofile.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "文件接口", description = "处理文件的上传、下载、删除")
@RestController
@RequestMapping("/file")
public class FileController {

    @Operation(summary = "单文件上传", description = "上传文件并返回临时文件ID")
    @PostMapping("/upload")
    public Result<String> upload() {
        return Result.success("上传成功");
    }
}