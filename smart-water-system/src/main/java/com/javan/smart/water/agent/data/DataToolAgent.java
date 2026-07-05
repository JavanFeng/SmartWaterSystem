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
    @Value("classpath:/prompts/tool-system.md")
    private Resource systemResource;

    @Autowired
    private ToolCallbackProvider tools;
    @Autowired
    private WeatherMockTools weatherMockTools;

    public ReactAgent create(List<Hook> hooks) {
        String systemPrompt = SystemPromptTemplate.builder().resource(
                systemResource
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
                .systemPrompt(systemPrompt)
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
