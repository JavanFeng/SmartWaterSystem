package com.javan.smart.water.common.enums;

import lombok.Getter;

/**
 * @author FengJ
 * @description 任务补充表单
 */

@Getter
public enum TaskTableCodeEnum {

    CONTAMINANT_ANALYSIS("contaminant_analysis"),

    ORDER_ASSIGN("order_assign"),

    // 仅对话
    CHAT("chat");

    private String taskType;

    TaskTableCodeEnum(String taskType) {
        this.taskType = taskType;
    }
}
