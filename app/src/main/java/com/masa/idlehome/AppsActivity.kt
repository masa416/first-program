package com.masa.idlehome

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView

// アプリ一覧（ホーム画面から1段階奥）
class AppsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pm = packageManager
        val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(query, 0)
            .filter { it.activityInfo.packageName != packageName }
            .map { Pair(it.loadLabel(pm).toString(), it) }
            .sortedBy { it.first }

        val root = Ui.vbox(this)
        root.setBackgroundColor(Ui.BG)
        root.addView(Ui.text(this, "アプリ", 22f, Ui.TEXT, true))

        val lv = ListView(this)
        lv.adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, apps.map { it.first }) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = super.getView(position, convertView, parent) as TextView
                v.setTextColor(Ui.TEXT)
                return v
            }
        }
        lv.setOnItemClickListener { _, _, pos, _ ->
            val ai = apps[pos].second.activityInfo
            val i = Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setClassName(ai.packageName, ai.name)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            try { startActivity(i) } catch (e: Exception) { }
            finish()
        }
        root.addView(lv, LinearLayout.LayoutParams(Ui.MP, 0, 1f))
        setContentView(root)
    }
}
