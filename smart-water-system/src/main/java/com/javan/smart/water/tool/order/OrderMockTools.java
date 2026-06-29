package com.javan.smart.water.tool.order;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.EvictingQueue;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 模拟工单系统调用
 *
 * @author FengJ
 * @description 模拟工单系统调用
 */
@Getter
@Component
@Slf4j
public class OrderMockTools {

    private final EvictingQueue<Order> ORDER_HANDLE = EvictingQueue.create(20);

    @Tool(description = "创建水质污染处理的工单。返回数据：工单编号（orderCode）、派发对象（handler）、 紧急程度（urgent）、行动指令（action）")
    public String createOrder(@ToolParam(description = "工单标题。格式统一为“【异常类型】+ 监测点位 + 核心事件”（例如：【人工指派】A河段断面周边可疑排污口排查）") String title,
                              @ToolParam(description = "处置对象。如：杭州西湖XH站点") String target,
                              @ToolParam(description = "行动指令。将人工的自然语言指令转化为一线人员能看懂的短句") String action,
                              @ToolParam(required = false, description = "紧急程度。人工明确指定则遵从指定；否则根据风险描述自动定级（涉及有毒物质、严重超标或执法建议定为“高”；常规指标波动定为“中”") String urgent,
                              @ToolParam(required = false, description = "处理人") String handler,
                              @ToolParam(required = false, description = "备注") String remark) {
        Order order = new Order(UUID.randomUUID().toString(), title, target, action, urgent, remark, LocalDateTime.now(), handler == null ? "李明" : handler);
        ORDER_HANDLE.add(order);
        return JSON.toJSONString(order);
    }


    public record Order(String orderCode,
                        String title,
                        String target,
                        String action,
                        String urgent,
                        String remark,
                        LocalDateTime createTime,
                        String handler) {
    }
}
