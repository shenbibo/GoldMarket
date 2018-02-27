package com.sky.goldmarket.retrofit.common.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * [一句话描述类的作用]
 * [详述类的功能。]
 * Created by sky on 2017/7/18.
 */
@Target(FIELD)
@Retention(RUNTIME)
public @interface QueryField {
    String value() default "";
}
