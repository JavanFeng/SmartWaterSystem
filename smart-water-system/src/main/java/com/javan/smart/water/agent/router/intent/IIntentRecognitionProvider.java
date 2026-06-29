package com.javan.smart.water.agent.router.intent;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.javan.smart.water.agent.router.model.RecognitionResult;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author FengJ
 * @description 意图识别工具提供
 */
public interface IIntentRecognitionProvider {
    /**
     * 获取自定义 IRecognition 列表
     *
     * @return IRecognition 列表
     */
    default List<IRecognition> getRecognitions() {
        return Collections.emptyList();
    }


    /**
     * 执行意图识别
     *
     * @author Javan
     * @since 1.0.0
     */
    RecognitionResult execute(OverAllState state, RunnableConfig config);

    /**
     * 执行意图识别
     *
     * @author Javan
     * @since 1.0.0
     */
    CompletableFuture<RecognitionResult> asyncExecute(OverAllState state, RunnableConfig config);
}
