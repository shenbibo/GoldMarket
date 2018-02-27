package com.sky.goldmarket.retrofit.common;

import android.support.annotation.Nullable;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jakewharton.retrofit2.adapter.rxjava2.HttpException;
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import com.sky.goldmarket.retrofit.common.annotation.FormField;
import com.sky.goldmarket.retrofit.common.annotation.HeaderField;
import com.sky.goldmarket.retrofit.common.annotation.IgnoreField;
import com.sky.goldmarket.retrofit.common.annotation.QueryField;
import com.sky.slog.Slog;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.*;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * http请求工具类
 * [详述类的功能。]
 * Created by sky on 2017/7/4.
 */
// TODO 如何解决某些请求需要不同的超时的情况，是否考虑使用Build模式来解决
public class HttpUtils {
    public static final String TAG = "RetrofitHttpUtils";
    private static final HttpRequestImpl REQUEST = new HttpRequestImpl();

    public static void init(String baseUrl) {
        init(baseUrl, getDefaultOkHttpClient());
    }

    public static void init(String baseUrl, @Nullable OkHttpClient okHttpClient) {
        REQUEST.init(baseUrl, okHttpClient);
    }

    public static void setBaseUrl(String baseUrl) {
        REQUEST.setBaseUrl(baseUrl);
    }

    public static void setOkHttpClient(OkHttpClient okHttpClient) {
        REQUEST.setOkHttpClient(okHttpClient);
    }

    public static OkHttpClient.Builder getOkHttpBuilder() {
        return REQUEST.getOkHttpBuilder();
    }

    public static Retrofit.Builder getRetrofitBuilder() {
        return REQUEST.getRetrofitBuilder();
    }

    public static <T> void get(RequestBean requestBean, HttpCallback<T> httpCallback) {
        REQUEST.get(requestBean, httpCallback);
    }

    public static <T> void postBody(RequestBean requestBean, HttpCallback<T> httpCallback) {
        REQUEST.postBody(requestBean, httpCallback);
    }

    public static <T> void postForm(RequestBean requestBean, HttpCallback<T> httpCallback) {
        REQUEST.postForm(requestBean, httpCallback);
    }

    private static OkHttpClient getDefaultOkHttpClient() {
        return new OkHttpClient().newBuilder()
                                 .addInterceptor(new TestInterceptor())
                                 .connectTimeout(15, TimeUnit.SECONDS)
                                 .readTimeout(15, TimeUnit.SECONDS)
                                 .writeTimeout(15, TimeUnit.SECONDS)
                                 .build();
    }

    private static Retrofit getDefaultRetrofit(String baseUrl, OkHttpClient okHttpClient) {
        return new Retrofit.Builder().baseUrl(baseUrl)
                                     .client(okHttpClient)
                                     .addConverterFactory(GsonConverterFactory.create(createGson()))
                                     .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                                     .build();
    }

    private static <T> void request(Observable<ResponseBody> observable, HttpCallback<T> httpCallback) {
        // 直接启动子线程执行
        Observable.create(e -> {
            Class<?> beanClass = getHttpCallbackParamsType(httpCallback);
            requestAndParseResponse(httpCallback, observable, beanClass);
        }).subscribeOn(Schedulers.io()).subscribe();
    }

    private static <T> void requestAndParseResponse(final HttpCallback<T> httpCallback,
        Observable<ResponseBody> getObservable,
        Class<?> beanClass) {
        getObservable.subscribeOn(Schedulers.io())
                     .unsubscribeOn(Schedulers.io())
                     .map(responseBody -> {
                         Slog.t(TAG).d("have receiver the response body");
                         Slog.t(TAG).d("beanclass = " + beanClass.getSimpleName() + ", rClass = " + responseBody
                             .getClass().getSimpleName());
                         if (beanClass.isAssignableFrom(responseBody.getClass())) {
                             //noinspection unchecked
                             return (T) responseBody;
                         }

                         if (beanClass == String.class) {
                             return (T) responseBody.string();
                         }

                         // TODO 后续需要根据contentType(MediaType)来区分使用什么来解析responseBody
                         Gson gson = new Gson();
                         return (T) (gson.fromJson(responseBody.string(), beanClass));
                     })
                     .observeOn(AndroidSchedulers.mainThread())
                     .subscribe(new Observer<T>() {
                         @Override
                         public void onSubscribe(Disposable d) {
                             httpCallback.onStart(new RxCancellable(d));
                         }

                         @Override
                         public void onNext(T t) {
                             httpCallback.onSuccess(t);
                         }

                         @Override
                         public void onError(Throwable e) {
                             if (e instanceof HttpException) {
                                 e = new RxHttpException((HttpException) e);
                             }
                             httpCallback.onFailure(e);
                         }

                         @Override
                         public void onComplete() {
                             Slog.t(TAG).i("REQUEST complete");
                         }
                     });
    }

    private static <T> Class<?> getHttpCallbackParamsType(HttpCallback<T> httpCallback) {
        Class<?> beanClass;
        ParameterizedType type = (ParameterizedType) httpCallback.getClass().getGenericSuperclass();
        beanClass = (Class<?>) type.getActualTypeArguments()[0];
        return beanClass;
    }


    public static class TestInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            Slog.t(TAG).i(request.toString());
            Slog.t(TAG).i(request.headers().toString());
            if (request.body() != null) {
                Slog.t(TAG).i(request.body().toString());
                if (request.body() instanceof FormBody) {
                    Slog.t(TAG).i("isFormBody");
                }
            }

            Response response = chain.proceed(request);
            Slog.t(TAG).i(response.toString());
            return response;
        }
    }

    private static Gson createGson() {
        return new GsonBuilder().setExclusionStrategies(new GsonExclusionStrategy()).create();
    }

    private static class GsonExclusionStrategy implements ExclusionStrategy {
        private static final List<Class<? extends Annotation>> EXCLUDE_ANNOTATIONS =
            Arrays.asList(IgnoreField.class, FormField.class, HeaderField.class, QueryField.class);

        private static final List<String> EXCLUDE_FIELD_NAME =
            Arrays.asList("method", "headerMap");

        @Override
        public boolean shouldSkipField(FieldAttributes f) {
            // 忽略指定字段名称的字段
            if (EXCLUDE_FIELD_NAME.contains(f.getName())) {
                return true;
            }

            Collection<? extends Annotation> annotations = f.getAnnotations();
            for (Annotation annotation : annotations) {
                if (EXCLUDE_ANNOTATIONS.contains(annotation.getClass())) {
                    return true;
                }
            }

            return false;
        }

        @Override
        public boolean shouldSkipClass(Class<?> clazz) {
            return false;
        }
    }

    public static class HttpRequestImpl implements HttpRequest {
        private String baseUrl;
        private Retrofit retrofit;
        private OkHttpClient okHttpClient;

        HttpRequestImpl() {
        }

        HttpRequestImpl(Builder builder) {
            okHttpClient = builder.okHttpBuilder != null ? builder.okHttpBuilder.build() : null;

            retrofit = okHttpClient != null
                ? builder.retrofitBuilder.client(okHttpClient).build()
                : builder.retrofitBuilder.build();
        }

        @Override
        public <T> void get(RequestBean requestBean, HttpCallback<T> httpCallback) {
            request(createGetObservable(requestBean), httpCallback);
        }

        @Override
        public <T> void postBody(RequestBean requestBean, HttpCallback<T> httpCallback) {
            request(createPostBodyObservable(requestBean), httpCallback);
        }

        @Override
        public <T> void postForm(RequestBean requestBean, HttpCallback<T> httpCallback) {
            request(createPostFormObservable(requestBean), httpCallback);
        }

        void init(String baseUrl, OkHttpClient okHttpClient) {
            this.baseUrl = baseUrl;
            this.okHttpClient = okHttpClient == null ? getDefaultOkHttpClient() : okHttpClient;
            this.retrofit = getDefaultRetrofit(this.baseUrl, this.okHttpClient);
        }

        void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            retrofit = retrofit.newBuilder().baseUrl(baseUrl).build();
        }

        void setOkHttpClient(OkHttpClient okHttpClient) {
            this.okHttpClient = okHttpClient;
            retrofit = retrofit.newBuilder().client(okHttpClient).build();
        }

        OkHttpClient.Builder getOkHttpBuilder() {
            return okHttpClient.newBuilder();
        }

        Retrofit.Builder getRetrofitBuilder() {
            return retrofit.newBuilder();
        }

        Builder newBuilder() {
            return new Builder(this);
        }

        private Observable<ResponseBody> createGetObservable(RequestBean requestBean) {
            return getService().get(requestBean.getMethod(),
                requestBean.getHeaders(),
                FieldUtils.parseFields(requestBean, QueryField.class));
        }

        private Observable<ResponseBody> createPostBodyObservable(RequestBean requestBean) {
            return getService().postBody(requestBean.getMethod(),
                requestBean.getHeaders(),
                requestBean);
        }

        private Observable<ResponseBody> createPostFormObservable(RequestBean requestBean) {
            return getService().postForm(requestBean.getMethod(),
                requestBean.getHeaders(),
                FieldUtils.parseFields(requestBean, FormField.class));
        }

        private BasicService getService() {
            return retrofit.create(BasicService.class);
        }
    }


    public static Builder newBuilder() {
        return REQUEST.newBuilder();
    }

    public static class Builder {
        private OkHttpClient.Builder okHttpBuilder;
        private Retrofit.Builder retrofitBuilder;

        Builder(HttpRequestImpl httpRequest) {
            //            okHttpBuilder = httpRequest.okHttpClient.newBuilder();
            retrofitBuilder = httpRequest.retrofit.newBuilder();
        }

        /**
         * 要设置的builder如果需要使用全局的okhttp的配置，则可以从HttpUtils.getOkHttpBuilder获取之后修改的
         * 如果不需要，也可以使用自定义生成的。
         * // TODO 使用自定义的，这样就可能导致一些公共的设置项没有了，比如缓存，公共的header等。
         */
        public Builder okHttpBuilder(OkHttpClient.Builder builder) {
            okHttpBuilder = builder;
            return this;
        }

        /**
         * 要设置的builder必要来自HttpUtils.getRetrofitBuilder获取之后修改的，否则可能出错
         */
        public Builder retrofitBuilder(Retrofit.Builder builder) {
            retrofitBuilder = builder;
            return this;
        }

        public HttpRequest create() {
            return new HttpRequestImpl(this);
        }
    }

    //    // TODO 后面考虑使用统一的一个工具类来管理取消网络请求操作，如RxLifecycle框架
    //    // TODO 或者将onStart方法的调用不关注其是在哪个线程调用的，曲调切换到主线程的步骤
    //    private static <T> void startOnMainThread(HttpCallback<T> httpCallback,
    //    Observable<ResponseBody> getObservable, Class<?> beanClass) {
    //        // 强制切换到主线程发起，保证onSubscribe回调也在主线程运行
    //        Observable.create(e -> requestAndParseResponse(httpCallback, getObservable, beanClass))
    //                  .subscribeOn(AndroidSchedulers.mainThread())
    //                  .subscribe();
    //    }
}
