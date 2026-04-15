package com.geofile.entity;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class ArchiveNode {
    private String name;          // 文件或目录名
    private boolean isDirectory;  // 是否为目录
    private long size;            // 文件大小（字节）
    private List<ArchiveNode> children = new ArrayList<>(); // 子节点

    public ArchiveNode(String name, boolean isDirectory, long size) {
        this.name = name;
        this.isDirectory = isDirectory;
        this.size = size;
    }
}
