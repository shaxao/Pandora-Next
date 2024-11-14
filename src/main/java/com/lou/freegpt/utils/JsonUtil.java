package com.lou.freegpt.utils;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.lou.freegpt.vo.MessageVo;

/**
 * 处理json数据
 */
public class JsonUtil {
    private JSONObject parseGlobalSettings(String globalSet) {
        return JSONUtil.parseObj(globalSet);
    }

    private JSONObject buildRequestJSON(MessageVo messageVo, String escapedContent, String apiKey) {
        // 根据条件构建JSON（类似于原方法中的实现）
        return null;
    }

}
