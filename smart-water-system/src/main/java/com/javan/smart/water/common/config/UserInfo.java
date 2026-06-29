package com.javan.smart.water.common.config;

import com.javan.smart.water.common.enums.RoleEnums;

/**
 * @version 1.0
 * @author: Javan
 */
public class UserInfo {
    /** 用户名*/
    private String userName;
    /** 手机号*/
    private String mobile;
    /** 真实姓名*/
    private String realName;
    /** 权限*/
    private RoleEnums role;

    /**
     * Gets the value of role.
     *
     * @return the value of role
     */
    public RoleEnums getRole() {
        return role;
    }

    /**
     * Sets the role.
     *
     * <p>You can use getRole() to get the value of role</p>
     *
     * @param role role
     */
    public void setRole(RoleEnums role) {
        this.role = role;
    }

    /**
     * Gets the value of userName.
     *
     * @return the value of userName
     */
    public String getUserName() {
        return userName;
    }

    /**
     * Sets the userName.
     *
     * <p>You can use getUserName() to get the value of userName</p>
     *
     * @param userName userName
     */
    public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * Gets the value of mobile.
     *
     * @return the value of mobile
     */
    public String getMobile() {
        return mobile;
    }

    /**
     * Sets the mobile.
     *
     * <p>You can use getMobile() to get the value of mobile</p>
     *
     * @param mobile mobile
     */
    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    /**
     * Gets the value of realName.
     *
     * @return the value of realName
     */
    public String getRealName() {
        return realName;
    }

    /**
     * Sets the realName.
     *
     * <p>You can use getRealName() to get the value of realName</p>
     *
     * @param realName realName
     */
    public void setRealName(String realName) {
        this.realName = realName;
    }
}
