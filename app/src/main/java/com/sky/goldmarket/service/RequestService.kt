package com.sky.goldmarket.service

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.sky.goldmarket.data.ConfigParam
import com.sky.goldmarket.data.ConfigParamEvent
import com.sky.goldmarket.data.Constant
import com.sky.goldmarket.data.RequestGoldBean
import com.sky.goldmarket.retrofit.common.HttpUtils
import com.sky.goldmarket.retrofit.common.HttpUtils.get
import com.sky.goldmarket.retrofit.common.SimpleHttpCallback
import com.sky.slog.Slog
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * 一句话描述类的作用
 * 详述类的功能。
 * Created by sky on 2018/2/27.
 */
class RequestService : Service() {
    private var isDestroyed = false
    private var configParam = ConfigParam(0.0, 60, 0.5)

    override fun onCreate() {
        super.onCreate()
        HttpUtils.init("http://www.icbc.com.cn/ICBCDynamicSite/Charts/GoldTendencyPicture.aspx/")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            configParam.buyPrice = intent.getDoubleExtra(Constant.KEY_BUY_PRICE, 0.0)
            configParam.intervalTime = intent.getLongExtra(Constant.KEY_INTERVAL_TIME, 60)
            configParam.riseThreshold = intent.getDoubleExtra(Constant.KEY_RISE_THRESHOLD, 0.5)
        }

        startRequest()
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startRequest() {
        requestGoldData()
        requestDelay()
    }

    private fun requestDelay() {
        Handler(Looper.getMainLooper()).postDelayed(
                {
                    requestGoldData()
                    if (!isDestroyed) {
                        requestDelay()
                    }
                },
                configParam.intervalTime * 1000)
    }

    private fun requestGoldData() {
        get(RequestGoldBean(), object : SimpleHttpCallback<String>() {
            override fun onSuccess(s: String) {
                if (isDestroyed) {
                    return
                }

                Slog.i(s)
                parseResponseHtmlString(s)
                if (shouldNotifyToUser()) {
                    notifyToUser()
                }
            }

            override fun onFailure(e: Throwable) {
                Slog.e(e)
            }
        })
    }

    private fun parseResponseHtmlString(htmlString: String) {}

    private fun shouldNotifyToUser(): Boolean {
        return true
    }

    private fun notifyToUser() {}

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(configParamEvent: ConfigParamEvent) {
        configParam = configParamEvent.configParam
    }

    override fun onDestroy() {
        super.onDestroy()
        isDestroyed = true
    }
}