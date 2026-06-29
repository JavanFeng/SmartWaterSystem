package com.javan.smart.water.common.base;

/**
 * @version 1.0
 */
public enum BaseMessageCode implements IBaseCode {
    /**
     * 基础操作返回信息
     */
    SUCCESS("SUCCESS", "成功"),
    CHECK_SUCCESS("CHECK SUCCESS", "校验参数通过"),

    SEND_SUCCESS("SEND SUCCESS", "发送成功"),

    INSERT_SUCCESS("ADDED SUCCESS", "新增成功"),

    UPDATE_SUCCESS("MODIFY SUCCESS", "修改成功"),


    DELETE_SUCCESS("DELETE SUCCESS", "刪除成功"),
    /**
     * 导入
     */
    IMPORT_SUCCESS("IMPORT SUCCESS", "导入成功"),

    /**
     * 登录
     */
    LOGIN_SUCCESS("LOGIN SUCCESS", "登录成功"),
    /**
     * 移动成功
     */
    MOVE_SUCESS("MOVE SUCCESS", " 移动成功"),
    /**
     * 初始化
     */
    INIT_SUCCESS("INIT SUCCESS", "初始化成功"),
    /**
     * 停止成功
     */
    STOP_SUCCESS("STOP SUCCESS", "停止成功"),

    /**
     * 操作成功
     */
    OPS_SUCCESS("OPS_SUCCESS", "操作成功"),

    LOAD_SUCCESS("LOAD_SUCCESS","加载成功" );

    private String message;

    private String code;


    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    BaseMessageCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
