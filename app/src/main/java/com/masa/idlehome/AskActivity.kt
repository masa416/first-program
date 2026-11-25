package com.masa.idlehome

import android.app.Activity
import android.os.Bundle
import android.widget.EditText

// スマホを開くたびに出る「今なに考えてた？」の画面
class AskActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.setPendingAsk(this, false)
        val idle = Store.lastIdle(this)

        val root = Ui.vbox(this, 24)
        root.setBackgroundColor(Ui.BG)
        root.addView(Ui.text(this, "今なに考えてた？", 28f, Ui.TEXT, true))
        val sub = if (idle > 0) "何もしない時間：" + Ui.fmtMin(idle) else ""
        root.addView(Ui.text(this, sub, 14f, Ui.SUB), Ui.mw(this, 8))

        val input = EditText(this)
        input.hint = "一言でいいよ"
        input.setTextColor(Ui.TEXT)
        input.setHintTextColor(Ui.SUB)
        input.minLines = 2
        root.addView(input, Ui.mw(this, 16))

        root.addView(Ui.button(this, "残す") {
            val t = input.text.toString().trim()
            if (t.isNotEmpty()) Store.addMemo(this, t, idle)
            finish()
        }, Ui.mw(this, 12))
        root.addView(Ui.button(this, "今はいい") { finish() }, Ui.mw(this, 4))

        setContentView(root)
        input.requestFocus()
    }
}
