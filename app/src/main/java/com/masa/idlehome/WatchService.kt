package com.masa.idlehome

import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.provider.Settings

// 画面が消えた・スマホを開いたを見張って、何もしない時間を数える
class WatchService : Service() {

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    if (Store.screenOffAt(ctx) == 0L) Store.setScreenOffAt(ctx, System.currentTimeMillis())
                }
                Intent.ACTION_USER_PRESENT -> onOpened(ctx)
                Intent.ACTION_SCREEN_ON -> {
                    // 画面ロックを使っていない場合は、画面がついた時点で「開いた」とみなす
                    val km = ctx.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
                    if (!km.isKeyguardLocked) onOpened(ctx)
                }
            }
        }
    }

    private fun onOpened(ctx: Context) {
        val off = Store.screenOffAt(ctx)
        if (off == 0L) return
        val now = System.currentTimeMillis()
        val mins = Store.countedMinutes(ctx, off, now)
        Store.addIdle(ctx, Store.dayKey(now), mins)
        Store.setLastIdle(ctx, mins)
        Store.setScreenOffAt(ctx, 0L)
        Store.setPendingAsk(ctx, true)
        // 「他のアプリの上に表示」が許可されていれば、どのアプリを開いていても質問を出す
        if (Settings.canDrawOverlays(ctx) && !Store.inCall(ctx)) {
            try {
                ctx.startActivity(Intent(ctx, AskActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (e: Exception) {
                // 出せなかったときは、次にホーム画面を開いたときに質問する
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH, "記録中の表示", NotificationManager.IMPORTANCE_MIN))
        val pi = PendingIntent.getActivity(this, 0, Intent(this, HomeActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val n = Notification.Builder(this, CH)
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setContentTitle("何もしない時間を記録中")
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, n)
        }
        val f = IntentFilter()
        f.addAction(Intent.ACTION_SCREEN_OFF)
        f.addAction(Intent.ACTION_SCREEN_ON)
        f.addAction(Intent.ACTION_USER_PRESENT)
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(receiver, f, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(receiver, f)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        try { unregisterReceiver(receiver) } catch (e: Exception) { }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CH = "watch"
        fun start(c: Context) {
            try {
                c.startForegroundService(Intent(c, WatchService::class.java))
            } catch (e: Exception) {
            }
        }
    }
}
