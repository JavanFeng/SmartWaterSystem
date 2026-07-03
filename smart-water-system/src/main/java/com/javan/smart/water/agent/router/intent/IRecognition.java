package com.javan.smart.water.agent.router.intent;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.javan.smart.water.agent.router.model.RecognitionResult;

import java.util.concurrent.CompletableFuture;

/**
 * @author FengJ
 * @description 识别类
 */
public interface IRecognition {


    RecognitionResult recognize(OverAllState state, RunnableConfig config);

    CompletableFuture<RecognitionResult> recognizeAsync(OverAllState state, RunnableConfig config);

    /**
     * unique id
     *
     * @author Javan
     * @since 1.0.0
     */
    String getRecognitionId();

    /**
     * 识别超时时间,默认30s
     *
     * @author Javan
     * @since 1.0.0
     */
    default Long getTimeoutMs() {
        return 30000L;
    }
}
