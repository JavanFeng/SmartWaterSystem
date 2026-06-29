package com.javan.smart.water.graph.dispatcher;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.EdgeAction;
import com.javan.smart.water.common.constant.GraphConstant;
import com.javan.smart.water.common.constant.GraphRouterConstant;
import com.javan.smart.water.common.enums.IntentEnum;

/**
 * 意图分发器
 *
 * @author Javan
 * @since 1.0.0
 */
public class IntentDispatcher implements EdgeAction {

    private static final IntentDispatcher INSTANCE = new IntentDispatcher();

    @Override
    public String apply(OverAllState state) {
        String intent = state.value(GraphConstant.INTENT_TYPE, "");
        String authPass = state.value(GraphConstant.AGENT_AUTH_STATUS_KEY, GraphConstant.AGENT_AUTH_STATUS_SUCCESS_VALUE);
        if (GraphConstant.AGENT_AUTH_STATUS_FAIL_VALUE.equals(authPass)) {
            // 没权限走chat
            return GraphRouterConstant.CHAT;
        }

        IntentEnum intentEnum = IntentEnum.fromIntent(intent);
        return switch (intentEnum) {
            case WATER_ANALYSIS -> GraphRouterConstant.ANALYSIS_TASK_WAIT;
            case WATER_QA -> GraphRouterConstant.WATER_QA;
            default -> GraphRouterConstant.CHAT;
        };
    }

    public static IntentDispatcher create() {
        return INSTANCE;
    }
}
