package com.javan.smart.water.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.NodeActionWithConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.Hook;
import com.javan.smart.water.agent.analysis.AnalysisAgent;
import com.javan.smart.water.agent.router.model.IntentResp;
import com.javan.smart.water.common.constant.CommonConstant;
import com.javan.smart.water.common.constant.GraphConstant;
import com.javan.smart.water.common.constant.GraphRouterConstant;
import com.javan.smart.water.graph.node.model.AnalysisResult;
import com.javan.smart.water.view.model.WaterAnalysisModel;
import com.javan.smart.water.view.service.WaterAnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 后续可 asNode来改写
 *
 * @author FengJ
 * @description 水质分析节点 SubCompiledGraphNodeAction
 */
public class AnalysisNode implements NodeActionWithConfig {

    private static final Logger logger = LoggerFactory.getLogger(ChatNode.class);

    private AnalysisAgent analysisAgent;

    private List<Hook> hooks;

    private WaterAnalysisService service;

    public AnalysisNode(AnalysisAgent analysisAgent, WaterAnalysisService service) {
        Assert.notNull(analysisAgent, "rbacService must not be null");
        this.analysisAgent = analysisAgent;
        this.hooks = List.of();
        this.service = service;
    }

    public AnalysisNode(AnalysisAgent analysisAgent, List<Hook> hooks, WaterAnalysisService service) {
        Assert.notNull(analysisAgent, "analysisAgent must not be null");
        Assert.notNull(hooks, "hooks must not be null");
        this.analysisAgent = analysisAgent;
        this.hooks = hooks;
        this.service = service;
    }

    @Override
    public Map<String, Object> apply(OverAllState state, RunnableConfig config) throws Exception {
        Optional<IntentResp> resp = state.value(GraphConstant.ANALYSIS_WATER_DEPEND_KEY, IntentResp.class);
        Map<String, Object> result = new HashMap<>();
        // init
        result.put(GraphConstant.MAKE_UP_KEY, List.of());
        result.put(GraphRouterConstant.NEXT_NODE_KEY, "");
        result.put(GraphConstant.ANALYSIS_REPORT_OUTPUT_KEY, "");
        if (resp.isEmpty()) {
            return result;
        }

        IntentResp intent = resp.get();
        if (!intent.forceRefresh()) {
            // 缓存
            WaterAnalysisModel byKey = service.getByKey(service.buildKey(intent.stationCode(), intent.day(), intent.hour()));
            if (byKey != null) {
                result.put(GraphConstant.ANALYSIS_REPORT_OUTPUT_KEY,
                        Flux.just(byKey.getAnalysisResult()));
                return result;
            }
        }

        String userId = state.value(GraphConstant.USER_ID_KEY).map(Object::toString).orElse("");
        String forceEnd = state.value(GraphConstant.FORCE_END_KEY).map(Object::toString).orElse(GraphConstant.AGENT_NO);
        String forceEndReason = state.value(GraphConstant.FORCE_END_REASON_KEY).map(Object::toString).orElse("");
        // 1. 获取上一轮的全局数据池
        Optional<Map> rawMapOpt = state.value(GraphConstant.MAKE_UP_CONTENT_MAP_KEY, Map.class);
        Map<String, String> dataMap = rawMapOpt
                .map(map -> (Map<String, String>) map)
                .orElse(new HashMap<>());
        String mkUpInfo = dataMap.entrySet().stream()
                .map(e -> e.getKey() + "：" + e.getValue())
                .collect(Collectors.joining(System.lineSeparator()));
        String input = state.value(GraphConstant.INPUT_KEY, "");
        boolean enableCot = state.value(GraphConstant.AGENT_COT_ENABLE_KEY, GraphConstant.AGENT_NO).equals(GraphConstant.AGENT_YES);
        ReactAgent reactAgent = analysisAgent.create(hooks,
                enableCot,
                intent.getStationInfo(),
                mkUpInfo,
                forceEnd,
                forceEndReason);
//        Flux<NodeOutput> stream = reactAgent.stream(input, RunnableConfig.builder()
//                .threadId(config.threadId().orElse("default")).build());
//        // 成功
//        result.put(GraphConstant.ANALYSIS_REPORT_OUTPUT_KEY, stream);

        AssistantMessage assistantMessage = reactAgent
                .call(input, RunnableConfig.builder()
                        .threadId(config.threadId().orElse("default"))
                        .addMetadata(CommonConstant.USER_ID, userId)
                        .build());
        logger.debug("analysis result assistantMessage: {}", assistantMessage);
        AnalysisResult convert = AnalysisAgent.outputConverter.convert(assistantMessage.getText());
        if (AnalysisResult.Status.SUCCESS.getStatus().equals(convert.status())) {
            result.put(GraphConstant.ANALYSIS_REPORT_OUTPUT_KEY, assistantMessage.getText());
            // save cache
            service.save(WaterAnalysisModel.builder()
                    .stationCode(intent.stationCode())
                    .day(intent.day())
                    .hour(intent.hour())
                    .analysisResult(convert.analysisReport()).build());
            result.put(GraphConstant.OUTPUT_KEY, Flux.just(convert.analysisReport()));
            result.put(GraphConstant.MAKE_UP_CONTENT_MAP_KEY, new HashMap<>());
            result.put(GraphConstant.FORCE_END_KEY, GraphConstant.AGENT_NO);
            result.put(GraphConstant.FORCE_END_REASON_KEY, "");
            return result;
        } else if (AnalysisResult.Status.FORCE_END.getStatus().equals(convert.status())) {
            //  force end
            result.put(GraphConstant.OUTPUT_KEY, Flux.just(convert.analysisReport()));
            result.put(GraphRouterConstant.NEXT_NODE_KEY, StateGraph.END);
            result.put(GraphConstant.MAKE_UP_CONTENT_MAP_KEY, new HashMap<>());
            result.put(GraphConstant.FORCE_END_KEY, GraphConstant.AGENT_NO);
            result.put(GraphConstant.FORCE_END_REASON_KEY, "");
            return result;
        } else if (AnalysisResult.Status.FATAL_ERROR.getStatus().equals(convert.status())) {
            //  error
            result.put(GraphConstant.FORCE_END_KEY, GraphConstant.AGENT_NO);
            result.put(GraphRouterConstant.NEXT_NODE_KEY, StateGraph.END);
            result.put(GraphConstant.FORCE_END_REASON_KEY, "");
            result.put(GraphConstant.MAKE_UP_CONTENT_MAP_KEY, new HashMap<>());
            result.put(GraphConstant.OUTPUT_KEY, Flux.just(convert.fallbackAction()));
            return result;
        } else {
            // 因为外部系统原因，需要手动提供数据
            if (rawMapOpt.isEmpty()) {
                result.put(GraphConstant.MAKE_UP_CONTENT_MAP_KEY, new HashMap<>());
            }
            result.put(GraphConstant.MAKE_UP_KEY, convert.missingFields());
            result.put(GraphRouterConstant.NEXT_NODE_KEY, GraphRouterConstant.ANALYSIS_INTENT);
            return result;
        }
    }
}
