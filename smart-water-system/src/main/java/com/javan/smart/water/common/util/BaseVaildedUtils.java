package com.javan.smart.water.common.util;

import java.util.Collection;
import java.util.Map;

/**
 * @version 1.0
 * @author: fengjf
 * @date: 2019/6/12 9:40
 */
public class BaseVaildedUtils {
    private BaseVaildedUtils() {
    }

    /**
     * 是否为空(支持 object,string integer... 集合类)
     *
     * @param obj 实体
     * @return 空：true 不空：false
     */
    public static boolean isEmpty(Object obj) {
        if (obj == null) {
            return true;
        }

        if (obj instanceof String) {
            return ((String) obj).isEmpty();
        }

        if (obj instanceof Collection) {
            return ((Collection) obj).isEmpty();
        }

        if (obj instanceof Map) {
            return ((Map) obj).isEmpty();
        }
        return false;
    }
}
