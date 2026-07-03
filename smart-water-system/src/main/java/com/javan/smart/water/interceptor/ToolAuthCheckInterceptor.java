package com.javan.smart.water.interceptor;

import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallResponse;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolInterceptor;
import com.javan.smart.water.auth.IRbacService;
import com.javan.smart.water.common.config.UserInfo;
import com.javan.smart.water.common.config.UserInfoHolder;
import com.javan.smart.water.common.constant.CommonConstant;
import com.javan.smart.water.common.util.SpringContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author FengJ
 * @description 权限校验
 */
public class ToolAuthCheckInterceptor extends ToolInterceptor {

    private static final Logger log = LoggerFactory.getLogger(LogToolInterceptor.class);

    @Override
    public ToolCallResponse interceptToolCall(ToolCallRequest request, ToolCallHandler handler) {
        String toolName = request.getToolName();
        Object userId = request.getContext().get(CommonConstant.USER_ID);
        log.info("ToolAuthCheckInterceptor:开始校验用户[{}]是否有工具[{}]的权限", userId, toolName);
        IRbacService bean = SpringContextUtil.getBean(IRbacService.class);
        if (userId != null && bean.hasToolPermission(userId.toString(), toolName)) {
            return handler.call(request);
        } else {
            log.info("ToolAuthCheckInterceptor:用户{}没有权限调用工具{}", userId, toolName);
            return ToolCallResponse.of(request.getToolCallId(), toolName, "用户没有权限调用工具。");
        }
    }

    @Override
    public String getName() {
        return "ToolAuthCheckInterceptor";
    }
}
