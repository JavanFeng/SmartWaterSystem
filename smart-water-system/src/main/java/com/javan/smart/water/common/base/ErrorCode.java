package com.javan.smart.water.common.base;

/**
 */
public enum ErrorCode implements IBaseCode {

    /**
     * 服务端错误
     */
    API_ERROR("API_ERROR", "服务端错误"),

    /**
     * 未登录或授权过期
     */
    UNAUTHENTICATED("UNAUTHENTICATED", "未登录或授权过期"),


    /**
     * 没有权限
     */
    NO_ARROW("NO_ARROW", "没有权限");

    private String code;
    private String message;


    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

}