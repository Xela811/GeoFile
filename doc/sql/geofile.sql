create table t_file
(
    id              bigint auto_increment comment '主键ID'
        primary key,
    file_name       varchar(255)                          not null comment '文件名',
    file_type       varchar(50)                           null comment '文件类型',
    file_size       bigint                                null comment '文件大小(字节)',
    file_hash       varchar(64)                           null comment '文件SHA-256哈希值',
    file_path       varchar(500)                          null comment '文件存储路径',
    original_name   varchar(255)                          null comment '原始文件名',
    storage_type    varchar(20) default 'OSS'             null comment '存储类型',
    upload_time     datetime    default CURRENT_TIMESTAMP null comment '上传时间',
    expire_time     datetime                              null comment '有效截止时间',
    download_count  int         default 0                 null comment '下载次数',
    status          tinyint     default 1                 null comment '状态 0-删除 1-正常 2-过期 3-满额 4-下架',
    location_lat    double                                null comment '地理位置纬度',
    location_lng    double                                null comment '地理位置经度',
    location_radius int         default 1000              null comment '地理位置半径(米)',
    region_id       varchar(50)                           null comment '区域ID',
    created_by      bigint                                null comment '创建人',
    created_time    datetime    default CURRENT_TIMESTAMP null,
    updated_by      bigint                                null comment '更新人',
    updated_time    datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    deleted         tinyint     default 0                 null comment '逻辑删除',
    upload_token    varchar(100)                          null comment '上传令牌（用于免登录身份验证）',
    download_token  varchar(100)                          null comment '下载令牌（用于下载验证）',
    is_private      tinyint(1)  default 1                 null comment '是否为私有文件：1-私有(需取件码)，0-公开(直接下载)'
)
    comment '文件信息表' collate = utf8mb4_0900_ai_ci;

create table t_download_limit
(
    id            bigint auto_increment comment '主键ID'
        primary key,
    file_id       bigint         not null comment '文件ID',
    max_downloads int default 5  null comment '最大下载次数',
    valid_minutes int default 24 null comment '有效时长(分钟)',
    constraint uk_file_id
        unique (file_id),
    constraint t_download_limit_ibfk_1
        foreign key (file_id) references t_file (id)
            on delete cascade
)
    comment '下载限制配置表' collate = utf8mb4_0900_ai_ci;

create index idx_expire_time
    on t_file (expire_time);

create index idx_file_hash
    on t_file (file_hash);

create index idx_is_private
    on t_file (is_private);

create index idx_location
    on t_file (location_lat, location_lng, location_radius);

create index idx_token_status_time
    on t_file (upload_token, status, upload_time);

create index idx_upload_time
    on t_file (upload_time);

create index idx_upload_token
    on t_file (upload_token);

create table t_file_batch
(
    id           bigint auto_increment comment '主键ID'
        primary key,
    batch_token  varchar(100)                         not null comment '对应 t_file 中的 upload_token',
    extract_code varchar(20)                          null comment '取件码(如果有)',
    client_ip    varchar(50)                          null comment '上传者真实IP',
    file_count   int        default 0                 null comment '批次内文件总数',
    total_size   bigint     default 0                 null comment '批次总大小(字节)',
    is_private   tinyint(1) default 1                 null comment '是否私有：1-私有，0-公开',
    expire_time  datetime                             not null comment '失效时间(过期后取件码失效)',
    created_time datetime   default CURRENT_TIMESTAMP null comment '上传时间',
    constraint uk_batch_token
        unique (batch_token)
)
    comment '文件上传批次增强索引表' collate = utf8mb4_0900_ai_ci;

create index idx_client_ip
    on t_file_batch (client_ip);

create index idx_expire_time
    on t_file_batch (expire_time);

create index idx_extract_code
    on t_file_batch (extract_code);

create table t_file_hash
(
    id              bigint unsigned auto_increment
        primary key,
    file_hash       char(64)                               not null comment '物理文件唯一标识(SHA-256值)',
    md5             char(32)                               null comment '前端快速校验值(MD5值)',
    file_size       bigint unsigned                        not null comment '物理文件字节大小',
    storage_path    varchar(512)                           not null comment '磁盘物理相对路径',
    storage_type    varchar(20)  default 'local'           null comment '存储类型: local, oss, minio',
    reference_count int unsigned default '1'               null comment '逻辑引用计数(为0时可物理删除)',
    status          tinyint      default 1                 null comment '物理文件状态: 1-可用, 0-上传中/已损坏',
    mime_type       varchar(128)                           null comment '真实媒体类型',
    extension       varchar(20)                            null comment '文件原始后缀',
    created_time    datetime     default CURRENT_TIMESTAMP null,
    updated_time    datetime     default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    sample_hash     varchar(64)                            null,
    constraint uk_file_hash
        unique (file_hash)
)
    comment '文件物理存储关联表' collate = utf8mb4_0900_ai_ci;

create index idx_md5
    on t_file_hash (md5);

create index idx_size_sample
    on t_file_hash (file_size, sample_hash);

create table t_file_log
(
    id          bigint auto_increment comment '主键ID'
        primary key,
    file_id     bigint                             null comment '关联的文件ID（单文件操作时记录）',
    action_type varchar(30)                        not null comment '操作类型：UPLOAD(上传), DOWNLOAD(下载), PREVIEW(预览), SEC_UPLOAD(秒传)',
    status      int      default 1                 not null comment '状态：1 成功, 0 失败',
    error_msg   varchar(500)                       null comment '失败时的异常信息原因',
    ip_address  varchar(50)                        null comment '操作者IP',
    lat         double                             null comment '请求时携带的纬度',
    lng         double                             null comment '请求时携带的经度',
    create_time datetime default CURRENT_TIMESTAMP not null comment '触发时间'
)
    comment '文件操作日志表' collate = utf8mb4_0900_ai_ci;

create index idx_create_time
    on t_file_log (create_time);

create index idx_file_id
    on t_file_log (file_id);


