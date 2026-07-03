package com.javan.smart.water.view.controller;

import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.streaming.OutputType;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.alibaba.fastjson.JSON;
import com.javan.smart.water.common.base.ApiResponse;
import com.javan.smart.water.common.base.DT;
import com.javan.smart.water.common.config.UserInfoHolder;
import com.javan.smart.water.common.constant.GraphConstant;
import com.javan.smart.water.common.constant.TaskConstant;
import com.javan.smart.water.common.enums.TaskTableCodeEnum;
import com.javan.smart.water.view.model.ChatModel;
import com.javan.smart.water.view.model.WaterAnalysisModel;
import com.javan.smart.water.view.model.WaterOrderTaskModel;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    private static final Map<String, List<ChatModel>> SESSION_HISTORY_MAP = new ConcurrentHashMap<>();

    private static final Map<String, List<String>> USER_SESSION = new ConcurrentHashMap<>();

    @Autowired
    private CompiledGraph compiledGraph;

    @GetMapping("/session/create")
    public ApiResponse<String> createSession() {
        String sessionId = UUID.randomUUID().toString();
        String userId = UserInfoHolder.currentUser().getUserName();
        List<String> history = USER_SESSION.computeIfAbsent(userId, k -> new ArrayList<>());
        history.add(sessionId);
        return DT.success(sessionId);
    }

    /**
     * assign任务继续
     *
     * @author Javan
     * @since 1.0.0
     */
    @PostMapping(value = "/orderAssign/resume", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> orderAssign(
            @RequestHeader String sessionId, @RequestBody WaterOrderTaskModel waterOrderTaskModel) {
        return chatStream(sessionId, Map.of(), waterOrderTaskModel);
    }

    /**
     * 分析任务继续
     *
     * @author Javan
     * @since 1.0.0
     */
    @PostMapping(value = "/analysis/resume", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> analysisResume(
            @RequestHeader String sessionId, @RequestBody WaterAnalysisModel chatModel) {
        return chatStream(sessionId, Map.of(), chatModel);
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatStream(
            @RequestHeader String sessionId, @RequestBody ChatModel chatModel) {
        String humanAnswer = null;
        if (chatModel.getReplay() != null && chatModel.getReplay()) {
            humanAnswer = chatModel.getInput();
        }
        return chatStream(sessionId, Map.of(GraphConstant.INPUT_KEY, chatModel.getInput()), humanAnswer);
    }

    public <T> Flux<ServerSentEvent<String>> chatStream(String sessionId,
                                                        Map<String, Object> inputs,
                                                        T metadata) {
        String userId = UserInfoHolder.currentUser().getUserName();
        inputs = new HashMap<>(inputs);
        inputs.put(GraphConstant.USER_ID_KEY, userId);

        RunnableConfig.Builder build = RunnableConfig.builder().threadId(sessionId);
        build.addMetadata(GraphConstant.USER_ID_KEY, userId);
        if (metadata != null) {
            build.addMetadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY, metadata);
        }
        List<ChatModel> history = SESSION_HISTORY_MAP.computeIfAbsent(sessionId, k -> new ArrayList<>());
        StringBuilder fullResponseBuilder = new StringBuilder();

        String input = inputs.get(GraphConstant.INPUT_KEY) == null ? null : inputs.get(GraphConstant.INPUT_KEY).toString();
        //        Sinks.many().multicast().onBackpressureBuffer();
        return compiledGraph.stream(inputs, build.build())
                .mapNotNull(output -> {
                    if (output instanceof StreamingOutput<?> streamingOutput) {
                        return doServerSentEvent(streamingOutput);

                    } else if (output instanceof InterruptionMetadata interruptionMetadata) {
                        return doHumanServerSentEvent(interruptionMetadata, history, input);
                    }

                    return null;
                }).concatMap(sse -> {
                    if ("message0".equals(sse.event())) {
                        String data = sse.data();
                        if (StrUtil.isNotBlank(data)) {
                            // 模拟打字機
                            return Flux.fromStream(data.codePoints().boxed())
                                    .map(cp -> ServerSentEvent.<String>builder()
                                            .event("message")
                                            .data(new String(Character.toChars(cp)))
                                            .build())
                                    .delayElements(Duration.ofMillis(50), Schedulers.boundedElastic());
                        }
                    }
                    return Flux.just(sse);
                })
                .doOnNext(sseEvent -> {
                    if (sseEvent.event().equals("message")) {
                        String chunk = sseEvent.data();
                        if (!chunk.isEmpty()) {
                            fullResponseBuilder.append(chunk);
                        }
                    }
                })
                .doOnComplete(() -> {
                    String completeText = fullResponseBuilder.toString();
                    logger.debug("流式生成完毕，准备存入DB: " + completeText);
                    history.add(new ChatModel(input, completeText));
                    // 调用您的 Service/Repository 保存会话记录
                    // chatMemoryService.saveAssistantMessage(sessionId, completeText);
                })
                .onErrorResume(e -> {
                    logger.error("流处理发生异常", e);
                    ServerSentEvent<String> errorEvent = ServerSentEvent.<String>builder()
                            .event("error")
                            .data("AI 生成过程中发生异常，请稍后重试")
                            .build();
                    fullResponseBuilder.append(errorEvent.data());
                    return Flux.just(errorEvent);
                });
    }

    @NotNull
    private ServerSentEvent<String> doHumanServerSentEvent(InterruptionMetadata interruptionMetadata, List<ChatModel> history, String input) {
        //
        String s = interruptionMetadata.metadata(TaskConstant.TASK_INFO_MAKEUP_TYPE).map(Object::toString).orElse("");
        if (TaskTableCodeEnum.CHAT.getTaskType().equals(s)) {
            history.add(new ChatModel(input, interruptionMetadata.metadata(GraphConstant.OUTPUT_KEY).toString()));
        }
        // human in loop
        return ServerSentEvent.<String>builder()
                .event("human") // 自定义一个事件类型
                .data(JSON.toJSONString(interruptionMetadata.metadata()))
                .build();
    }

    @NotNull
    private static ServerSentEvent<String> doServerSentEvent(StreamingOutput<?> streamingOutput) {
        // 1. 获取当前的输出类型和底层 Message
        OutputType type = streamingOutput.getOutputType();
        Message message = streamingOutput.message();

        // 2. 默认提取文本的逻辑
        String msg = "";

        // 仅当它是模型正常回复（非工具、非 Hook）且处于流式增量阶段时，才推送给前端
        if ((type == OutputType.AGENT_MODEL_STREAMING
                || type == OutputType.GRAPH_NODE_STREAMING)) {
            if (message instanceof AssistantMessage assistantMsg) {
                msg = assistantMsg.getText();
            } else if (streamingOutput.getOriginData() instanceof String str) {
                msg = str;
                return ServerSentEvent.<String>builder()
                        .event("message0") // 额外处理
                        .data(msg) // 提取增量文本
                        .build();
            }
        }
        return ServerSentEvent.<String>builder()
                .event("message")
                .data(msg) // 提取增量文本
                .build();
    }

    @PostMapping("/clear")
    public ApiResponse<String> clearSession(
            @RequestHeader String sessionId) {
        if (sessionId != null) {
            SESSION_HISTORY_MAP.remove(sessionId);
        }
        return DT.success("SESSION已清空");
    }

    @GetMapping("/history")
    public ApiResponse<List<ChatModel>> getHistory(@RequestHeader String sessionId) {
        List<ChatModel> chatModels = SESSION_HISTORY_MAP.get(sessionId);
        return DT.success(chatModels);
    }

    @GetMapping("/session")
    public ApiResponse<List<String>> getSessions() {
        String userId = UserInfoHolder.currentUser().getUserName();
        List<String> sessions = USER_SESSION.get(userId);
        return DT.success(sessions);
    }
}
