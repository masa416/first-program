package com.masa.idlehome

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ホーム画面：開くと最初にメモ帳が出る
class HomeActivity : Activity() {
    private lateinit var header: TextView
    private lateinit var list: LinearLayout
    private lateinit var input: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = Ui.vbox(this)
        root.setBackgroundColor(Ui.BG)

        header = Ui.text(this, "", 14f, Ui.SUB)
        root.addView(header)
        root.addView(Ui.text(this, "メモ", 26f, Ui.TEXT, true), Ui.mw(this, 8))

        input = EditText(this)
        input.hint = "思いついたことを書く"
        input.setTextColor(Ui.TEXT)
        input.setHintTextColor(Ui.SUB)
        input.minLines = 2
        root.addView(input, Ui.mw(this, 8))
        root.addView(Ui.button(this, "残す") { save() }, Ui.mw(this, 4))

        list = LinearLayout(this)
        list.orientation = LinearLayout.VERTICAL
        val scroll = ScrollView(this)
        scroll.addView(list)
        val slp = LinearLayout.LayoutParams(Ui.MP, 0, 1f)
        slp.topMargin = Ui.dp(this, 12)
        root.addView(scroll, slp)

        // アプリは1段階奥に置く（すぐには開けない）
        val bar = LinearLayout(this)
        bar.orientation = LinearLayout.HORIZONTAL
        bar.addView(Ui.button(this, "アプリ") { startActivity(Intent(this, AppsActivity::class.java)) },
            LinearLayout.LayoutParams(0, Ui.WC, 1f))
        bar.addView(Ui.button(this, "設定") { startActivity(Intent(this, SettingsActivity::class.java)) },
            LinearLayout.LayoutParams(0, Ui.WC, 1f))
        root.addView(bar, Ui.mw(this, 8))

        setContentView(root)

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        WatchService.start(this)
    }

    override fun onResume() {
        super.onResume()
        WatchService.start(this)
        // 普通モード中は、いつものホーム画面（Pixel Launcher など）を出す
        if (!Store.pendingAsk(this) && Store.isNormal(this) &&
            Store.remaining(this, System.currentTimeMillis()) > 0L && openNormalHome()) return
        refresh()
        if (Store.pendingAsk(this)) startActivity(Intent(this, AskActivity::class.java))
    }

    // 普通モード用のホームアプリを開く。見つからなければ false
    private fun openNormalHome(): Boolean {
        var pkg = Store.normalHome(this)
        if (pkg.isEmpty()) pkg = Store.PIXEL_LAUNCHER
        val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).setPackage(pkg)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (i.resolveActivity(packageManager) == null) return false
        return try {
            startActivity(i)
            overridePendingTransition(0, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    @Deprecated("ホーム画面なので戻るボタンでは何もしない")
    override fun onBackPressed() {
    }

    private fun save() {
        val t = input.text.toString().trim()
        if (t.isEmpty()) return
        Store.addMemo(this, t, 0)
        input.setText("")
        refresh()
    }

    private fun refresh() {
        val now = System.currentTimeMillis()
        val date = SimpleDateFormat("M月d日(E)", Locale.JAPAN).format(Date(now))
        val mode = if (Store.isNormal(this)) "普通モード中・" else ""
        header.text = date + "　今日の何もしない時間 " + Ui.fmtMin(Store.idleOn(this, Store.dayKey(now))) +
            "\n" + mode + "普通に使える時間 残り " + Store.fmtSec(Store.remaining(this, now))

        list.removeAllViews()
        val memos = Store.memos(this)
        if (memos.isEmpty()) list.addView(Ui.text(this, "まだメモはありません", 14f, Ui.SUB))
        val fmt = SimpleDateFormat("M/d HH:mm", Locale.JAPAN)
        for (m in memos) {
            val card = Ui.vbox(this, 12)
            card.setBackgroundColor(Ui.CARD)
            var meta = fmt.format(Date(m.time))
            if (m.idleMin > 0) meta += "　何もしない時間 " + Ui.fmtMin(m.idleMin) + " のあと"
            card.addView(Ui.text(this, meta, 12f, Ui.SUB))
            card.addView(Ui.text(this, m.text, 16f))
            card.setOnLongClickListener {
                AlertDialog.Builder(this)
                    .setMessage("このメモを消す？")
                    .setPositiveButton("消す") { _, _ ->
                        Store.deleteMemo(this, m.time)
                        refresh()
                    }
                    .setNegativeButton("やめる", null)
                    .show()
                true
            }
            list.addView(card, Ui.mw(this, 8))
        }
    }
}
