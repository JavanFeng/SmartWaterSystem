package com.javan.smart.water.auth;

/**
 * @author FengJ
 * @description 权限类
 */
public interface IRbacService {

    default boolean hasAgentPermission(String userId, String agentName) {
        return true;
    }

    default boolean hasToolPermission(String userId, String toolName) {
        return true;
    }

    default boolean isAdmin(String userId) {
        return false;
    }

}
