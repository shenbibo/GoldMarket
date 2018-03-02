package com.sky.goldmarket.ui

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.text.SpannableStringBuilder
import com.sky.goldmarket.R
import com.sky.goldmarket.data.ConfigParam
import com.sky.goldmarket.data.ConfigParamEvent
import com.sky.goldmarket.data.Constant
import com.sky.goldmarket.service.RequestService
import com.sky.slog.LogcatTree
import com.sky.slog.Slog
import kotlinx.android.synthetic.main.activity_main.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Slog.init(LogcatTree()).simpleMode(true).defaultTag("AF")
        setContentView(R.layout.activity_main)
        initListeners()
    }

    private fun initListeners() {
        EventBus.getDefault().register(this)

        start.setOnClickListener {
            startService()
            start.isClickable = false
            stop.isClickable = true
            start.alpha = 0.3f
            stop.alpha = 1.0f
        }

        stop.setOnClickListener {
            stopService(Intent(this.applicationContext, RequestService::class.java))
            start.isClickable = true
            stop.isClickable = false
            stop.alpha = 0.3f
            start.alpha = 1.0f
        }

        refresh.setOnClickListener {
            val configData = ConfigParam(watch_price.text.toString().toDouble(),
                    buy_price.text.toString().toDouble(),
                    interval_time.text.toString().toLong(),
                    rise_threshold.text.toString().toDouble())

            EventBus.getDefault().post(ConfigParamEvent(configData))
        }
    }

    private fun startService() {
        val intent = Intent(this.applicationContext, RequestService::class.java)
        intent.putExtra(Constant.KEY_WATCH_PRICE, watch_price.text.toString().toDouble())
        intent.putExtra(Constant.KEY_BUY_PRICE, buy_price.text.toString().toDouble())
        intent.putExtra(Constant.KEY_INTERVAL_TIME, interval_time.text.toString().toLong())
        intent.putExtra(Constant.KEY_RISE_THRESHOLD, rise_threshold.text.toString().toDouble())
        startService(intent)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(msg: SpannableStringBuilder){
        cur_data.text = msg
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }

}
