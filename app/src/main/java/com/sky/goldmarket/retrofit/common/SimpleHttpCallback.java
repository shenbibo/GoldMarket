package com.sky.goldmarket.retrofit.common;


public abstract class SimpleHttpCallback<T> implements HttpCallback<T>{
    @Override
    public void onStart(Cancellable cancellable){}

    @Override
    public void onFailure(Throwable e){}
}
