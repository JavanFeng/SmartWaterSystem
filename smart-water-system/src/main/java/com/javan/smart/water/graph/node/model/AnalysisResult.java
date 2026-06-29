package com.javan.smart.water.graph.node.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Getter;

import java.util.List;

/**
 * @author Javan
 * @since 1.0.0
 */
public record AnalysisResult(
        // 2. 核心状态：用于判断分析是否成功或需要补充信息
        @JsonProperty(required = true, value = "status")
        @JsonPropertyDescription("分析状态,如 SUCCESS, NEED_CLARIFICATION, FATAL_ERROR, FORCE_END")
        String status,

        // 3. 缺失数据：用于信息补充节点精准追问
        @JsonProperty(value = "missingFields")
        @JsonPropertyDescription("当 status 为 NEED_CLARIFICATION 时，列出缺失的关键数据维度")
        List<String> missingFields,

        // 4. 降级方案：用于提供 Plan B
        @JsonProperty(value = "fallbackAction")
        @JsonPropertyDescription("当 status 为 FATAL_ERROR 时，如果分析失败时，你可以提供的降级分析方案（一句话描述）")
        String fallbackAction,

        // 5. 核心结论：存放完整的 Markdown 格式研判报告
        @JsonProperty(value = "analysisReport")
        @JsonPropertyDescription("当 status 为 SUCCESS 或 FORCE_END 时，存放完整的 Markdown 格式研判报告")
        String analysisReport
) {

    @Getter
    public enum Status {
        SUCCESS("SUCCESS"),
        NEED_CLARIFICATION("NEED_CLARIFICATION"),
        FATAL_ERROR("FATAL_ERROR"),
        FORCE_END("FORCE_END");
        private final String status;

        Status(String status) {
            this.status = status;
        }
    }
}