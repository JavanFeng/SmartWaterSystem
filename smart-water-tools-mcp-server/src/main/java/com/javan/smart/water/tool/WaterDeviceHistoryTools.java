package com.javan.smart.water.tool;

import com.alibaba.fastjson.JSON;
import com.javan.smart.water.common.McpPermission;
import com.javan.smart.water.common.base.DT;
import com.javan.smart.water.config.IMcpTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 站点历史查询工具
 *
 * @author Javan
 * @since 1.0.0
 */
@Component
public class WaterDeviceHistoryTools implements IMcpTool {

    private static final Logger logger = LoggerFactory.getLogger(WaterDeviceHistoryTools.class);

    @McpPermission
    @Tool(description = "查询站点位水质信息历史记录。可以按站点，时间范围等条件查询。")
//    @McpTool(name = "getWaterHistory", description = "查询站点位水质信息历史记录。可以按站点，时间范围等条件查询。"
//            , annotations = {Tool.class, ToolParam.class})
    public String getWaterHistory(
            @ToolParam(description = "站点名称，如 HZ-XS-R01-01") String stcd,
            @ToolParam(description = "查询过去时间天数，取值为1-14之间。默认为7") Integer days) {
        logger.info("MCP getWaterHistory: stcd={}, days={}", stcd, days);
        if (days < 1 || days > 7) {
            return JSON.toJSONString(DT.error("参数错误", "days参数取值范围为1-14"));
        }

        List<WaterHistoryMock> historyData = List.of(
                // 5天前：降雨初期，地表径流导致污染物浓度开始上升
                new WaterHistoryMock("2026-05-27", "6", "氨氮:0.8mg/L, 总磷:0.15mg/L, 高锰酸盐指数:4.2mg/L"),

                // 4天前：持续降雨，污染负荷达到峰值，水质下降
                new WaterHistoryMock("2026-05-28", "6", "氨氮:1.5mg/L, 总磷:0.28mg/L, 高锰酸盐指数:5.8mg/L"),

                // 3天前：降雨结束，污染物浓度维持高位
                new WaterHistoryMock("2026-05-29", "6", "氨氮:1.3mg/L, 总磷:0.25mg/L, 高锰酸盐指数:5.5mg/L"),

                // 2天前：天气转晴，水体自净开始，指标略有下降
                new WaterHistoryMock("2026-05-30", "6", "氨氮:1.0mg/L, 总磷:0.20mg/L, 高锰酸盐指数:4.8mg/L"),

                // 1天前：水质持续好转，接近标准限值
                new WaterHistoryMock("2026-05-31", "6", "氨氮:0.7mg/L, 总磷:0.14mg/L, 高锰酸盐指数:4.0mg/L"),

                // 今天：水质恢复优良，各项指标达标
                new WaterHistoryMock("2026-06-01", "6", "氨氮:0.5mg/L, 总磷:0.10mg/L, 高锰酸盐指数:3.5mg/L")
        );

        return JSON.toJSONString(DT.success(historyData));
    }

    public record WaterHistoryMock(String day, String hour, String factorContent) {
    }
}
