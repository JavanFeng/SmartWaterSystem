package com.javan.smart.water.view.model;

import com.alibaba.fastjson.annotation.JSONField;
import com.alibaba.fastjson2.JSON;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

/**
 * @author FengJ
 * @description 水质工单
 */
@Data
public class WaterOrderTaskModel {

    /**
     * 取消
     */
    private String cancel;

    /**
     * 同意协议： YES/NO
     */
    private String agreement;

    /**
     * 紧急程度 可空
     */
    @JSONField(name = "同意")
    private String urgent;
    /**
     * 处理人
     */
    @JSONField(name = "处理人")
    private String handler;

    /**
     * 处理备注 可空
     */
    @JSONField(name = "备注")
    private String remark;

}
