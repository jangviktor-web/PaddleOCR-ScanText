package com.equationl.ncnnandroidppocr.bean

import android.graphics.Bitmap
import android.graphics.Point

data class OcrResult(
    /**
     * 简单识别结果（直接把所有识别到的字符串按顺序拼接到一起）
     * */
    val text: String,
    /**
     * 识别耗时
     * */
    val inferenceTime: Long,
    /**
     * 文本识别详情信息
     * */
    val textLines: List<OcrTextLineResult>,
    /**
     * 绘制了识别结果的 Bitmap 图像（如果 [DrawModel] 选择了 None 则该值为 null）
     * */
    val drawBitmap: Bitmap? = null
)

data class OcrTextLineResult(
    /**识别到的文本位置框，四个 [Point] 分别表示文本框的 左下，右下，右上，左上 点*/
    val points: List<Point>,
    /**识别到的文字（拼接后）*/
    val text: String,
    /**检测置信度*/
    val confidence: Float,
    /**检测到的文字方向 0=水平, 1=垂直*/
    val orientation: Int,
    /**字符详细列表**/
    val textList: List<OcrTextResult>
)

data class OcrTextResult(
    val text: String,
    val id: Int,
    val confidence: Float
)

