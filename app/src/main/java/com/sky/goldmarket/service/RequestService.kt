package com.sky.goldmarket.service

import android.annotation.TargetApi
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.sky.goldmarket.R
import com.sky.goldmarket.data.*
import com.sky.goldmarket.retrofit.common.HttpUtils
import com.sky.goldmarket.retrofit.common.HttpUtils.get
import com.sky.goldmarket.retrofit.common.SimpleHttpCallback
import com.sky.slog.Slog
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.jsoup.Jsoup


/**
 * 一句话描述类的作用
 * 详述类的功能。
 * Created by sky on 2018/2/27.
 */
@TargetApi(Build.VERSION_CODES.O)
class RequestService : Service() {
    private val foregroundChannelId = "2018"
    private val goldChannelId = "2019"
    private val foregroundChannel = NotificationChannel(foregroundChannelId,
                                                        "前台", NotificationManager.IMPORTANCE_HIGH)
    private val goldChannel = NotificationChannel(goldChannelId,
                                                  "前台", NotificationManager.IMPORTANCE_HIGH)

    private var isDestroyed = false
    private var configParam = ConfigParam(0.0, 0.0, 60, 0.5)

    override fun onCreate() {
        super.onCreate()
        HttpUtils.init("http://www.icbc.com.cn/ICBCDynamicSite/Charts/GoldTendencyPicture.aspx/")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            configParam.watchPrice = intent.getDoubleExtra(Constant.KEY_WATCH_PRICE, 0.0)
            configParam.buyPrice = intent.getDoubleExtra(Constant.KEY_BUY_PRICE, 0.0)
            configParam.intervalTime = intent.getLongExtra(Constant.KEY_INTERVAL_TIME, 60)
            configParam.riseThreshold = intent.getDoubleExtra(Constant.KEY_RISE_THRESHOLD, 0.5)
        }

        setServiceForeground()
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

                val au9999Bean = parseResponseHtmlString(s)
                if (shouldNotifyToUser(au9999Bean.curPrice)) {
                    notifyToUser(au9999Bean)
                }
            }

            override fun onFailure(e: Throwable) {
                Slog.e(e)
            }
        })
    }

    private fun parseResponseHtmlString(htmlString: String): Au9999Bean {
        val doc = Jsoup.parse(htmlString)
        val table2 = doc.getElementById("TABLE2")
        val tBody = table2.child(0)
        val au9999 = tBody.child(2)
        val auBean = Au9999Bean(au9999.child(0).text(),
                                au9999.child(1).text().toDouble(),
                                au9999.child(3).text(),
                                au9999.child(5).text().toDouble(),
                                au9999.child(6).text().toDouble(),
                                au9999.child(7).text().toDouble(),
                                au9999.child(8).text().toDouble(),
                                au9999.child(9).text())

        Slog.i(auBean.toString())
        return auBean
    }

    /**
     * 当前价格差的幅度大于等于设置的涨幅值则返回true，或者当前的成交价格低于设定的阈值
     * */
    private fun shouldNotifyToUser(curPrice: Double): Boolean {
        return (configParam.buyPrice == 0.0 && (curPrice <= configParam.watchPrice))
                || (getRisePercent(curPrice) >= configParam.riseThreshold)
    }

    private fun getRisePercent(curPrice: Double) = (curPrice - configParam.buyPrice) / 100.0

    private fun notifyToUser(au9999Bean: Au9999Bean) {
        val notification = Notification.Builder(applicationContext, foregroundChannelId)
                .setContentText("cur: " + au9999Bean.curPrice + " buy: " + configParam.buyPrice + '\n'
                                        + "watch: " + configParam.watchPrice
                                        + " rise: " + getRisePercent(au9999Bean.curPrice))
                .setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))
                .setSmallIcon(R.mipmap.ic_launcher)
                .build()
        startForeground(2018, notification)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(configParamEvent: ConfigParamEvent) {
        configParam = configParamEvent.configParam
    }

    private fun setServiceForeground() {
        val notifyManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notifyManager.createNotificationChannel(foregroundChannel)
//        notifyManager.createNotificationChannel(goldChannel)
        val notification = Notification.Builder(applicationContext, foregroundChannelId)
                .setContentText("gold market is running")
                .setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))
                .setSmallIcon(R.mipmap.ic_launcher)
                .build()
        startForeground(2018, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(true)
        isDestroyed = true
    }
}