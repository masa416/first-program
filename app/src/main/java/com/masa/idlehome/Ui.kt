package com.masa.idlehome

import android.content.Context
import android.graphics.Typeface
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

// 画面づくりの共通部品
object Ui {
    const val BG = 0xFF101418.toInt()
    const val CARD = 0xFF1C2228.toInt()
    const val TEXT = 0xFFE8EAED.toInt()
    const val SUB = 0xFF9AA4AE.toInt()
    const val MP = ViewGroup.LayoutParams.MATCH_PARENT
    const val WC = ViewGroup.LayoutParams.WRAP_CONTENT

    fun dp(c: Context, v: Int): Int = (v * c.resources.displayMetrics.density).toInt()

    fun text(c: Context, s: String, size: Float = 16f, color: Int = TEXT, bold: Boolean = false): TextView {
        val t = TextView(c)
        t.text = s
        t.textSize = size
        t.setTextColor(color)
        if (bold) t.setTypeface(t.typeface, Typeface.BOLD)
        return t
    }

    fun button(c: Context, s: String, onClick: () -> Unit): Button {
        val b = Button(c)
        b.text = s
        b.isAllCaps = false
        b.setOnClickListener { onClick() }
        return b
    }

    fun vbox(c: Context, padDp: Int = 16): LinearLayout {
        val l = LinearLayout(c)
        l.orientation = LinearLayout.VERTICAL
        val p = dp(c, padDp)
        l.setPadding(p, p, p, p)
        return l
    }

    // 横いっぱい・高さは中身に合わせる。上の余白を指定できる
    fun mw(c: Context, topDp: Int = 0): LinearLayout.LayoutParams {
        val lp = LinearLayout.LayoutParams(MP, WC)
        lp.topMargin = dp(c, topDp)
        return lp
    }

    fun fmtMin(m: Int): String = if (m >= 60) "${m / 60}時間${m % 60}分" else "${m}分"
}
