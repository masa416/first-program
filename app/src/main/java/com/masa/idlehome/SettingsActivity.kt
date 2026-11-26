package com.masa.idlehome

import android.app.Activity
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.LinearLayout
import android.widget.ScrollView
import java.util.Locale

class SettingsActivity : Activity() {
    private lateinit var root: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        root = Ui.vbox(this)
        val scroll = ScrollView(this)
        scroll.setBackgroundColor(Ui.BG)
        scroll.addView(root)
        setContentView(scroll)
    }

    override fun onResume() {
        super.onResume()
        build()
    }

    private fun hhmm(m: Int): String = String.format(Locale.JAPAN, "%02d:%02d", m / 60, m % 60)

    private fun build() {
        root.removeAllViews()
        root.addView(Ui.text(this, "設定", 22f, Ui.TEXT, true))

        root.addView(Ui.text(this, "寝ている時間（何もしない時間から外す）", 14f, Ui.SUB), Ui.mw(this, 16))
        val s = Store.sleepStart(this)
        val e = Store.sleepEnd(this)
        root.addView(Ui.button(this, "寝る時刻 " + hhmm(s)) {
            TimePickerDialog(this, { _, h, m ->
                Store.setSleep(this, h * 60 + m, Store.sleepEnd(this))
                build()
            }, s / 60, s % 60, true).show()
        }, Ui.mw(this, 4))
        root.addView(Ui.button(this, "起きる時刻 " + hhmm(e)) {
            TimePickerDialog(this, { _, h, m ->
                Store.setSleep(this, Store.sleepStart(this), h * 60 + m)
                build()
            }, e / 60, e % 60, true).show()
        }, Ui.mw(this, 4))

        root.addView(Ui.text(this, "最初にやること", 14f, Ui.SUB), Ui.mw(this, 20))
        val overlay = Settings.canDrawOverlays(this)
        val label = (if (overlay) "✓ " else "") + "他のアプリの上に表示を許可（開くたびに質問するため）"
        root.addView(Ui.button(this, label) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        }, Ui.mw(this, 4))
        root.addView(Ui.button(this, "ホームアプリをこのアプリにする") {
            startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
        }, Ui.mw(this, 4))
        root.addView(Ui.button(this, "電池の最適化から外す（記録が止まらないように）") {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }, Ui.mw(this, 4))

        root.addView(Ui.text(this, "「緊急で使う」を押した回数：" + Store.emergencyCount(this) + "回", 12f, Ui.SUB), Ui.mw(this, 20))
        root.addView(Ui.text(this, "版 0.3（試作）", 12f, Ui.SUB), Ui.mw(this, 20))
    }
}
