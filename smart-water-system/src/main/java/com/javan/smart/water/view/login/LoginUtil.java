package com.javan.smart.water.view.login;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Javan
 * @since 1.0.0
 */
public class LoginUtil {

    /**
     * token保存
     */
    private static final Map<String, RequestToken> TOKEN_MAP = new ConcurrentHashMap<>();


    /**
     * 获取token信息
     *
     * @param tokenId id
     * @return 结果
     */
    public static RequestToken getTokenInfoById(String tokenId) {
        return TOKEN_MAP.get(tokenId);
    }

    /**
     * 登录
     *
     * @param tokenId id
     * @param token   token
     */
    public static synchronized void addToken(String tokenId, RequestToken token) {
        // 加入新的
        TOKEN_MAP.put(tokenId, token);
    }
}
