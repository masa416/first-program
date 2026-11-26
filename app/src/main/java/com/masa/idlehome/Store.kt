package com.masa.idlehome

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioManager
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class Memo(val time: Long, val text: String, val idleMin: Int)

// データの保存（スマホの中だけに保存される）
object Store {
    private fun p(c: Context): SharedPreferences = c.getSharedPreferences("store", Context.MODE_PRIVATE)

    fun dayKey(t: Long): String = SimpleDateFormat("yyyy-MM-dd", Locale.JAPAN).format(Date(t))

    // ---- メモ ----
    fun memos(c: Context): List<Memo> {
        val arr = JSONArray(p(c).getString("memos", "[]"))
        val out = ArrayList<Memo>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(Memo(o.getLong("t"), o.getString("x"), o.optInt("i", 0)))
        }
        return out.sortedByDescending { it.time }
    }

    fun addMemo(c: Context, text: String, idleMin: Int) {
        val arr = JSONArray(p(c).getString("memos", "[]"))
        val o = JSONObject()
        o.put("t", System.currentTimeMillis())
        o.put("x", text)
        o.put("i", idleMin)
        arr.put(o)
        p(c).edit().putString("memos", arr.toString()).apply()
    }

    fun deleteMemo(c: Context, time: Long) {
        val arr = JSONArray(p(c).getString("memos", "[]"))
        val out = JSONArray()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            if (o.getLong("t") != time) out.put(o)
        }
        p(c).edit().putString("memos", out.toString()).apply()
    }

    // ---- 寝ている時間（分。0:00 からの経過分）----
    fun sleepStart(c: Context): Int = p(c).getInt("sleepStart", 0)
    fun sleepEnd(c: Context): Int = p(c).getInt("sleepEnd", 7 * 60)
    fun setSleep(c: Context, start: Int, end: Int) {
        p(c).edit().putInt("sleepStart", start).putInt("sleepEnd", end).apply()
    }

    // ---- 画面が消えた時刻・直前の何もしない時間・質問待ち ----
    fun screenOffAt(c: Context): Long = p(c).getLong("offAt", 0L)
    fun setScreenOffAt(c: Context, t: Long) { p(c).edit().putLong("offAt", t).apply() }
    fun lastIdle(c: Context): Int = p(c).getInt("lastIdle", 0)
    fun setLastIdle(c: Context, m: Int) { p(c).edit().putInt("lastIdle", m).apply() }
    fun pendingAsk(c: Context): Boolean = p(c).getBoolean("pendingAsk", false)
    fun setPendingAsk(c: Context, v: Boolean) { p(c).edit().putBoolean("pendingAsk", v).apply() }

    // ---- 普通モード（何もしない時間の0.5倍だけ、普通にスマホを使える）----
    const val RATE = 0.5          // 何もしない1分 → 普通に使える0.5分
    // 残り時間は秒で持つ。日をまたいだら0に戻す
    private fun ensureDay(c: Context, now: Long) {
        val today = dayKey(now)
        if (p(c).getString("creditDay", "") != today) {
            p(c).edit().putString("creditDay", today).putLong("credit", 0L)
                .putBoolean("normal", false).putLong("segStart", 0L).apply()
        }
    }
    fun addCredit(c: Context, idleMin: Int, now: Long) {
        ensureDay(c, now)
        val add = (idleMin * 60 * RATE).toLong()
        p(c).edit().putLong("credit", p(c).getLong("credit", 0L) + add).apply()
    }
    fun isNormal(c: Context): Boolean = p(c).getBoolean("normal", false)
    // 今の残り秒（普通モードで画面がついている間は、その分を引いて計算）
    fun remaining(c: Context, now: Long): Long {
        ensureDay(c, now)
        var r = p(c).getLong("credit", 0L)
        val seg = p(c).getLong("segStart", 0L)
        if (isNormal(c) && seg > 0L) r -= (now - seg) / 1000
        return if (r < 0L) 0L else r
    }
    // 普通モードを始める（画面がついている前提）
    fun startNormal(c: Context, now: Long) {
        p(c).edit().putBoolean("normal", true).putLong("segStart", now).apply()
    }
    // 画面がついた：普通モードなら減り始める
    fun resumeSegment(c: Context, now: Long) {
        if (isNormal(c)) p(c).edit().putLong("segStart", now).apply()
    }
    // 画面が消えた・時間切れ：使った分を引く。0になったら普通モードを終える
    fun consume(c: Context, now: Long) {
        val r = remaining(c, now)
        val e = p(c).edit().putLong("credit", r).putLong("segStart", 0L)
        if (r <= 0L) e.putBoolean("normal", false)
        e.apply()
    }

    // 普通モード中に使うホームアプリ（空なら Pixel Launcher を自動で探す）
    const val PIXEL_LAUNCHER = "com.google.android.apps.nexuslauncher"
    fun normalHome(c: Context): String = p(c).getString("normalHome", "") ?: ""
    fun setNormalHome(c: Context, pkg: String) { p(c).edit().putString("normalHome", pkg).apply() }

    fun fmtSec(s: Long): String = if (s >= 60) "${s / 60}分${s % 60}秒" else "${s}秒"

    // ---- 「緊急で使う」を押した回数 ----
    fun emergencyCount(c: Context): Int = p(c).getInt("emergency", 0)
    fun addEmergency(c: Context) { p(c).edit().putInt("emergency", emergencyCount(c) + 1).apply() }

    // 電話中・着信中かどうか（このときは質問で邪魔しない）
    fun inCall(c: Context): Boolean {
        val am = c.getSystemService(AudioManager::class.java)
        val m = am.mode
        return m == AudioManager.MODE_IN_CALL || m == AudioManager.MODE_IN_COMMUNICATION || m == AudioManager.MODE_RINGTONE
    }

    // ---- 日ごとの何もしない時間（分）----
    fun idleOn(c: Context, day: String): Int = JSONObject(p(c).getString("idle", "{}")).optInt(day, 0)
    fun addIdle(c: Context, day: String, min: Int) {
        val o = JSONObject(p(c).getString("idle", "{}"))
        o.put(day, o.optInt(day, 0) + min)
        p(c).edit().putString("idle", o.toString()).apply()
    }

    // start〜end のうち、寝ている時間帯を除いた分数を数える（最大48時間）
    fun countedMinutes(c: Context, start: Long, end: Long): Int {
        val s = sleepStart(c)
        val e = sleepEnd(c)
        val cal = Calendar.getInstance()
        var t = start
        var n = 0
        var guard = 0
        while (t + 60_000L <= end && guard < 2880) {
            cal.timeInMillis = t
            val m = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            val inSleep = if (s <= e) (m >= s && m < e) else (m >= s || m < e)
            if (!inSleep) n++
            t += 60_000L
            guard++
        }
        return n
    }
}
