package com.sky.goldmarket.retrofit.common;

import java.util.IdentityHashMap;

/**
 * JavaBean请求的抽象基类
 * [详述类的功能。]
 * Created by sky on 2017/7/4.
 */
public abstract class RequestBean {
    protected String method;

    protected final IdentityHashMap<String, String> headerMap = new IdentityHashMap<>();

    public RequestBean(){
        modifyHeaders(headerMap);
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    protected void modifyHeaders(IdentityHashMap<String, String> headerMap){}

    /**
     * 该方法返回一个可重复的请求头键值对，注意如果想添加重复的键值对，不能使用常量，需要new一个新的String对象。
     * 子类可以复写该方法，如果其对请求头有特殊要求的话。
     * */
    public IdentityHashMap<String, String> getHeaders(){
        return headerMap;
    }
}
