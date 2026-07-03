package com.javan.smart.water.agent.data;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.Hook;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.javan.smart.water.common.constant.AgentConstant;
import com.javan.smart.water.interceptor.LogToolInterceptor;
import com.javan.smart.water.interceptor.ToolAuthCheckInterceptor;
import com.javan.smart.water.tool.common.WeatherMockTools;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * TODO 现在tool是固定死并且全量，后续需要动态更新
 *
 * @author Javan
 * @since 1.0.0
 */
@Component
public class DataToolAgent {
    @Qualifier("dashScopeChatModel")
    @Autowired
    DashScopeChatModel chatModel;
    private static final MemorySaver memorySaver = new MemorySaver();

    private static final String SYS_PROMPT = "你是智慧水务平台的数据调度与工具调用专家。你的核心职责是精准理解用户关于水质分析、污染溯源及水务管理的提问，并自主调度最合适的 MCP 工具或内部接口来获取真实数据。请始终保持客观严谨的态度，严禁编造任何事实信息。";
    @Value("classpath:/prompts/tool-instruction.md")
    private Resource instructionResource;

    @Autowired
    private ToolCallbackProvider tools;
    @Autowired
    private WeatherMockTools weatherMockTools;

    public ReactAgent create(List<Hook> hooks) {
        String instruction = SystemPromptTemplate.builder().resource(
                instructionResource
        ).build().render();

        // default model
        DashScopeChatModel systemModel = chatModel.mutate().defaultOptions(DashScopeChatOptions.builder()
//                .model(DashScopeChatModel.DEFAULT_MODEL_NAME)
                        .model(chatModel.getDashScopeChatOptions().getModel())
                .temperature(0.1)
                .maxToken(1000)
                .topP(0.5)
                .build()).build();
        return ReactAgent.builder()
                .name(AgentConstant.TOOL_AGENT_NAME)
                .model(systemModel)
                .systemPrompt(SYS_PROMPT)
                .instruction(instruction)
                .hooks(hooks)
                .inputType(String.class)
                .interceptors(new LogToolInterceptor(), new ToolAuthCheckInterceptor())
                // 先用内存即可
                .saver(memorySaver)
                .tools(Stream.concat(Arrays.stream(tools.getToolCallbacks()),
                        Arrays.stream(MethodToolCallbackProvider.builder().toolObjects(weatherMockTools)
                                .build().getToolCallbacks())).toArray(ToolCallback[]::new))
                .build();
    }
}
