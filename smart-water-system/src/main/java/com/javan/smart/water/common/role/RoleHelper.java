package com.javan.smart.water.common.role;

import com.javan.smart.water.common.util.BaseVaildedUtils;

import java.util.*;

/**
 *  简易权限校验
 * @version 1.0
 */
public class RoleHelper {

    /**
     * admin 角色才能操作
     */
    private static final List<String> URI_LIST = List.of("/api/user/add", "/api/user/update", "/api/user/delete", "/api/user/detail", "/api/user/query", "/api/report/remark");

    public static boolean isAllow4User(String uri) {
        if (BaseVaildedUtils.isEmpty(uri)) {
            return true;
        }
        return !URI_LIST.contains(uri);
    }
}
