package com.javan.smart.water.auth;

import cn.hutool.core.util.StrUtil;
import com.javan.smart.water.common.constant.AgentConstant;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认 简易
 *
 * @author Javan
 * @since 1.0.0
 */
@Service
public class DefaultRbacService implements IRbacService {
    private static final String ADMIN_ROLE = "admin";
    private static final String AN_ROLE = "analysis";
    private static final String VIEW_ROLE = "viewer";
    private static final String DEFAULT_ROLE = VIEW_ROLE;

    private static final Map<String, Set<String>> ROLE_PREFIX_TOOL_PERMISSIONS = new ConcurrentHashMap<>();
    private static final Map<String, Set<String>> ROLE_AGENT_PERMISSIONS = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> userRoles = new ConcurrentHashMap<>();

    static {
        ROLE_AGENT_PERMISSIONS.put(ADMIN_ROLE, Set.of(AgentConstant.ANALYSIS_AGENT_NAME, AgentConstant.WORK_AGENT_NAME, AgentConstant.TOOL_AGENT_NAME));
        ROLE_AGENT_PERMISSIONS.put(AN_ROLE, Set.of(AgentConstant.ANALYSIS_AGENT_NAME, AgentConstant.TOOL_AGENT_NAME));
        ROLE_AGENT_PERMISSIONS.put(VIEW_ROLE, Set.of(AgentConstant.TOOL_AGENT_NAME));

        // simple name filter
        ROLE_PREFIX_TOOL_PERMISSIONS.put(ADMIN_ROLE, Set.of("*"));
        ROLE_PREFIX_TOOL_PERMISSIONS.put(AN_ROLE, Set.of("view", "search"));
        ROLE_PREFIX_TOOL_PERMISSIONS.put(VIEW_ROLE, Set.of("viewPub", "searchPub"));

    }

    public DefaultRbacService() {
        userRoles.put("admin", Set.of(ADMIN_ROLE));
        userRoles.put("analysis", Set.of(AN_ROLE));
        userRoles.put("viewer", Set.of(VIEW_ROLE));
    }


    @Override
    public boolean hasAgentPermission(String userId, String agentName) {
        if (StrUtil.isBlank(agentName)) {
            return true;
        }
        Set<String> roles = userRoles.getOrDefault(userId, Set.of(DEFAULT_ROLE));
        return roles.stream()
                .anyMatch(role -> ROLE_AGENT_PERMISSIONS.getOrDefault(role, Set.of()).contains(agentName));
    }

    @Override
    public boolean hasToolPermission(String userId, String toolName) {
        Set<String> roles = userRoles.getOrDefault(userId, Set.of(DEFAULT_ROLE));
        // *
        boolean all = roles.stream()
                .anyMatch(role -> ROLE_PREFIX_TOOL_PERMISSIONS.getOrDefault(role, Set.of()).contains("*"));
        if (all) {
            return true;
        }
        // 前缀匹配
        return roles.stream()
                .anyMatch(role -> ROLE_PREFIX_TOOL_PERMISSIONS.getOrDefault(role, Set.of())
                        .stream().anyMatch(prefix -> toolName.toLowerCase().startsWith(prefix.toLowerCase())));
    }

    @Override
    public boolean isAdmin(String userId) {
        return userRoles.getOrDefault(userId, Set.of(DEFAULT_ROLE)).contains(ADMIN_ROLE);
    }
}
