package com.zssql.common.handler;

import java.time.LocalDateTime;

/**
 * @description: 调度器
 **/
public interface Scheduler {
    /**
     * 调度方法
     * @param flowId  工作流流程ID
     * @param cron    cron表达式
     * @param scheduleTime 调度时间
     */
    void schedule(String flowId, String cron, LocalDateTime scheduleTime);
}