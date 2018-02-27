package com.sky.goldmarket.retrofit.common;

/**
 * 网络请求接口方法
 * 详述类的功能。
 * Created by sky on 2017/7/26.
 */
public interface HttpRequest {
    <T> void get(RequestBean requestBean, HttpCallback<T> httpCallback);

    <T> void postBody(RequestBean requestBean, HttpCallback<T> httpCallback);

    <T> void postForm(RequestBean requestBean, HttpCallback<T> httpCallback);
}
