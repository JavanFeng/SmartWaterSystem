package com.javan.smart.water.common.config;

import com.alibaba.fastjson.JSON;
import com.javan.smart.water.common.base.ApiResponse;
import com.javan.smart.water.common.base.DT;
import com.javan.smart.water.common.base.ErrorCode;
import com.javan.smart.water.common.enums.RoleEnums;
import com.javan.smart.water.common.role.RoleHelper;
import com.javan.smart.water.common.util.BaseVaildedUtils;
import com.javan.smart.water.view.login.LoginUtil;
import com.javan.smart.water.view.login.RequestToken;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;

/**
 *
 * @author Javan
 * @since 1.0.0
 */
public class MemberVerificationInterceptor implements HandlerInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(MemberVerificationInterceptor.class);
    /**
     * 请求头 token
     */
    public static final String TOKEN_ID = "device-token";


    @Override
    public boolean preHandle(HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler) throws Exception {
        // 未携带tokenId
        String tokenId = request.getHeader(TOKEN_ID);
        if (BaseVaildedUtils.isEmpty(tokenId)) {
            returnErrorInfo(response, ErrorCode.UNAUTHENTICATED);
            return false;
        }

        // 获取token信息
        RequestToken token = LoginUtil.getTokenInfoById(tokenId);
        // check token
        boolean valid = checkValidateToken(token, request, response, handler);
        // 是否有权限
        boolean re = checkRoleAllows(request, response, valid, token);
        // 绑定用户信息
        bindUser(re, token);
        return re;
    }

    private void bindUser(boolean re, RequestToken token) {
        if (re) {
            String mobile = token.getMobile();
            String name = token.getName();
            String realName = token.getRealName();
            UserInfo user = new UserInfo();
            user.setMobile(mobile);
            user.setRealName(realName);
            user.setUserName(name);
            user.setRole(token.getRole());
            UserInfoHolder.bindUser(user);
        }
    }

    private boolean checkRoleAllows(HttpServletRequest request, HttpServletResponse response, boolean valid, RequestToken token) {

        if (!valid) {
            // 直接失败
            return false;
        }

        // 如果是管理员不用判断
        RoleEnums role = token.getRole();
        if (role == RoleEnums.ADMIN) {
            return valid;
        }

        // 判断是否有权限
        String servletPath = request.getServletPath();
        boolean allow4User = RoleHelper.isAllow4User(servletPath);
        if (!allow4User) {
            returnErrorInfo(response, ErrorCode.NO_ARROW);
        }


        return allow4User;
    }

    private boolean checkValidateToken(RequestToken token, HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // token 是否为空
        if (BaseVaildedUtils.isEmpty(token)) {
            returnErrorInfo(response, ErrorCode.UNAUTHENTICATED);
            return false;
        }

        // 检查是否已经过期
        if (LocalDateTime.now().isAfter(token.getExpired())) {
            returnErrorInfo(response, ErrorCode.UNAUTHENTICATED);
            return false;
        }
        return true;
    }

    private void returnErrorInfo(HttpServletResponse response, ErrorCode code) {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        ApiResponse<?> error = DT.error(code);
        PrintWriter writer = null;
        try {
            writer = response.getWriter();
            writer.write(JSON.toJSONString(error));
        } catch (IOException e) {
            LOG.warn("写错误登录验证信息失败", e);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 清空用户信息
        UserInfoHolder.unbindUser();
    }
}
