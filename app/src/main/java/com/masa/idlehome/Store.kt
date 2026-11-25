package com.masa.idlehome

import android.content.Context
import android.content.SharedPreferences
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
