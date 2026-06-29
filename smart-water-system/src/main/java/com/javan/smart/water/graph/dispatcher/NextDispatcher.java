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
public class NextDispatcher implements EdgeAction {

    private static final NextDispatcher INSTANCE = new NextDispatcher();

    @Override
    public String apply(OverAllState state) {
        return state.value(GraphRouterConstant.NEXT_NODE_KEY, GraphRouterConstant.CHAT);
    }

    public static NextDispatcher create() {
        return INSTANCE;
    }
}
