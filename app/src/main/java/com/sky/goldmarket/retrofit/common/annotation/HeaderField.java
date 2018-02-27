package com.sky.goldmarket.retrofit.common.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 头注解，因为不能被子类继承，所以废弃，即使设置也没有任何作用
 * [详述类的功能。]
 * Created by sky on 2017/7/18.
 */
@Target(FIELD)
@Retention(RUNTIME)
@Deprecated
public @interface HeaderField {
    String value() default "";
}
