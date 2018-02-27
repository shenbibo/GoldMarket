package com.sky.goldmarket.retrofit.common;

import java.util.Map;

import io.reactivex.Observable;
import okhttp3.ResponseBody;
import retrofit2.http.*;

/**
 * [一句话描述类的作用]
 * [详述类的功能。]
 * Created by sky on 2017/7/3.
 */

public interface BasicService {

    /**
     * get方法
     */
    @GET
    Observable<ResponseBody> get(@Url String method,
                                 @HeaderMap Map<String, String> headerMap,
                                 @QueryMap Map<String, String> queryMap);

    /**
     * 当设置了body时，form无效，multipart无效，
     * 另外如果Body对象是RequestBody或其子类会被默认的BuiltInConverters转换器消耗
     */
    @POST
    Observable<ResponseBody> postBody(@Url String method,
                                      @HeaderMap Map<String, String> headerMap,
                                      @Body RequestBean requestBean);

    /**
     * post方法，使用表单上传请求
     */
    @FormUrlEncoded
    @POST
    Observable<ResponseBody> postForm(@Url String method,
                                      @HeaderMap Map<String, String> headerMap,
                                      @FieldMap Map<String, String> formFieldMap);
}
