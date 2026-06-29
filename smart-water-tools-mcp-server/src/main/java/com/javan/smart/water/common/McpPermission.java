package com.javan.smart.water.common;

import com.javan.smart.water.common.role.RoleEnums;

import java.lang.annotation.*;

/**
 * @author FengJ
 * @description 简单权限校验
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface McpPermission {
    // 默认都可以访问
    RoleEnums[] alc() default {};
}
