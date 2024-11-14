package com.lou.freegpt.common.anntation;

import com.lou.freegpt.enums.BusinessType;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LogInterface {

    /**
     * 标题
     * @return
     */
    String title() default "";

    /**
     * 功能
     * @return
     */
    BusinessType businessType() default BusinessType.OTHER;

    /**
     * 是否保存请求的参数
     */
    boolean isSaveRequestData() default true;
}
