package com.sky.goldmarket.service

import android.annotation.TargetApi
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import com.sky.goldmarket.R
import com.sky.goldmarket.data.*
import com.sky.goldmarket.retrofit.common.HttpUtils
import com.sky.goldmarket.retrofit.common.HttpUtils.get
import com.sky.goldmarket.retrofit.common.SimpleHttpCallback
import com.sky.slog.Slog
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.jsoup.Jsoup
import java.text.DecimalFormat


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
    private val handler = Handler(Looper.getMainLooper())
    private var isNewRefresh = true

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

        EventBus.getDefault().register(this)

        setServiceForeground()
        isNewRefresh = true
        startRequest()
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startRequest() {
        requestGoldData()
        requestDelay(configParam.intervalTime)
    }

    private fun requestDelay(time: Long) {
        handler.postDelayed(
                {
                    requestGoldData()
                    if (!isDestroyed) {
                        requestDelay(configParam.intervalTime)
                    }
                },
                time * 1000)
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
        val shouldNotify = isNewRefresh
                || (configParam.buyPrice == 0.0 && (curPrice <= configParam.watchPrice))
                || (getRisePercent(curPrice) >= configParam.riseThreshold)
        isNewRefresh = false

        return shouldNotify
    }

    private fun getRisePercent(curPrice: Double) = (curPrice - configParam.buyPrice) / configParam.buyPrice

    private fun notifyToUser(au9999Bean: Au9999Bean) {
        val content = createNotificationContent(au9999Bean)
        val notification = Notification.Builder(applicationContext, foregroundChannelId)
                .setContentText(content)
                //.setLargeIcon(Icon.createWithResource(applicationContext, R.drawable.large_icon_round))
                .setSmallIcon(R.drawable.small_icon_round)
                .build()

        startForeground(2018, notification)
    }

    private fun createNotificationContent(au9999Bean: Au9999Bean): SpannableStringBuilder {
        val risePercent = getRisePercent(au9999Bean.curPrice)
        val ssb = SpannableStringBuilder()
        val curPrice = SpannableString(au9999Bean.curPrice.toString())
        val buyPrice = SpannableString(configParam.buyPrice.toString())
        val watchPrice = SpannableString(configParam.watchPrice.toString())
        val risePercentStr = SpannableString(formatDouble4(risePercent))

        val curPriceColor = if (au9999Bean.curPrice > configParam.buyPrice) {
            Color.RED
        } else {
            Color.GREEN
        }
        val buyPriceColor = Color.BLUE
        val watchPriceColor = Color.BLUE
        val risePercentColor = if (risePercent > 0) {
            Color.RED
        } else {
            Color.GREEN
        }

        val curPriceColorSpan = ForegroundColorSpan(curPriceColor)
        val buyPriceColorSpan = ForegroundColorSpan(buyPriceColor)
        val watchPriceColorSpan = ForegroundColorSpan(watchPriceColor)
        val risePercentColorSpan = ForegroundColorSpan(risePercentColor)

        curPrice.setSpan(curPriceColorSpan, 0, curPrice.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        buyPrice.setSpan(buyPriceColorSpan, 0, buyPrice.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        watchPrice.setSpan(watchPriceColorSpan, 0, watchPrice.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        risePercentStr.setSpan(risePercentColorSpan, 0, risePercentStr.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        ssb.append("c:")
                .append(curPrice)
                .append(" b:")
                .append(buyPrice)
                .append(" w:")
                .append(watchPrice)
                .append(" r:")
                .append(risePercentStr)

        return ssb
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(configParamEvent: ConfigParamEvent) {
        configParam = configParamEvent.configParam
        handler.removeCallbacksAndMessages(null)
        isNewRefresh = true
        requestDelay(0)
    }

    private fun setServiceForeground() {
        val notifyManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notifyManager.createNotificationChannel(foregroundChannel)
        val notification = Notification.Builder(applicationContext, foregroundChannelId)
                .setContentText("gold market is running")
                .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.large_icon_round))
                .setSmallIcon(R.drawable.small_icon_round)
                .build()
        startForeground(2018, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
        handler.removeCallbacksAndMessages(null)
        stopForeground(true)
        isDestroyed = true
    }

    /**
     * 这个方法挺简单的。
     * DecimalFormat is a concrete subclass of NumberFormat that formats decimal numbers.
     * @param d
     * @return
     */
    private fun formatDouble4(d: Double): String {
        return String.format("%.4f", d)
    }
}