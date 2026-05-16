package com.equationl.paddleocr4android.app

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class HistoryActivity : AppCompatActivity() {

    private lateinit var rvHistory: RecyclerView
    private lateinit var emptyView: View
    private lateinit var btnClearAll: View
    private val db by lazy { HistoryDatabase.getInstance(this) }
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        findViewById<ImageView>(R.id.btn_back_history).setOnClickListener { finish() }
        rvHistory = findViewById(R.id.rv_history)
        emptyView = findViewById(R.id.empty_history)
        btnClearAll = findViewById(R.id.btn_clear_all)

        rvHistory.layoutManager = LinearLayoutManager(this)

        btnClearAll.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("清空历史")
                .setMessage("确定要清空所有识别记录吗？此操作不可撤销。")
                .setPositiveButton("清空") { _, _ -> clearAll() }
                .setNegativeButton("取消", null)
                .show()
        }

        loadHistory()
    }

    private fun loadHistory() {
        scope.launch {
            val list = withContext(Dispatchers.IO) { db.historyDao().getAll() }
            if (list.isEmpty()) {
                rvHistory.visibility = View.GONE
                emptyView.visibility = View.VISIBLE
                btnClearAll.visibility = View.GONE
            } else {
                rvHistory.visibility = View.VISIBLE
                emptyView.visibility = View.GONE
                btnClearAll.visibility = View.VISIBLE
                rvHistory.adapter = HistoryAdapter(list) { item ->
                    MaterialAlertDialogBuilder(this@HistoryActivity)
                        .setTitle("删除记录")
                        .setMessage("确定要删除这条记录吗？")
                        .setPositiveButton("删除") { _, _ -> deleteItem(item) }
                        .setNegativeButton("取消", null)
                        .show()
                }
            }
        }
    }

    private fun deleteItem(item: HistoryEntity) {
        scope.launch {
            withContext(Dispatchers.IO) { db.historyDao().delete(item) }
            // 删除图片文件
            File(item.imagePath).delete()
            loadHistory()
        }
    }

    private fun clearAll() {
        scope.launch {
            val all = withContext(Dispatchers.IO) {
                val list = db.historyDao().getAll()
                db.historyDao().deleteAll()
                list
            }
            // 删除所有图片文件
            all.forEach { File(it.imagePath).delete() }
            loadHistory()
            Toast.makeText(this@HistoryActivity, "已清空", Toast.LENGTH_SHORT).show()
        }
    }
}

class HistoryAdapter(
    private val items: List<HistoryEntity>,
    private val onLongClick: (HistoryEntity) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.VH>() {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val ivThumb: ImageView = view.findViewById(R.id.iv_history_thumb)
        val tvText: TextView = view.findViewById(R.id.tv_history_text)
        val tvMeta: TextView = view.findViewById(R.id.tv_history_meta)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_history, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]

        // 加载缩略图
        val file = File(item.imagePath)
        if (file.exists()) {
            val bitmap = BitmapFactory.decodeFile(item.imagePath)
            holder.ivThumb.setImageBitmap(bitmap)
        } else {
            holder.ivThumb.setImageResource(R.drawable.ic_photo)
        }

        holder.tvText.text = item.text.take(100) + if (item.text.length > 100) "..." else ""
        holder.tvMeta.text = "${item.language} · ${dateFormat.format(Date(item.timestamp))}"

        holder.itemView.setOnLongClickListener {
            onLongClick(item)
            true
        }
    }

    override fun getItemCount() = items.size
}
