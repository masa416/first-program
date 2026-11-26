package com.masa.idlehome

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.widget.EditText
import android.widget.Toast

// スマホを開くたびに出る「今なに考えてた？」の画面
// 記録を書くまで、他のアプリには行けない（電話中と「緊急」は例外）
class AskActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val idle = Store.lastIdle(this)

        val root = Ui.vbox(this, 24)
        root.setBackgroundColor(Ui.BG)
        root.addView(Ui.text(this, "今なに考えてた？", 28f, Ui.TEXT, true))
        val sub = if (idle > 0) "何もしない時間：" + Ui.fmtMin(idle) else ""
        root.addView(Ui.text(this, sub, 14f, Ui.SUB), Ui.mw(this, 8))
        root.addView(Ui.text(this, "書くまで他のアプリは開けないよ", 13f, Ui.SUB), Ui.mw(this, 4))

        val input = EditText(this)
        input.hint = "一言でいいよ"
        input.setTextColor(Ui.TEXT)
        input.setHintTextColor(Ui.SUB)
        input.minLines = 2
        root.addView(input, Ui.mw(this, 16))

        root.addView(Ui.button(this, "残す") {
            val t = input.text.toString().trim()
            if (t.isEmpty()) {
                Toast.makeText(this, "一言だけ書いてね", Toast.LENGTH_SHORT).show()
            } else {
                Store.addMemo(this, t, idle)
                Store.setPendingAsk(this, false)
                finish()
            }
        }, Ui.mw(this, 12))

        // 緊急のときだけの抜け道（電話・地図・連絡など）
        root.addView(Ui.button(this, "緊急で使う") {
            Store.setPendingAsk(this, false)
            Store.addEmergency(this)
            finish()
        }, Ui.mw(this, 32))

        setContentView(root)
        input.requestFocus()
    }

    @Deprecated("記録を書くまで戻れない")
    override fun onBackPressed() {
    }

    // 書かずに別のアプリへ移ろうとしたら、この画面に引き戻す
    override fun onStop() {
        super.onStop()
        if (!Store.pendingAsk(this)) return
        val app = applicationContext
        Handler(Looper.getMainLooper()).postDelayed({
            val pm = app.getSystemService(PowerManager::class.java)
            if (Store.pendingAsk(app) && pm.isInteractive && !Store.inCall(app) && Settings.canDrawOverlays(app)) {
                try {
                    app.startActivity(Intent(app, AskActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } catch (e: Exception) {
                }
            }
        }, 800)
    }
}
