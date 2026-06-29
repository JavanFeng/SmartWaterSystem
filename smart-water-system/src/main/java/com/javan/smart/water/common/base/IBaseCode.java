package com.javan.smart.water.common.base;

/**
 * @version 1.0
 */
public interface IBaseCode {
    /**
     * 获取错误码
     *
     * @return 返回错误码
     */
    String getCode();

    /**
     * 获取错误信息
     *
     * @return 获取错误信息
     */
    String getMessage();
}
