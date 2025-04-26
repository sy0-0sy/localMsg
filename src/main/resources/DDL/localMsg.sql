drop table if exists local_message;
CREATE TABLE `local_message` (
                                 `id` BIGINT NOT NULL AUTO_INCREMENT,
                                 `gmt_create` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
                                 `gmt_modified` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'modified time',
                                 `deleted_at` BIGINT NOT NULL DEFAULT '0' COMMENT 'deleted at',
                                 `req_snapshot` JSON NOT NULL COMMENT '请求快照参数json',
                                 `status` VARCHAR(16) NOT NULL COMMENT '状态 INIT, FAIL, SUCCESS',
                                 `next_retry_time` BIGINT NOT NULL COMMENT '下一次重试的时间',
                                 `retry_times` INT NOT NULL DEFAULT 0 COMMENT '已经重试的次数',
                                 `max_retry_times` INT NOT NULL COMMENT '最大重试次数',
                                 `fail_reason` TEXT COMMENT '执行失败的信息',
                                 `biz_code` VARCHAR(64) NOT NULL COMMENT '业务code',
                                 PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='本地消息表';