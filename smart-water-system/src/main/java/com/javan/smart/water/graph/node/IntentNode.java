package com.javan.smart.water.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.fastjson.JSON;
import com.javan.smart.water.agent.router.intent.IIntentRecognitionProvider;
import com.javan.smart.water.agent.router.model.RecognitionStatus;
import com.javan.smart.water.auth.IRbacService;
import com.javan.smart.water.common.constant.GraphConstant;
import com.javan.smart.water.common.enums.IntentEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @author FengJ
 * @description 意图识别节点
 */
public class IntentNode implements AsyncNodeActionWithConfig {

    private static final Logger logger = LoggerFactory.getLogger(IntentNode.class);
    private final IIntentRecognitionProvider intentRecognitionProvider;
    private final IRbacService iRbacService;

    public IntentNode(IIntentRecognitionProvider intentRecognitionProvider, IRbacService rbacService) {
        // assert not null
        Assert.notNull(rbacService, "rbacService must not be null");
        Assert.notNull(intentRecognitionProvider, "intentRecognitionProvider must not be null");
        this.intentRecognitionProvider = intentRecognitionProvider;
        this.iRbacService = rbacService;
    }

    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state, RunnableConfig config) {
        // init
        Map<String, Object> result = new HashMap<>();
        result.put(GraphConstant.INTENT_TYPE, IntentEnum.WATER_QA.getCode()); // 默认兜底意图
        result.put(GraphConstant.AGENT_AUTH_STATUS_KEY, GraphConstant.AGENT_AUTH_STATUS_SUCCESS_VALUE); // 默认放行
        result.put(GraphConstant.AGENT_AUTH_DENIED_REASON_KEY, "");

        String input = state.value(GraphConstant.INPUT_KEY).map(Object::toString).orElse("");
        String userId = state.value(GraphConstant.USER_ID_KEY).map(Object::toString).orElse("");
        if (input.isEmpty()) {
            logger.info("IntentNode: 输入文本为空{}，默认执行知识库QA意图", input);
            return CompletableFuture.completedFuture(result);
        }

        return intentRecognitionProvider.asyncExecute(state, config).thenApply(recognitionResult -> {
            logger.info("IntentNode: 输入文本{}，识别结果{}", input, JSON.toJSONString(recognitionResult));
            // TODO 置信度检查
            if (recognitionResult.getStatus() == RecognitionStatus.SUCCESS) {
                result.put(GraphConstant.INTENT_TYPE, recognitionResult.getIntent().getCode());
                result.putAll(recognitionResult.getMetadata());
                // 权限检查
                if (!iRbacService.hasAgentPermission(userId, recognitionResult.getIntent().getAgentName())) {
                    result.put(GraphConstant.AGENT_AUTH_STATUS_KEY, GraphConstant.AGENT_AUTH_STATUS_FAIL_VALUE);
                    result.put(GraphConstant.AGENT_AUTH_DENIED_REASON_KEY, GraphConstant.AGENT_AUTH_DENIED_NO_PERMISSION);
                }
                return result;
            } else {
                // other error
                result.put(GraphConstant.OUTPUT_KEY, Flux.just(recognitionResult.getErrorMessage()));
                result.put(GraphConstant.INTENT_TYPE, StateGraph.END);
                return result;
            }
        });
    }
}
