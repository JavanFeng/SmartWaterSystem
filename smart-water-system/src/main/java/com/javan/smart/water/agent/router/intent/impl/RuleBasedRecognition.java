/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.javan.smart.water.agent.router.intent.impl;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.javan.smart.water.agent.router.intent.IRecognition;
import com.javan.smart.water.agent.router.model.RecognitionResult;
import com.javan.smart.water.agent.router.model.RecognitionStatus;
import com.javan.smart.water.common.constant.AgentConstant;
import com.javan.smart.water.common.constant.GraphConstant;
import com.javan.smart.water.common.enums.IntentEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;


/**
 * Rule-based 一些具体业务指令或者快速命中意图，这里是简单实现
 *
 * @author Javan
 * @since 1.0.0
 */
@Component
@Order(AgentConstant.INTENT_RULE_BASED_ORDER)
public class RuleBasedRecognition implements IRecognition {

    private static final Logger logger = LoggerFactory.getLogger(RuleBasedRecognition.class);
    private final static String recognitionId = "rule-base-recognition";

    @Override
    public RecognitionResult recognize(OverAllState state, RunnableConfig config) {
        String userInput = state.value(GraphConstant.INPUT_KEY, "");
        RecognitionResult result = new RecognitionResult();
        result.setRecognitionName(recognitionId);
        result.setStartTimeMillis(System.currentTimeMillis());
        try {
            IntentEnum intent = IntentEnum.fastRecognize(userInput);
            if (intent != null) {
                result.setStatus(RecognitionStatus.SUCCESS);
                result.setMetadata(Map.of());
                result.setIntent(intent);
                logger.debug("[{}]规则意图识别成功，命中意图：{}\n，命中规则类型：{}",
                        recognitionId, intent, intent.getRuleMatch().getClass().getSimpleName());
            } else {
                result.setStatus(RecognitionStatus.FAILED);
                result.setErrorMessage(String.format("[%s]规则意图识别失败。用户输入：%s", recognitionId, userInput));
                logger.warn(result.getErrorMessage());
            }
        } catch (Exception e) {
            result.setStatus(RecognitionStatus.ERROR);
            result.setErrorMessage(e.getMessage());
        } finally {
            result.setEndTimeMillis(System.currentTimeMillis());
        }

        return result;
    }

    @Override
    public CompletableFuture<RecognitionResult> recognizeAsync(OverAllState state, RunnableConfig config) {
        // 简单匹配不超时
        return CompletableFuture.completedFuture(recognize(state,config));
    }

    @Override
    public String getRecognitionId() {
        return recognitionId;
    }


    public static class KeyWordsMatcher implements IntentConditionMatcher {

        private final List<String> keywords;

        public KeyWordsMatcher(List<String> keywords) {
            this.keywords = keywords;
        }

        @Override
        public boolean matches(String input) {
            if (!StringUtils.hasText(input)) {
                return false;
            }

            return keywords.stream().anyMatch(input::contains);
        }
    }

    public static class WordRegexMatcher implements IntentConditionMatcher {
        private final List<Pattern> regexes;

        public WordRegexMatcher(List<Pattern> regexes) {
            this.regexes = regexes;
        }

        @Override
        public boolean matches(String userInput) {
            if (!StringUtils.hasText(userInput)) {
                return false;
            }
            return regexes.stream().anyMatch(p -> p.matcher(userInput).find());
        }
    }

    public interface IntentConditionMatcher {
        boolean matches(String userInput);
    }

}

