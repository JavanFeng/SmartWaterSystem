package com.javan.smart.water.agent.analysis;

import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.dashscope.spec.DashScopeModel;
import com.alibaba.cloud.ai.graph.agent.AgentTool;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.Hook;
import com.alibaba.cloud.ai.graph.agent.hook.summarization.SummarizationHook;
import com.alibaba.cloud.ai.graph.agent.hook.toolcalllimit.ToolCallLimitHook;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolInterceptor;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.javan.smart.water.agent.data.DataToolAgent;
import com.javan.smart.water.common.constant.AgentConstant;
import com.javan.smart.water.common.constant.GraphConstant;
import com.javan.smart.water.graph.node.model.AnalysisResult;
import com.javan.smart.water.interceptor.LogToolInterceptor;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Component
public class AnalysisAgent {

    @Qualifier("dashScopeChatModel")
    @Autowired
    DashScopeChatModel chatModel;

    @Autowired
    DataToolAgent dataToolAgent;

    private static final MemorySaver memorySaver = new MemorySaver();

    public static final BeanOutputConverter<AnalysisResult> outputConverter = new BeanOutputConverter<>(AnalysisResult.class);

    private static final String SYS_PROMPT = "你是一个专业的工业级水质分析引擎（Analysis Engine）。你的唯一任务是：接收上游数据调度层传入的数据以及调用tool_agent工具获取的数据，进行严谨研判，并**严格以 JSON 格式**输出分析结果。绝对不要直接输出自然语言或纯 Markdown 文本与用户对话，你的输出将被下游系统程序化解析。";
    @Value("classpath:/prompts/analysis-instruction.md")
    private Resource instructionResource;

    private static final String fallbackInject = """
            【当前降级数据注入】
             %s
            """;

    private static final String userIntentInject = """
            【用戶是否要求强制结束分析】
             强制结束: %s,
             用户原回答：%s
            """;
    private static final String SUMMARY_PROMPT = """
            <role>
            水质分析上下文提取专家
            </role>

            <primary_objective>
            你在当前任务中的唯一目标，是从下方的对话历史中提取出与“水质污染分析”最相关的核心事实、数据状态和业务意图，作为下一轮研判报告的上下文。
            </primary_objective>

            <instructions>
            在当前的处理步骤中，下方的对话历史将被你提取出的结构化上下文所替换。请严格按照以下维度进行信息提取和记录：

            1. **核心业务意图**：用户当前是在询问具体指标趋势，还是在进行异常溯源排查？
            2. **关键数据快照**：提取已确认的核心水质指标（如COD、氨氮等）、超标倍数、以及关键的气象水文条件（流速、降雨等）。
            3. **工具调用状态**：明确记录上一轮是否触发了tool_agent工具，以及该工具的执行结果或待查询的具体断面/数据类型。
            4. **阶段性结论**：提取已经得出的合规性判定、嫌疑污染源排序或排除项。
            5. **数据缺失记录**：如实记录用户在对话中提及但目前仍缺失的数据维度。

            ⚠️ 严格约束：
            - 仅回复提取出的结构化上下文内容。
            - 绝对不要包含任何通用的聊天废话、寒暄语或无关的历史交互。
            - 严禁将系统级的“行为准则”或“输出格式约束”当作上下文提取出来。
            </instructions>

            <messages>
            需要总结的消息列表：
            %s
            </messages>
                        """;


    /**
     * create
     *
     * @param hooks
     * @param enableCOT
     * @param stationInfo    站点信息
     * @param fallbackData   用户补充的信息
     * @param userForceEnd   用户要求强制结束分析
     * @param forceEndReason 强制结束分析的原因
     * @return
     */
    public ReactAgent create(List<Hook> hooks,
                             Boolean enableCOT,
                             String stationInfo,
                             String fallbackData,
                             String userForceEnd,
                             String forceEndReason) {
        String fallbackDataFinal = StrUtil.EMPTY;
        if (StrUtil.isNotBlank(fallbackData)) {
            fallbackDataFinal = fallbackInject.formatted(fallbackData);
        }

        String forceEndFinal = StrUtil.EMPTY;
        if (StrUtil.isNotBlank(userForceEnd) && GraphConstant.AGENT_YES.equals(userForceEnd)) {
            forceEndFinal = userIntentInject.formatted(userForceEnd, forceEndReason);
        }
        String instruction = SystemPromptTemplate.builder().resource(
                instructionResource
        ).build().render(Map.of("stationInfo", stationInfo,
                "fallbackDataInjection", fallbackDataFinal,
                "forceEndInjection", forceEndFinal));
        ReactAgent toolAgent = dataToolAgent.create(List.of());
        // 次数限制 防止死循环
        ToolCallLimitHook toolCallLimitHook = ToolCallLimitHook.builder().runLimit(20)
                .toolName(toolAgent.name())
                .build();

        // default model
        DashScopeChatModel systemModel = chatModel.mutate().defaultOptions(DashScopeChatOptions.builder()
                // 用更高級別model
//                .model(DashScopeModel.ChatModel.QWEN_MAX.value)
                        .model(chatModel.getDefaultOptions().getModel())
                .temperature(0.1)
                .topP(0.5)
                .build()).build();
        // 防止token过大
        SummarizationHook summarizationHook = SummarizationHook.builder().model(chatModel)
                .keepFirstUserMessage(true)
                .messagesToKeep(20)
                .maxTokensBeforeSummary(4000)
                .summaryPrompt(SUMMARY_PROMPT)
                .summaryPrefix("## 之前的对话内容:").build();
        return ReactAgent.builder()
                .name(AgentConstant.ANALYSIS_AGENT_NAME)
                .model(systemModel)
                .returnReasoningContents(enableCOT)
                .hooks(Stream.concat(
                        Stream.of(toolCallLimitHook, summarizationHook),
                        hooks.stream()
                ).toArray(Hook[]::new))
                .systemPrompt(SYS_PROMPT)
                .outputSchema(outputConverter.getFormat())
                // 这种方式现在无法带context,需要底层代码的原因
                .tools(AgentTool.create(toolAgent))
//                .enableLogging(true)
                .interceptors(
                        new LogToolInterceptor()
                )
                // 先用内存即可
                .saver(memorySaver)
                .instruction(instruction)
                .build();
    }
}
