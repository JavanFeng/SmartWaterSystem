package com.javan.smart.water.agent.work;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.Hook;
import com.alibaba.cloud.ai.graph.agent.hook.toolcalllimit.ToolCallLimitHook;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.javan.smart.water.common.constant.AgentConstant;
import com.javan.smart.water.tool.order.OrderMockTools;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Stream;

/**
 * @author FengJ
 * @description 派发工单等调度agent, 先只有工单吧
 */
@Component
public class HandleOrderAgent {

    @Qualifier("dashScopeChatModel")
    @Autowired
    DashScopeChatModel chatModel;
    private static final MemorySaver memorySaver = new MemorySaver();
    private static final String SYS_PROMPT = "你是智慧水务平台的工单调度专员。你的核心职责是综合上游“研判专家”的自动化分析报告以及“人工审核员”的干预指令，智能决策并生成最终的线下处置工单。请始终保持客观严谨、忠实于事实的调度态度。";
    @Value("classpath:/prompts/handle-order-instruction.md")
    private Resource instructionResource;

    @Autowired
    private OrderMockTools orderMockTools;

    public ReactAgent create(List<Hook> hooks) {
        String instruction = SystemPromptTemplate.builder().resource(
                instructionResource
        ).build().render();
        // 次数限制 防止死循环
        ToolCallLimitHook toolCallLimitHook = ToolCallLimitHook.builder().runLimit(3)
                .toolName("createOrder")
                .build();

        // default model
        DashScopeChatModel systemModel = chatModel.mutate().defaultOptions(DashScopeChatOptions.builder()
                .model(DashScopeChatModel.DEFAULT_MODEL_NAME)
                .temperature(0.2)
                .maxToken(500)
                .topP(0.6)
                .build()).build();
        return ReactAgent.builder()
                .name(AgentConstant.WORK_AGENT_NAME)
                .model(systemModel)
                .systemPrompt(SYS_PROMPT)
                .instruction(instruction)
                .hooks(Stream.concat(Stream.of(toolCallLimitHook), hooks.stream()).toArray(Hook[]::new))
                // 先用内存即可
                .saver(memorySaver)
                .tools(MethodToolCallbackProvider.builder()
                        .toolObjects(
                                orderMockTools
                        )
                        .build().getToolCallbacks())
                .build();
    }

}
