package com.javan.smart.water.graph.node;

import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.dashscope.spec.DashScopeModel;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.fastjson.JSON;
import com.javan.smart.water.agent.router.model.IntentResp;
import com.javan.smart.water.common.base.ServerException;
import com.javan.smart.water.common.constant.GraphConstant;
import com.javan.smart.water.common.constant.GraphRouterConstant;
import com.javan.smart.water.graph.node.model.AnalysisIntentResp;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * @author FengJ
 * @description 分析意图识别节点
 */
public class AnalysisIntentNode implements AsyncNodeActionWithConfig {
    private static final Logger logger = LoggerFactory.getLogger(AnalysisIntentNode.class);
    /**
     * 根据需要修改持久化
     */
    private static final MessageWindowChatMemory CHAT_MEMORY = MessageWindowChatMemory.builder()
            .chatMemoryRepository(new InMemoryChatMemoryRepository())
            .maxMessages(25).build();
    private Resource intentPromptResource;

    private final ChatClient chatClient;

    public AnalysisIntentNode(ChatClient.Builder chatClientBuilder, Resource intentPromptResource) {
        this.chatClient = chatClientBuilder.build();
        this.intentPromptResource = intentPromptResource;
    }

    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state, RunnableConfig config) {
        // Get input data
        String inputData = state.value(GraphConstant.USER_REPLY_KEY).map(Object::toString).orElse("");
        Optional<List> missingFieldsOpt = state.value(GraphConstant.MAKE_UP_KEY, List.class);
        if (missingFieldsOpt.isEmpty()) {
            throw new ServerException("missing fields are empty?");
        }

        if (StrUtil.isBlank(inputData)) {
            return CompletableFuture.completedFuture(Map.of(
                    GraphConstant.ANALYSIS_INTENT_KEY, GraphRouterConstant.WATER_ANALYSIS_MAKE_UP,
                    GraphRouterConstant.NEXT_NODE_KEY, GraphRouterConstant.WATER_ANALYSIS_MAKE_UP
            ));
        }
        logger.info("{} is running, inputData:{}, state: {}", this.getClass().getSimpleName(), inputData,
                JSON.toJSONString(state));
        String instruction = SystemPromptTemplate.builder().resource(
                intentPromptResource
        ).build().render(Map.of(
                "requiredData", "为了更好的为您分析，希望您可以提供该站点该日期以下数据" + missingFieldsOpt.get().get(0)
        ));

        // Process using ChatClient
        try {
            AnalysisIntentResp intentResp = chatClient
                    .prompt("用户回答：" + inputData)
                    .system(instruction)
                    .advisors(SimpleLoggerAdvisor.builder().build(),
                            MessageChatMemoryAdvisor.builder(CHAT_MEMORY)
                                    .conversationId(config.threadId().orElse("default")).build())
                    .options(DashScopeChatOptions.builder()
                            .model(DashScopeModel.ChatModel.QWEN_PLUS.value).build())
                    .call().entity(AnalysisIntentResp.class);
            Map<String, Object> result = new HashMap<>();
            result.put(GraphConstant.ANALYSIS_INTENT_KEY, intentResp.intent());
            result.put(GraphConstant.ANALYSIS_INTENT_REASON_KEY, intentResp.intentReason());
            if (Intent.COMPLETE_ANSWER.getCode().equals(intentResp.intent()) || Intent.FORCE_END.getCode().equals(intentResp.intent())) {
                result.put(GraphRouterConstant.NEXT_NODE_KEY, GraphRouterConstant.WATER_ANALYSIS);
                Optional<Map> rawMapOpt = state.value(GraphConstant.MAKE_UP_CONTENT_MAP_KEY, Map.class);
                Map<String, String> makeUpContentMap;
                if (rawMapOpt.isEmpty()) {
                    makeUpContentMap = new HashMap<>();
                    result.put(GraphConstant.MAKE_UP_CONTENT_MAP_KEY, makeUpContentMap);
                } else {
                    makeUpContentMap = rawMapOpt.map(e -> (Map<String, String>) e).get();
                }
                // 数据不为空
                if (StrUtil.isNotBlank(intentResp.compressData())) {
                    makeUpContentMap.put(String.valueOf(missingFieldsOpt.get().get(0)), intentResp.compressData());
                }
                // 用户强制结束
                if (Intent.FORCE_END.getCode().equals(intentResp.intent())) {
                    result.put(GraphConstant.FORCE_END_KEY, GraphConstant.AGENT_YES);
                    result.put(GraphConstant.FORCE_END_REASON_KEY, intentResp.intentReason());
                }
                // init
                result.put(GraphConstant.USER_REPLY_KEY, "");
                result.put(GraphConstant.ANALYSIS_INTENT_KEY, "");
                result.put(GraphConstant.ANALYSIS_INTENT_REASON_KEY, "");
            } else {
                result.put(GraphRouterConstant.NEXT_NODE_KEY, GraphRouterConstant.WATER_ANALYSIS_MAKE_UP);
            }

            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            // If ChatClient call fails, use simulated result
            Flux<String> result = Flux.just("[%s] Simulated processing result: %s".formatted(this.getClass().getSimpleName(), inputData));
            return
                    CompletableFuture.completedFuture(Map.of(GraphConstant.OUTPUT_KEY, result));
        }

    }

    @Getter
    public enum Intent {

        ASK_QUESTION("ASK_QUESTION"),

        CLARIFICATION("CLARIFICATION"),

        COMPLETE_ANSWER("COMPLETE_ANSWER"),

        FORCE_END("FORCE_END");

        private final String code;

        Intent(String code) {
            this.code = code;
        }
    }
}
