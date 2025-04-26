package com.sy.localMsg.repo;


import com.sy.localMsg.taskStatus.TaskStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class LocalMessagePO {
    private Long id;

    private LocalDateTime gmtCreate;

    private LocalDateTime gmtModified;

    private Long deletedAt;

    private String extraData;
    /**
     *  请求参数 快照 JSON格式
     */
    private String reqSnapshot;
    /**
     * 状态 INIT FAIL SUCCESS
     */
    private String status;
    /**
     * 下次重试时间
     */
    private Long nextRetryTime;
    /**
     * 已经重试的次数
     */
    private Integer retryTimes;
    /**
     * 最大重试次数
     */
    private Integer maxRetryTimes;
    /**
     * 失败原因
     */
    private String failReason;
    /**
     * 业务Code
     */
    private String bizCode;

    public LocalMessagePO(String bizCode, String reqSnapshot, Integer maxRetryTimes, Long nextRetryTime) {
        this.status = TaskStatus.INIT.name();
        this.bizCode = bizCode;
        this.reqSnapshot = reqSnapshot;
        this.maxRetryTimes = maxRetryTimes;
        this.nextRetryTime = nextRetryTime;
        this.retryTimes = 0;
    }

}