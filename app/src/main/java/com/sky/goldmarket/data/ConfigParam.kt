package com.sky.goldmarket.data

/**
 * 一句话描述类的作用
 * 详述类的功能。
 * Created by sky on 2018/2/28.
 */
data class ConfigParam (var buyPrice: Double,
                        var intervalTime: Long,
                        var riseThreshold: Double  /** 涨幅阈值 */)

data class ConfigParamEvent(val configParam: ConfigParam)