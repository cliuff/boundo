package com.madness.collision.unit.cool_app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.jsoup.Jsoup
import java.net.URL
import java.util.regex.Pattern

class CoolApp(private val packageName: String) {
    var countDownloads: Double = 0.0
    var countFlowers: Double = 0.0
    var countComments: Double = 0.0
    var logo: Bitmap? = null
    var rating: Double = 0.0
    var countRatings: Double = 0.0
    var isHealthy = false

    private fun coolURLAddress(): String = "https://www.coolapk.com/apk/$packageName"

    fun retrieve(resetOnError: Boolean = false): Boolean{
        try {
            val document = Jsoup.parse(URL(coolURLAddress()), 10000)
            val etsLogo = document.getElementsByClass("apk_topbar")
            val etsInfo = document.getElementsByClass("apk_topba_message")
            val etsRate = document.getElementsByClass("rank_num")
            val etsRates = document.getElementsByClass("apk_rank_p1")
            val logoLink = etsLogo[0].child(0).attr("src")
            val info = etsInfo[0].text() + etsRate[0].text() + etsRates[0].text()
            val counts = doubleArrayOf(0.0, 0.0, 0.0, 0.0, 0.0)
            var index = 0
            val matcher = Pattern.compile("\\d+(?:\\.\\d+)?万?").matcher(info)
            if (matcher.find()) { // clear the first one
                while (matcher.find()) {
                    val text = matcher.group()
                    if (text.endsWith("万")) {
                        counts[index] = text.substring(0, text.length - 1).toDouble() * 10000.0
                    } else {
                        counts[index] = text.toDouble()
                    }
                    index++
                }
            }
            countDownloads = counts[0]
            countFlowers = counts[1]
            countComments = counts[2]
            rating = counts[3]
            countRatings = counts[4]

            logo = BitmapFactory.decodeStream(URL(logoLink).openStream())

            isHealthy = true
        } catch (e: Exception) {
            e.printStackTrace()
            isHealthy = false
            if (resetOnError) reset()
        }
        return isHealthy
    }

    fun reset(){
        countDownloads = 0.0
        countFlowers = 0.0
        countComments = 0.0
        rating = 0.0
        countRatings = 0.0
        logo = null
    }

    fun isNotHealthy() = !isHealthy
}