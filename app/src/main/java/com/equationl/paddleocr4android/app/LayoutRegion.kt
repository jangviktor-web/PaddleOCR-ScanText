package com.equationl.paddleocr4android.app

import android.graphics.Color
import android.graphics.RectF

enum class LayoutType(val label: String, val color: Int) {
    TITLE("标题", Color.parseColor("#4CAF50")),
    TEXT("正文", Color.parseColor("#2196F3")),
    TABLE("表格", Color.parseColor("#FF9800")),
    LIST("列表", Color.parseColor("#9E9E9E")),
    IMAGE("图片", Color.parseColor("#9C27B0"))
}

data class LayoutRegion(
    val type: LayoutType,
    val boxIndices: List<Int>,
    val bbox: RectF,
    val lines: List<String>,
    val confidence: Float
)
