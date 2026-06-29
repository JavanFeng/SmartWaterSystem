package com.javan.smart.water.agent.router.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.Map;

/**
 * @author FengJ
 * @description 意图识别结果
 */
public record IntentResp(@JsonProperty(required = true, value = "intent")
                         @JsonPropertyDescription("用户意图,如WATER_ANALYSIS,CHAT,WATER_QA等") String intent,
                         @JsonProperty(required = true, value = "confidence")
                         @JsonPropertyDescription("识别信心指数（0到1，如0.7）") String confidence,
                         @JsonProperty(required = false, value = "stationCode")
                         @JsonPropertyDescription("站点编号") String stationCode,
                         @JsonProperty(required = false, value = "day")
                         @JsonPropertyDescription("查询数据日期。格式为yyyy-MM-dd") String day,
                         @JsonProperty(required = false, value = "hour")
                         @JsonPropertyDescription("查询数据小时。24小时制 0-23") String hour,
                         @JsonProperty(required = false, value = "forceRefresh")
                         @JsonPropertyDescription("是否强制执行,如用户要求重新执行，则为true。默认false")
                         boolean forceRefresh) {

    @JsonIgnore
    public Map<String, Object> getMetadata() {
        return Map.of(
                "confidence", confidence,
                "stationCode", stationCode,
                "day", day,
                "hour", hour,
                "forceRefresh", forceRefresh
        );
    }

    @JsonIgnore
    public String getStationInfo() {
        // 查询站点基础信息，如所在城市等
        return "城市：%s,站点编号：%s, 日期：%s，小时（0-23）：%s".formatted("杭州", stationCode, day, hour);
    }
}
