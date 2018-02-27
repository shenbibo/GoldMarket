package com.sky.goldmarket.retrofit.common.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 忽略不使用的字段
 * [详述类的功能。]
 * Created by sky on 2017/7/18.
 */
@Target(FIELD)
@Retention(RUNTIME)
public @interface IgnoreField {
}
