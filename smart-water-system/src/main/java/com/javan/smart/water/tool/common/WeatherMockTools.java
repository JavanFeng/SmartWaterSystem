package com.javan.smart.water.tool.common;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

/**
 * 模拟天气调用
 *
 * @author FengJ
 * @description 获取地图天气状况
 */
@Component
@Slf4j
public class WeatherMockTools {

    private static final List<List<HistoryWeather>> MOCK_HISTORY = List.of(
            List.of(
                    new HistoryWeather("晴", "33", "0", "31", "2"),
                    new HistoryWeather("晴", "34", "0", "32", "1"),
                    new HistoryWeather("多云", "32", "0", "30", "2")
            ),
            List.of(
                    new HistoryWeather("中雨", "26", "18.5", "24", "4"),
                    new HistoryWeather("大雨", "24", "35.2", "23", "5"),
                    new HistoryWeather("小雨转阴", "25", "6.0", "24", "3")
            ),
            List.of(
                    new HistoryWeather("多云", "28", "0", "25", "2"),
                    new HistoryWeather("阴", "26", "1.2", "24", "2"),
                    new HistoryWeather("晴转多云", "29", "0", "26", "3")
            )
    );

    @Tool(description = "获取指定地区或者城市站点的历史天气。适用于询问地区城市历史天气；辅助分析水质元素，污染物数值异常分析。")
    public String getHistoryWeatherInfo(@ToolParam(description = "查询天气的地区或城市名称，如：杭州") String city) {
        int round = new Random().nextInt(3);
        List<HistoryWeather> historyWeathers = MOCK_HISTORY.get(Math.max(0, round - 1));
        String result = JSON.toJSONString(historyWeathers);
        log.info("查询{}的历史天气结果如下：{}", city, result);
        return result;
    }


    public record HistoryWeather(@JSONField(name = "天气") String condition,
                                 @JSONField(name = "气温（摄氏度）") String temp,
                                 @JSONField(name = "降水量（毫米）") String precipitation,
                                 @JSONField(name = "相对湿度（%）") String humidity,
                                 @JSONField(name = "风速（公里/小时）") String windSpeed) {
    }

}
