package com.javan.smart.water.common.config;

/**
 * @version 1.0
 */
public class UserInfoHolder {

    private static final ThreadLocal<UserInfo> USER_HOLDER = InheritableThreadLocal.withInitial(UserInfo::new);

    /**
     * 绑定信息
     *
     * @param info 用户
     */
    public static void bindUser(UserInfo info) {
        USER_HOLDER.set(info);
    }


    /**
     * 清除信息
     */
    public static void unbindUser() {
        USER_HOLDER.remove();
    }

    /**
     * 清除信息
     * @return 当前用户
     */
    public static UserInfo currentUser() {
        return USER_HOLDER.get();
    }
}
