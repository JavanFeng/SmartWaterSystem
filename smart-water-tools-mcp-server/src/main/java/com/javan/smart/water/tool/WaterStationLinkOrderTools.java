package com.javan.smart.water.tool;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;
import com.javan.smart.water.common.McpPermission;
import com.javan.smart.water.common.base.DT;
import com.javan.smart.water.config.IMcpTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author FengJ
 * @description 站点相关的处理工单
 */
@Component
public class WaterStationLinkOrderTools implements IMcpTool {

    private static final Logger logger = LoggerFactory.getLogger(WaterStationLinkOrderTools.class);

    @Tool(description = "查询站点近期12个月内相关水质污染物异常的处理工单")
    @McpPermission
    public String getWaterStationLinkOrder(
            @ToolParam(description = "站点名称，如 HZ-XS-R01-01") String stcd) {
        logger.info("MCP getWaterSectionData: stcd={}", stcd);
        List<WaterHandlerOrder> handlerOrders = List.of(
                // 场景1：暴雨过后，地表径流带来的面源污染
                new WaterHandlerOrder(
                        "2026-06-01",
                        "6",
                        "总磷、氨氮",
                        "总磷:0.28mg/L, 氨氮:1.8mg/L, 浊度:85NTU",
                        "连续强降雨导致周边农田与地表初期雨水径流汇入",
                        "已加密监测频次，待水体自然稀释降解，预计2天后恢复"
                ),

                // 场景2：晴热天气，水体富营养化引发藻类异常
                new WaterHandlerOrder(
                        "2026-05-28",
                        "6",
                        "溶解氧、叶绿素a",
                        "溶解氧:13.5mg/L(过饱和), 叶绿素a:25ug/L, pH:9.1",
                        "近期持续晴热高温，光照强烈导致局部水域藻类光合作用旺盛",
                        "已启动生态调水增加水体流动性，并安排人工打捞藻类"
                ),

                // 场景3：夜间突发，企业偷排导致的高浓度污染
                new WaterHandlerOrder(
                        "2026-05-20",
                        "6",
                        "高锰酸盐指数、氨氮",
                        "高锰酸盐指数:12.5mg/L, 氨氮:4.2mg/L, 电导率:异常升高",
                        "上游某工业园区夜间存在隐蔽排口异常偷排",
                        "已联动环境执法大队溯源查处，责令涉事企业停产整改"
                )
        );

        return JSON.toJSONString(DT.success(handlerOrders));

    }

    public record WaterHandlerOrder(@JSONField(name = "日期") String day,

                                    @JSONField(name = "小时") String hour,
                                    @JSONField(name = "主要污染物") String mainContaminant,
                                    @JSONField(name = "水质数据") String data,
                                    @JSONField(name = "最终原因") String specialOri,
                                    @JSONField(name = "备注信息") String handleRemark) {
    }
}
