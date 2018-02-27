package com.sky.goldmarket.retrofit.common;

import io.reactivex.disposables.Disposable;

/**
 * RxDisposable的包装类
 * Created by sky on 2017/7/25.
 */
class RxCancellable implements Cancellable {
    private Disposable disposable;

    RxCancellable(Disposable disposable) {
        this.disposable = disposable;
    }

    @Override
    public void cancel() {
        disposable.dispose();
    }

    @Override
    public boolean isCancelled() {
        return disposable.isDisposed();
    }
}
