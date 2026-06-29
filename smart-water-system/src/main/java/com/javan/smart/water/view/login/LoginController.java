package com.javan.smart.water.view.login;

import com.javan.smart.water.common.enums.RoleEnums;
import jakarta.annotation.PostConstruct;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * @author Javan
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/")
@Validated
public class LoginController {
    /**
     * 默認不過期
     */
    public static final int COOKIE_EXPIRED_TIME_SECONDS = 999999999;

    @PostConstruct
    public void initUser() {
        // admin
        generateToken("admin", "admin", "管理员", "123213", RoleEnums.ADMIN);
        // user
        generateToken("user", "user", "普通用户", "12321322", RoleEnums.VIEWER);
        // user
        generateToken("analysis", "analysis", "分析员", "12321322", RoleEnums.ANALYSIS);
    }


    private RequestToken generateToken(String uuid, String name, String realName, String mobile, RoleEnums role) {
        RequestToken token = new RequestToken();
        token.setName(name);
        token.setTokenId(uuid);
        token.setMobile(mobile);
        token.setRealName(realName);
        token.setRole(role);
        token.setRoleId(role.getRoleCode());
        token.setExpired(LocalDateTime.now().plusSeconds(COOKIE_EXPIRED_TIME_SECONDS));
        // add
        LoginUtil.addToken(uuid, token);
        return token;
    }
}
