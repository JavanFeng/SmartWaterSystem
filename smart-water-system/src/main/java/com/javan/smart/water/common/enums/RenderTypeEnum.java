package com.javan.smart.water.common.enums;

import lombok.Getter;

/**
 * @author FengJ
 * @description 渲染类型
 */
@Getter
public enum RenderTypeEnum {

    TABLE("table"),
    CARD("card"),
    TEXT("text");

    private String type;

    RenderTypeEnum(String type) {
        this.type = type;
    }
}
