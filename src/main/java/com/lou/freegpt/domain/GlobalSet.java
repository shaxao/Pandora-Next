package com.lou.freegpt.domain;


import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 全局模型设置
 */
@Data
public class GlobalSet implements Serializable {

    // 模型集合
    private List<String> models;

    // 预设集合
    private List<String> prompt;

    // 随机性
    private double temperature;

    // 核采样
    private double topP;

    // 上下文最大token
    private int maxTokens;

    // 话题惩罚度
    private double presencePenalty;

    // 频率惩罚度
    private double frequencyPenalty;

    // 代理地址
    private String baseUrl;

    // API KEY
    private String apiKey;


}
