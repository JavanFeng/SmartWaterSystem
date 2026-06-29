package com.javan.smart.water.agent.router.intent;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.javan.smart.water.agent.router.intent.impl.RuleBasedRecognition;
import com.javan.smart.water.agent.router.model.RecognitionResult;
import com.javan.smart.water.agent.router.model.RecognitionStatus;
import com.javan.smart.water.common.base.ServerException;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 默认按照ordered顺序
 *
 * @author FengJ
 * @description 意图识别
 */
@Configuration
public class DefaultIntentRecognitionProvider implements IIntentRecognitionProvider {

    private static final Logger logger = LoggerFactory.getLogger(DefaultIntentRecognitionProvider.class);
    private final List<IRecognition> recognitions;

    @Autowired
    public DefaultIntentRecognitionProvider(ObjectProvider<IRecognition> objectProvider) {
        recognitions = objectProvider.orderedStream().toList();
        if (recognitions.isEmpty()) {
            throw new ServerException("No intent recognition found,  one intent recognition at least");
        }
    }


    @Override
    public RecognitionResult execute(OverAllState state, RunnableConfig config) {
        RecognitionResult result = checkRecognitionExist();
        if (result != null) return result;

        for (IRecognition recognition : recognitions) {
            RecognitionResult intentResult = recognition.recognize(state, config);
            if (intentResult != null && intentResult.getStatus() == RecognitionStatus.SUCCESS) {
                return intentResult;
            }
        }

        RecognitionResult noRecognitionResult = new RecognitionResult();
        noRecognitionResult.setRecognitionName("DefaultIntentRecognitionProvider");
        noRecognitionResult.setErrorMessage("NO INTENT RECOGNITION FIND! DEFAULT INTENT WILL BE SET!");
        noRecognitionResult.setStatus(RecognitionStatus.NO_RECOGNITION_ALL);
        return noRecognitionResult;
    }

    @Nullable
    private RecognitionResult checkRecognitionExist() {
        if (recognitions.isEmpty()) {
            RecognitionResult result = new RecognitionResult();
            result.setRecognitionName("DefaultIntentRecognitionProvider");
            result.setErrorMessage("NO INTENT RECOGNITION FIND! DEFAULT INTENT WILL BE SET!");
            result.setStatus(RecognitionStatus.NO_RECOGNITION_ALL);
            return result;
        }
        return null;
    }

    @Override
    public CompletableFuture<RecognitionResult> asyncExecute(OverAllState state, RunnableConfig config) {
        RecognitionResult checkResult = checkRecognitionExist();
        if (checkResult != null) return CompletableFuture.completedFuture(checkResult);

        CompletableFuture<RecognitionResult> currentFuture =
                CompletableFuture.completedFuture(null);

        for (IRecognition recognition : recognitions) {
            currentFuture = currentFuture.thenCompose(previousResult -> {
                // 【短路逻辑】：如果上一个已经成功，直接返回，不再往下走
                if (previousResult != null && previousResult.getStatus() == RecognitionStatus.SUCCESS) {
                    return CompletableFuture.completedFuture(previousResult);
                }

                logger.debug("Executing recognition: {}", recognition.getClass().getSimpleName());
                return recognition.recognizeAsync(state, config);
            });
        }
        // 兜底逻辑
        currentFuture.thenApply(result -> {
            if (result != null && result.getStatus() == RecognitionStatus.SUCCESS) {
                return result;
            }

            RecognitionResult noRecognitionResult = new RecognitionResult();
            noRecognitionResult.setRecognitionName("DefaultIntentRecognitionProvider");
            noRecognitionResult.setErrorMessage("NO INTENT RECOGNITION FIND! DEFAULT INTENT WILL BE SET!");
            noRecognitionResult.setStatus(RecognitionStatus.NO_RECOGNITION_ALL);
            return noRecognitionResult;
        });

        // 异常兜底
        return currentFuture.exceptionally(ex -> {
            logger.error("System error during intent recognition!!", ex);
            RecognitionResult result = new RecognitionResult();
            result.setRecognitionName("DefaultIntentRecognitionProvider");
            result.setErrorMessage("System error during intent recognition!!");
            result.setStatus(RecognitionStatus.ERROR);
            return result;
        });
    }


}
