package com.javan.smart.water.view.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author FengJ
 * @description 聊天
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatModel {

    private String input;

    private String output;

    private Boolean replay;

    public ChatModel(String input, String output) {
        this.input = input;
        this.output = output;
    }
}
