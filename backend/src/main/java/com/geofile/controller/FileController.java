//package com.geofile.controller;
//
//
//import com.geofile.common.Result;
//import com.geofile.service.FileService;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.*;
//
///**
// * 文件管理Controller
// * 处理文件的上传、下载、删除
// */
//@Tag(name = "文件接口", description = "处理文件的上传、下载、删除")
//@RestController
//@RequestMapping("/api/file")
//@CrossOrigin
//public class FileController {
//
//    @Autowired
//    private FileService fileService;
//
//    /**
//     * 获取文件列表
//     */
//    @Operation(summary = "获取文件列表", description = "分页查询文件列表")
//    @GetMapping("/list")
//    public Result<?> fileList() {
//        return Result.success("文件列表");
//    }
//
//    /**
//     * 获取文件详情
//     */
//    @Operation(summary = "获取文件详情", description = "根据文件ID获取文件详情")
//    @GetMapping("/detail/{id}")
//    public Result<?> fileDetail(@PathVariable Long id) {
//        return Result.success("文件详情");
//    }
//
//    /**
//     * 文件上传
//     */
//    @Operation(summary = "文件上传", description = "上传文件并返回文件信息")
//    @PostMapping("/upload")
//    public Result<?> upload() {
//        return Result.success("上传成功");
//    }
//
//    /**
//     * 文件下载
//     */
//    @Operation(summary = "文件下载", description = "下载指定文件")
//    @GetMapping("/download/{id}")
//    public Result<?> download(@PathVariable Long id) {
//        return Result.success("下载成功");
//    }
//
//    /**
//     * 文件删除
//     */
//    @Operation(summary = "文件删除", description = "删除指定文件")
//    @DeleteMapping("/{id}")
//    public Result<?> delete(@PathVariable Long id) {
//        return Result.success("删除成功");
//    }
//
//    /**
//     * 获取附近文件
//     */
//    @Operation(summary = "获取附近文件", description = "根据地理位置获取附近文件")
//    @GetMapping("/nearby")
//    public Result<?> nearby(
//            @RequestParam Double lat,
//            @RequestParam Double lng,
//            @RequestParam(defaultValue = "1000") Integer radius) {
//        return Result.success("附近文件");
//    }
//}