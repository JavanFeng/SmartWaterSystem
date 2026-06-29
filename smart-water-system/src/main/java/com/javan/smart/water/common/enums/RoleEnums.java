package com.javan.smart.water.common.enums;

import lombok.Getter;

/**
 * 角色
 */
@Getter
public enum RoleEnums{

    /**
     * 管理员，普通成员
     */
    ADMIN("admin", "管理员"),

    ANALYSIS("analysis", "分析员"),
    VIEWER("viewer", "用户");

    private final String roleCode;
    private final String name;

    RoleEnums(String roleCode, String name) {
        this.roleCode = roleCode;
        this.name = name;
    }

    public static RoleEnums getRoleByCode(String role) {
        for (RoleEnums value : values()) {
            if (value.roleCode.equals(role)) {
                return value;
            }
        }
        return VIEWER;
    }
}
