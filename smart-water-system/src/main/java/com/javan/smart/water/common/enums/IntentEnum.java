package com.javan.smart.water.common.enums;

import cn.hutool.core.util.StrUtil;
import com.javan.smart.water.agent.router.intent.impl.RuleBasedRecognition;
import com.javan.smart.water.common.constant.AgentConstant;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author FengJ
 * @description 意图类型
 */
@Getter
public enum IntentEnum {
    WATER_QA("WATER_QA",
            "知识问答：用户询问水质等级标准，水资源污染物处理规范等但不涉及站点水质数据值异常分析/污染物原因分析等",
            "涉及查询水质等级标准/水质污染物处理规范",
            new RuleBasedRecognition.WordRegexMatcher(List.of(
                    Pattern.compile("^(水质等级标准|水资源|污染物)(处理规范)是(什么|多少|怎么样).*?"),
                    Pattern.compile("^(水温|pH|溶解氧|电导率|浊度|高锰酸盐指数|氨氮|总磷|总氮|叶绿素a|藻密度)(标准|规范)是(什么|多少|怎么样).*?"))
            ), AgentConstant.TOOL_AGENT_NAME),
    WATER_ANALYSIS("WATER_ANALYSIS",
            "诊断分析：用户描述站点上报水质数据值异常，历史告警，要求进行原因分析等,如今天站点总磷浓度严重超标，请分析一下原因",
            "涉及站点水质数据值异常分析/污染物原因分析",
            new RuleBasedRecognition.WordRegexMatcher(List.of(
                    Pattern.compile(".*?(分析|溯源)(一下)?(水温|pH|溶解氧|电导率|浊度|高锰酸盐指数|氨氮|总磷|总氮|叶绿素a|藻密度).*?(原因|异常|溯源).*?")
            )), AgentConstant.ANALYSIS_AGENT_NAME),

    CHAT("CHAT", "通用对话：其他与水质无关一般性问答",
            "涉及闲聊/其他与水资源无关的问题", new RuleBasedRecognition.KeyWordsMatcher(List.of(
            "今天天气怎么样", "你几岁了", "随便聊聊", "讲个笑话"
    )));
    private final String code;

    private final String description;

    /**
     * llm判断规则
     */
    private final String llmRule;

    /**
     * 关键词匹配 （一般來説，匹配业务明确或特定的功能的关键词）
     */
    private final RuleBasedRecognition.IntentConditionMatcher ruleMatch;

    /**
     * 意图涉及agent名称
     */
    private String agentName;

    IntentEnum(String code, String description, String llmRule, RuleBasedRecognition.IntentConditionMatcher ruleMatch) {
        this.code = code;
        this.description = description;
        this.llmRule = llmRule;
        this.ruleMatch = ruleMatch;
    }

    IntentEnum(String code, String description, String llmRule, RuleBasedRecognition.IntentConditionMatcher ruleMatch, String agentName) {
        this.code = code;
        this.description = description;
        this.llmRule = llmRule;
        this.ruleMatch = ruleMatch;
        this.agentName = agentName;
    }

    public static String buildLLMRule() {
        return Arrays.stream(IntentEnum.values())
                .map(e -> String.format("- %s，识别为：%s", e.getLlmRule(), e.getCode()))
                .collect(Collectors.joining(System.lineSeparator()));
    }

    public static String buildIntentInfo() {
        // code : description
        return Arrays.stream(IntentEnum.values())
                .map(e -> String.format("- %s:%s", e.getCode(), e.getDescription()))
                .collect(Collectors.joining(System.lineSeparator()));
    }

    public static IntentEnum fromIntent(String intent) {
        return Arrays.stream(IntentEnum.values()).filter(e -> e.code.equals(intent)).findFirst().orElse(null);
    }


    public static IntentEnum fastRecognize(String input) {
        if (StrUtil.isBlank(input)) {
            return null;
        }
        return Arrays.stream(IntentEnum.values())
                .filter(e -> e.getRuleMatch().matches(input))
                .findFirst().orElse(null);
    }

    public static void main(String[] args) {
        boolean ori = IntentEnum.WATER_ANALYSIS.getRuleMatch().matches("分析一下水温原因");
        System.out.println(ori);
    }
}
