package com.javan.smart.water.graph.node;

import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.action.InterruptableAction;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.utils.TypeRef;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.javan.smart.water.agent.router.model.IntentResp;
import com.javan.smart.water.common.constant.GraphConstant;
import com.javan.smart.water.common.constant.GraphRouterConstant;
import com.javan.smart.water.common.constant.TaskConstant;
import com.javan.smart.water.common.enums.IntentEnum;
import com.javan.smart.water.common.enums.TaskTableCodeEnum;
import com.javan.smart.water.view.model.WaterAnalysisModel;
import com.javan.smart.water.view.service.WaterAnalysisService;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * @author Javan
 * @description 人工介入，需要补充信息
 */
public class TaskInfoWaitNode implements AsyncNodeActionWithConfig, InterruptableAction {

    private WaterAnalysisService service;

    public TaskInfoWaitNode(WaterAnalysisService service) {
        this.service = service;
    }

    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state, RunnableConfig config) {
        Optional<WaterAnalysisModel> feedback = config.getMetadataAndRemove(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY, new TypeRef<WaterAnalysisModel>() {
        });

        String intent = state.value(GraphConstant.INTENT_TYPE).map(Object::toString).orElse("");
        Map<String, Object> result = new HashMap<>();
        result.put(GraphConstant.ANALYSIS_WATER_DEPEND_KEY, null);
        IntentEnum intentEnum = IntentEnum.fromIntent(intent);
        // 回分析节点 或者結束节点
        if (intentEnum == IntentEnum.WATER_ANALYSIS) {
            // 检查槽位(后续抽离单独进行检查执行)
            Map<String, Object> data = state.data();
            IntentResp intentResp = JSONObject.parseObject(JSON.toJSONString(data), IntentResp.class);
            if (feedback.isPresent()) {
                // 存在说明有新的 使用人工传来的
                WaterAnalysisModel waterAnalysisModel = feedback.get();
                if(waterAnalysisModel.getCancel() != null && GraphConstant.AGENT_YES.equals(waterAnalysisModel.getCancel())) {
                    // 取消分析
                    return CompletableFuture.completedFuture(Map.of(
                            GraphConstant.OUTPUT_KEY, Flux.just("由于未补充缺失信息，已为您取消本次分析。"),
                            GraphRouterConstant.NEXT_NODE_KEY, StateGraph.END
                    ));
                }
                intentResp = new IntentResp(intentResp.intent(), intentResp.confidence(),
                        waterAnalysisModel.getStationCode(), waterAnalysisModel.getDay(), waterAnalysisModel.getHour(), intentResp.forceRefresh());
            }
            result.put(GraphConstant.ANALYSIS_WATER_DEPEND_KEY, intentResp);
            result.put(GraphRouterConstant.NEXT_NODE_KEY, GraphRouterConstant.WATER_ANALYSIS);
            return CompletableFuture.completedFuture(result);
        }
        return CompletableFuture.completedFuture(Map.of(
                GraphRouterConstant.NEXT_NODE_KEY, GraphRouterConstant.CHAT
        ));
    }

    @Override
    public Optional<InterruptionMetadata> interrupt(String nodeId, OverAllState state, RunnableConfig config) {
        Optional<Object> feedback = config.metadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY);
        if (feedback.isPresent()) {
            return Optional.empty();
        }

        String intent = state.value(GraphConstant.INTENT_TYPE).map(Object::toString).orElse("");
        IntentEnum intentEnum = IntentEnum.fromIntent(intent);
        if (intentEnum == IntentEnum.WATER_ANALYSIS) {
            // 检查槽位(后续抽离单独进行检查执行)
            Map<String, Object> data = state.data();
            IntentResp intentResp = JSONObject.parseObject(JSON.toJSONString(data), IntentResp.class);
            // 非强制刷新采用缓存
            if (!intentResp.forceRefresh() && StrUtil.isNotBlank(intentResp.stationCode()) && StrUtil.isNotBlank(intentResp.day()) && StrUtil.isNotBlank(intentResp.hour())) {
                // 检查缓存是否存在
                if (service.getByKey(service.buildKey(intentResp.stationCode(), intentResp.day(), intentResp.hour())) != null) {
                    // 缓存存在，直接返回
                    return Optional.empty();
                }

            }
            if (StrUtil.isBlank(intentResp.stationCode()) || StrUtil.isBlank(intentResp.day()) || StrUtil.isBlank(intentResp.hour())) {
                InterruptionMetadata.Builder builder = InterruptionMetadata.builder(nodeId, state);
                addMetadata(builder, TaskConstant.TASK_INFO_MAKEUP_TYPE, TaskTableCodeEnum.CONTAMINANT_ANALYSIS.getTaskType());
                addMetadata(builder, TaskConstant.CONTAMINANT_ANALYSIS_HOUR, intentResp.hour());
                addMetadata(builder, TaskConstant.CONTAMINANT_ANALYSIS_STATION_CODE, intentResp.stationCode());
                addMetadata(builder, TaskConstant.CONTAMINANT_ANALYSIS_DAY, intentResp.day());
                return Optional.of(builder.build());
            }
            // TODO 都完整的话，需要校验站点是否真实存在，否则也进行interrupt
        }
        return Optional.empty();
    }

    private static InterruptionMetadata.Builder addMetadata(InterruptionMetadata.Builder metadata, String key, String value) {
        if (StrUtil.isNotBlank(value)) {
            metadata.addMetadata(key, value);
        }
        return metadata;
    }
}
