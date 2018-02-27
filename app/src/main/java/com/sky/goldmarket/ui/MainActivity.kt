package com.sky.goldmarket.ui

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.sky.goldmarket.R
import com.sky.goldmarket.data.Constant
import com.sky.goldmarket.service.RequestService
import com.sky.slog.LogcatTree
import com.sky.slog.Slog
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Slog.init(LogcatTree()).simpleMode(true).defaultTag("AF")
        setContentView(R.layout.activity_main)
        initListeners()
    }

    private fun initListeners() {
        start.setOnClickListener {
            startService()
            start.isClickable = false
            stop.isClickable = true
        }

        stop.setOnClickListener {
            stopService(Intent(this.applicationContext, RequestService::class.java))
            start.isClickable = true
            stop.isClickable = false
        }
    }

    private fun startService() {
        val intent = Intent(this.applicationContext, RequestService::class.java)
        intent.putExtra(Constant.KEY_BUY_PRICE, buy_price.text.toString().toDouble())
        intent.putExtra(Constant.KEY_INTERVAL_TIME, interval_time.text.toString().toLong())
        intent.putExtra(Constant.KEY_RISE_THRESHOLD, rise_threshold.text.toString().toDouble())
        startService(intent)
    }

}
