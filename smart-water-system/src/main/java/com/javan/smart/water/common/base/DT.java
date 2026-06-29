package com.javan.smart.water.common.base;

import java.util.List;

/**
 * @version 1.0
 */
public class DT {


    /**
     * success
     *
     * @return
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<T>(BaseMessageCode.SUCCESS.getCode(),
                BaseMessageCode.SUCCESS.getMessage(), data, null);
    }


    public static <T> ApiResponse<List<T>> success(List<T> data, FPageInfo pageInfo) {
        return new ApiResponse<>(BaseMessageCode.SUCCESS.getCode(),
                BaseMessageCode.SUCCESS.getMessage(), data, pageInfo);
    }


    public static <T> ApiResponse<T> success(IBaseCode msg) {
        return new ApiResponse<T>(msg.getCode(), msg.getMessage(),null,null);
    }

    /**
     * error
     *
     * @param msg 消息
     * @return 结果
     */
    public static <T> ApiResponse<T> error(IBaseCode msg) {
        return new ApiResponse<T>(msg.getCode(), msg.getMessage(),null,null);
    }

    /**
     * error
     *
     * @param msg 消息
     * @return 结果
     */
    public static <T> ApiResponse<T> error(String code ,String msg) {
        return new ApiResponse<T>(code, msg,null,null);
    }

}
