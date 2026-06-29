package com.javan.smart.water.graph.node;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.action.InterruptableAction;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.utils.TypeRef;
import com.javan.smart.water.agent.work.HandleOrderAgent;
import com.javan.smart.water.auth.IRbacService;
import com.javan.smart.water.common.constant.GraphConstant;
import com.javan.smart.water.common.constant.TaskConstant;
import com.javan.smart.water.common.enums.RenderTypeEnum;
import com.javan.smart.water.common.enums.TaskTableCodeEnum;
import com.javan.smart.water.view.model.WaterOrderTaskModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.PromptTemplate;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * 如果是admin的话 询问是否依据分析是否建立工单
 *
 * @author FengJ
 * @description 创建工单
 */
public class HandleOrderNode implements AsyncNodeActionWithConfig, InterruptableAction {
    private static final Logger logger = LoggerFactory.getLogger(HandleOrderNode.class);

    public static final String WATER_ORDER = "WATER_ORDER";

    private static final String INPUT_TEMPLATE = """
            ## 自动化研判分析报告:
            {%s}
            ## 人工审核指令:
            {%s}
            """.formatted(GraphConstant.ANALYSIS_REPORT_OUTPUT_KEY, WATER_ORDER);
    private ReactAgent handleOrderAgent;

    private IRbacService service;

    public HandleOrderNode(HandleOrderAgent handleOrderAgent, IRbacService service) {
        this.handleOrderAgent = handleOrderAgent.create(List.of());
        this.service = service;
    }

    @Override
    public Optional<InterruptionMetadata> interrupt(String nodeId, OverAllState state, RunnableConfig config) {
        Optional<Object> feedback = config.metadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY);
        if (feedback.isPresent()) {
            return Optional.empty();
        }
        // 是否为admin,是否依据分析是否建立工单
        String userId = state.value(GraphConstant.USER_ID_KEY).map(Object::toString).orElse("");
        boolean isAdmin = service.isAdmin(userId);
        if (isAdmin) {
            // 需要让管理员进行补充，后续可以设置默认执行的业务逻辑（定时任务）
            return Optional.of(InterruptionMetadata.builder(nodeId, state)
                    .addMetadata(TaskConstant.TASK_INFO_MAKEUP_TYPE, TaskTableCodeEnum.ORDER_ASSIGN.getTaskType())
                    .build());
        }
        return Optional.empty();
    }

    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state, RunnableConfig config) {
        Optional<WaterOrderTaskModel> feedback = config.getMetadataAndRemove(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY, new TypeRef<WaterOrderTaskModel>() {
        });
        Map<String, Object> output = new HashMap<>();
        output.put(GraphConstant.RENDER_TYPE, RenderTypeEnum.TEXT.getType());
        output.put(GraphConstant.OUTPUT_KEY, "");
        WaterOrderTaskModel waterOrderTaskModel = feedback.orElse(null);
        if (waterOrderTaskModel != null
                && GraphConstant.AGENT_YES.equals(waterOrderTaskModel.getAgreement())) {
            try {
                UserMessage userMessage = UserMessage.builder()
                        .text(PromptTemplate.builder().template(INPUT_TEMPLATE)
                                .variables(state.data()).build().render(
                                        Map.of(WATER_ORDER, waterOrderTaskModel)
                                )).build();
                Flux<NodeOutput> text = handleOrderAgent
                        .stream(userMessage, RunnableConfig.builder()
                                .threadId(config.threadId().orElse("default")).build());
                logger.debug("创建工单结果：{}", text);
                output.put(GraphConstant.OUTPUT_KEY, text);
                return CompletableFuture.completedFuture(output);
            } catch (Exception e) {
                logger.error("创建工单失败", e);
                output.put(GraphConstant.OUTPUT_KEY, Flux.just("创建工单出现异常，请手动创建！"));
                return CompletableFuture.completedFuture(output);
            }
        } else {
            // 取消 或者 不用创建
            output.put(GraphConstant.OUTPUT_KEY, Flux.just("已取消创建工单"));
            return CompletableFuture.completedFuture(output);
        }
    }
}
