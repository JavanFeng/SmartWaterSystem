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
 * 上下游以及断面数据
 *
 * @author Javan
 * @since 1.0.0
 */
@Component
public class WaterSectionLinkDataTools  implements IMcpTool {

    private static final Logger logger = LoggerFactory.getLogger(WaterSectionLinkDataTools.class);

    @McpPermission
    @Tool(description = "查询站点所在相关断面以及上下游的水质数据。")
    public String searchWaterSectionData(
            @ToolParam(description = "站点名称，如 HZ-XS-R01-01") String stcd,
            @ToolParam(description = "日期，格式YYYY-MM-DD。如2026-01-01") String day,
            @ToolParam(description = "小时，24小时制,0-23") String hour) {
        logger.info("MCP getWaterSectionData: stcd={}，day={}，hour={}", stcd, day, hour);
        // 根据站点位置查询 截面， 上下游数据日期的数据
        List<WaterLinkData> rainyWaterData = List.of(
                new WaterLinkData("上游", "太浦河浙北", "对照断面", "II类", "氨氮:0.3mg/L, 总磷:0.08mg/L, 浊度:12NTU"),
                new WaterLinkData("中游", "太浦河浙北", "左岸（近排污口）", "劣V类（氨氮、总磷）", "氨氮:3.5mg/L, 总磷:0.45mg/L, 浊度:85NTU"),
                new WaterLinkData("中游", "太浦河浙北", "中泓（当前站点）", "IV类（总磷）", "氨氮:1.6mg/L, 总磷:0.26mg/L, 高锰酸盐指数:6.2mg/L"),
                new WaterLinkData("中游", "太浦河浙北", "右岸站点", "III类", "氨氮:0.9mg/L, 总磷:0.15mg/L, 浊度:40NTU"),
                new WaterLinkData("下游", "太浦河浙北", "消减断面", "III类", "氨氮:0.7mg/L, 总磷:0.12mg/L, 浊度:25NTU")
        );

        return JSON.toJSONString(DT.success(rainyWaterData));

    }


    public record WaterLinkData(
            @JSONField(name = "上下流") String riverPos,
            @JSONField(name = "断面名称") String section,
            @JSONField(name = "位于断面的位置") String pos,
            @JSONField(name = "数值等级以及（主要污染物）") String waterLevelAndMain,
            @JSONField(name = "水质数据") String data) {
    }
}
