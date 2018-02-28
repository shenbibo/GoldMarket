package com.sky.goldmarket.data

/**
 * 一句话描述类的作用
 * 详述类的功能。
 * Created by sky on 2018/2/28.
 */
data class Au9999Bean(val name: String,
                        val curPrice: Double,
                        val priceLimit: String, /** 涨跌幅*/
                        val startPrice:Double,
                        val yesterdayPrice:Double,
                        val highestPrice:Double,
                        val lowestPrice:Double,
                        val updateTime:String)