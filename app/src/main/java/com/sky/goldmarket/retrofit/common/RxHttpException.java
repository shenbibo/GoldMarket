package com.sky.goldmarket.retrofit.common;

import com.jakewharton.retrofit2.adapter.rxjava2.HttpException;

/**
 * RxHttpException的包装类
 * Created by sky on 2017/7/25.
 */
class RxHttpException extends Exception implements ExceptionInterface {
    private HttpException httpException;

    RxHttpException(HttpException httpException) {
        // 注意当父类存在默认无参数构造器时，子类可以不调用父类的构造器方法
        super(httpException.message());
        this.httpException = httpException;
    }

    @Override
    public int code() {
        return httpException.code();
    }

    @Override
    public String message() {
        return httpException.message();
    }
}
