package com.javan.smart.water.graph.node.model;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * @author FengJ
 * @description 意图识别结果
 */
public record AnalysisIntentResp(@JsonProperty(required = true, value = "intent")
                                 @JsonPropertyDescription("用户意图,如ASK_QUESTION,CLARIFICATION,COMPLETE_ANSWER,FORCE_END") String intent,
                                 @JsonPropertyDescription("仅intent为COMPLETE_ANSWER存在，用户针对提问提供的数据，需要做精炼") String compressData,
                                 @JsonPropertyDescription("意图识别理由") String intentReason
) {

}
