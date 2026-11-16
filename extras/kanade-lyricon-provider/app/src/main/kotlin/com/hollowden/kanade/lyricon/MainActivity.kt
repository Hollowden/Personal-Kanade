package com.hollowden.kanade.lyricon

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val density = resources.displayMetrics.density
        fun dp(value: Int): Int = (value * density + 0.5f).toInt()

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(32), dp(24), dp(32))
        }

        content.addView(TextView(this).apply {
            text = "Kanade 歌词支持"
            textSize = 26f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.rgb(32, 34, 40))
        })

        content.addView(TextView(this).apply {
            text = "这是一个 LSPosed 模块，用来把 Kanade 正在播放的歌曲、歌手、歌词和进度发送给 Lyricon。"
            textSize = 16f
            setTextColor(Color.rgb(70, 74, 86))
            setLineSpacing(0f, 1.25f)
            setPadding(0, dp(18), 0, 0)
        })

        content.addView(TextView(this).apply {
            text = "使用步骤"
            textSize = 19f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.rgb(32, 34, 40))
            setPadding(0, dp(28), 0, 0)
        })

        content.addView(TextView(this).apply {
            text = "1. 在 LSPosed 中启用本模块。\n2. 作用域只勾选“Kanade”。\n3. 强制停止并重新打开 Kanade。\n4. 在 Lyricon 中确认已识别本模块。\n\n注意：如果 Lyricon 没有显示歌词，请检查 Lyricon 自身是否已启用、Kanade 是否正在播放，以及 LSPosed 日志中是否出现 KanadeLyricon 连接记录。"
            textSize = 15f
            setTextColor(Color.rgb(70, 74, 86))
            setLineSpacing(0f, 1.3f)
            setPadding(0, dp(12), 0, 0)
        })

        content.addView(TextView(this).apply {
            text = "本模块仅注入 com.lxi.kanade，不读取其它应用。"
            textSize = 13f
            setTextColor(Color.rgb(130, 134, 146))
            setPadding(0, dp(28), 0, 0)
            movementMethod = LinkMovementMethod.getInstance()
        })

        setContentView(ScrollView(this).apply {
            setBackgroundColor(Color.rgb(255, 247, 250))
            addView(content, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
            isFillViewport = true
        })
    }
}
