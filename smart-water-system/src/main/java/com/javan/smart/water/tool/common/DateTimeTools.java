package com.javan.smart.water.tool.common;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DateTimeTools {

    public static final String TOOL_GET_CURRENT_DATETIME = "getCurrentDateTime";

    @Tool(name = TOOL_GET_CURRENT_DATETIME, description = "获取当前日期时间")
    public String getCurrentDateTime() {
        return LocalDateTime.now().atZone(LocaleContextHolder.getTimeZone().toZoneId()).toString();
    }
}
