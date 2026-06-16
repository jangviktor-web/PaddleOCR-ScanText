package com.equationl.paddleocr4android.app

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class SettingsActivity : AppCompatActivity() {

    companion object {
        const val PREF_NAME = "ocr_settings"
        const val KEY_PHOTO_MAX_SIZE = "photo_max_size"
        const val KEY_CAMERA_RESOLUTION = "camera_resolution"
        const val DEFAULT_PHOTO_MAX_SIZE = 4096
        const val DEFAULT_CAMERA_RESOLUTION = 3 // 索引：2560x1440
        private val PHOTO_SIZE_OPTIONS = intArrayOf(1024, 2048, 3072, 4096)
        private val PHOTO_SIZE_LABELS = arrayOf("1024 px", "2048 px", "3072 px", "4096 px（默认）")
        private val RESOLUTION_LABELS = arrayOf(
            "640 x 480", "1280 x 720", "1920 x 1080",
            "2560 x 1440（默认）", "3840 x 2160", "最高分辨率"
        )
    }

    private lateinit var prefs: SharedPreferences
    private lateinit var tvPhotoSizeValue: TextView
    private lateinit var tvCameraResValue: TextView
    private lateinit var tvCacheSize: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE)

        findViewById<ImageView>(R.id.btn_back_settings).setOnClickListener { finish() }

        tvPhotoSizeValue = findViewById(R.id.tv_photo_size_value)
        updatePhotoSizeLabel()
        findViewById<android.view.View>(R.id.btn_photo_size).setOnClickListener {
            showPhotoSizeDialog()
        }

        tvCameraResValue = findViewById(R.id.tv_camera_res_value)
        updateCameraResLabel()
        findViewById<android.view.View>(R.id.btn_camera_resolution).setOnClickListener {
            showCameraResDialog()
        }

        tvCacheSize = findViewById(R.id.tv_cache_size)
        updateCacheSize()

        findViewById<android.view.View>(R.id.btn_clear_cache).setOnClickListener {
            showClearCacheDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        updateCacheSize()
    }

    private fun showPhotoSizeDialog() {
        val currentSize = prefs.getInt(KEY_PHOTO_MAX_SIZE, DEFAULT_PHOTO_MAX_SIZE)
        val checkedIndex = PHOTO_SIZE_OPTIONS.indexOf(currentSize).coerceAtLeast(0)

        AlertDialog.Builder(this)
            .setTitle("选择照片最大尺寸")
            .setSingleChoiceItems(PHOTO_SIZE_LABELS, checkedIndex) { dialog, which ->
                prefs.edit().putInt(KEY_PHOTO_MAX_SIZE, PHOTO_SIZE_OPTIONS[which]).apply()
                updatePhotoSizeLabel()
                dialog.dismiss()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun updatePhotoSizeLabel() {
        val size = prefs.getInt(KEY_PHOTO_MAX_SIZE, DEFAULT_PHOTO_MAX_SIZE)
        tvPhotoSizeValue.text = if (size >= 4096) "不缩放（原图）" else "$size px"
    }

    private fun showCameraResDialog() {
        val current = prefs.getInt(KEY_CAMERA_RESOLUTION, DEFAULT_CAMERA_RESOLUTION)
        AlertDialog.Builder(this)
            .setTitle("选择拍照分辨率")
            .setSingleChoiceItems(RESOLUTION_LABELS, current) { dialog, which ->
                prefs.edit().putInt(KEY_CAMERA_RESOLUTION, which).apply()
                updateCameraResLabel()
                dialog.dismiss()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun updateCameraResLabel() {
        val idx = prefs.getInt(KEY_CAMERA_RESOLUTION, DEFAULT_CAMERA_RESOLUTION)
        tvCameraResValue.text = RESOLUTION_LABELS[idx]
    }

    private fun getCacheSize(): Long {
        var size = 0L
        cacheDir.listFiles()?.forEach { size += it.length() }
        File(filesDir, "history_images").listFiles()?.forEach { size += it.length() }
        return size
    }

    private fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> String.format("%.1f MB", bytes / 1024.0 / 1024.0)
        }
    }

    private fun updateCacheSize() {
        tvCacheSize.text = formatSize(getCacheSize())
    }

    private fun showClearCacheDialog() {
        val size = getCacheSize()
        if (size == 0L) {
            Toast.makeText(this, "缓存已为空", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("清除缓存")
            .setMessage("将删除 ${formatSize(size)} 的临时文件（拍照缓存和历史图片），确认清除？")
            .setPositiveButton("清除") { _, _ ->
                clearCache()
                updateCacheSize()
                Toast.makeText(this, "缓存已清除", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun clearCache() {
        cacheDir.listFiles()?.forEach { it.delete() }
        File(filesDir, "history_images").listFiles()?.forEach { it.delete() }
    }
}
