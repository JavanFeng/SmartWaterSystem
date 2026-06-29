package com.javan.smart.water.common.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 拦截器
 */
public class McpVerificationInterceptor implements HandlerInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(McpVerificationInterceptor.class);


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader(HttpHeaders.AUTHORIZATION);
        LOG.info("获取到token:{},不进行校验，默认全放行",token);
        return true;
    }

}
