package com.javan.smart.water.view.login;

import com.javan.smart.water.common.enums.RoleEnums;
import lombok.Data;

import java.time.LocalDateTime;

/**
 *
 * @author Javan
 * @since 1.0.0
 */
@Data
public class RequestToken {
    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 手机号
     */
    private String mobile;

    /**
     * 角色
     */
    private RoleEnums role;

    private String roleId;

    /**
     * 用户名
     */
    private String name;
    /**
     * token的值
     */
    private String tokenId;
    /**
     * 过期时间
     */
    private LocalDateTime expired;
}
