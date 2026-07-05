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
    @Value("classpath:/prompts/handle-order-system.md")
    private Resource systemResource;

    @Autowired
    private OrderMockTools orderMockTools;

    public ReactAgent create(List<Hook> hooks) {
        String systemPrompt = SystemPromptTemplate.builder().resource(
                systemResource
        ).build().render();
        // 次数限制 防止死循环
        ToolCallLimitHook toolCallLimitHook = ToolCallLimitHook.builder().runLimit(3)
                .toolName("createOrder")
                .build();

        // default model
        DashScopeChatModel systemModel = chatModel.mutate().defaultOptions(DashScopeChatOptions.builder()
                // 换一个model
                .model(chatModel.getDefaultOptions().getModel())
                .temperature(0.2)
                .maxToken(500)
                .topP(0.6)
                .build()).build();
        return ReactAgent.builder()
                .name(AgentConstant.WORK_AGENT_NAME)
                .model(systemModel)
                .systemPrompt(systemPrompt)
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
