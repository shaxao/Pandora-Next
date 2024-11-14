package com.lou.freegpt.controller;

import com.lou.freegpt.enums.AjaxResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1")
public class ModelController {

    /**
     * 获取所有模型
     * @return
     */
    @GetMapping("/models")
    public AjaxResult getModels() {
        return AjaxResult.success();
    }

    /**
     * 获取所有预置提示词
     * @return
     */
    @GetMapping("/promots")
    public AjaxResult getPromots(){
        return AjaxResult.success();
    }
}
