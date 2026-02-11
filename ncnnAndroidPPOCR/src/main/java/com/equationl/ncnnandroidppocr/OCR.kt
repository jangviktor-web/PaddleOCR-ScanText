package com.equationl.ncnnandroidppocr

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.equationl.ncnnandroidppocr.bean.Device
import com.equationl.ncnnandroidppocr.bean.DrawModel
import com.equationl.ncnnandroidppocr.bean.ImageSize
import com.equationl.ncnnandroidppocr.bean.ModelType
import com.equationl.ncnnandroidppocr.bean.OcrResult
import com.equationl.ncnnandroidppocr.cpp.OCRNative

class OCR {
    private val ocrNative by lazy { OCRNative() }

    // 绘制颜色列表
    private val drawColors = intArrayOf(
        Color.RED,
        Color.GREEN,
        Color.BLUE,
        Color.YELLOW,
        Color.MAGENTA,
        Color.CYAN,
        Color.rgb(128, 0, 0),    // Dark Red
        Color.rgb(0, 128, 0),    // Dark Green
        Color.rgb(0, 0, 128),    // Dark Blue
        Color.rgb(128, 128, 0),  // Olive
        Color.rgb(128, 0, 128),  // Purple
        Color.rgb(0, 128, 128),  // Teal
    )

    /**
     * 从 assets 目录加载模型
     *
     * @param assertManage context.assets
     * @param model 预置模型类型，可选移动端模型或服务端模型（需要将模型文件放置到项目的 assert 目录）
     * @param reSize 输入图像预处理尺寸
     * @param useDevice 计算方式选择，可使用 CPU、GPU 或 Turnip Vulkan（仅部分设备支持）
     * */
    @JvmOverloads
    fun initModelFromAssert(
        assertManage: AssetManager?,
        model: ModelType,
        reSize: ImageSize = ImageSize.Size640,
        useDevice: Device = Device.CPU
    ): Boolean {
        return ocrNative.loadModel(assertManage, model.ordinal, reSize.ordinal, useDevice.ordinal)
    }

    /**
     * 从指定路径加载指定模型
     *
     * **注意：可能需要申请储存权限**
     *
     * @param detParamPath 检测模型参数文件路径 (如 /sdcard/models/PP_OCRv5_mobile_det.ncnn.param)
     * @param detModelPath 检测模型权重文件路径 (如 /sdcard/models/PP_OCRv5_mobile_det.ncnn.bin)
     * @param recParamPath 识别模型参数文件路径 (如 /sdcard/models/PP_OCRv5_mobile_rec.ncnn.param)
     * @param recModelPath 识别模型权重文件路径 (如 /sdcard/models/PP_OCRv5_mobile_rec.ncnn.bin)
     * @param reSize 输入图像预处理尺寸
     * @param useDevice 计算方式选择，可使用 CPU、GPU 或 Turnip Vulkan（仅部分设备支持）
     * @param useFp16 是否使用 fp16 精度推理，默认为 true。注意：server 模型使用 fp16 可能产生 nan 结果
     *
     * */
    @JvmOverloads
    fun initModel(
        detParamPath: String,
        detModelPath: String,
        recParamPath: String,
        recModelPath: String,
        reSize: ImageSize = ImageSize.Size640,
        useDevice: Device = Device.CPU,
        useFp16: Boolean = true
    ): Boolean {
        return ocrNative.loadModelByPath(
            detParamPath,
            detModelPath,
            recParamPath,
            recModelPath,
            reSize.ordinal,
            useDevice.ordinal,
            useFp16
        )
    }

    /**
     *  释放模型资源
     *
     * 在不再需要 OCR 功能时调用此方法释放内存
     * */
    fun release() {
        ocrNative.release()
    }

    /**
     * 使用 Bitmap 进行 OCR 识别
     *
     * @param bitmap 要识别的图片
     * @param drawModel 是否将识别结果绘制到图片中，默认为不绘制。注意：绘制结果会增加识别时间。
     * @return OCR 识别结果
     * */
    @JvmOverloads
    fun detectBitmap(bitmap: Bitmap, drawModel: DrawModel = DrawModel.None): OcrResult? {
        val result = ocrNative.detectBitmap(bitmap) ?: return null
        val drawBitmap = if (drawModel != DrawModel.None) {
            drawResults(bitmap, result, drawModel)
        } else {
            null
        }
        return result.copy(drawBitmap = drawBitmap)
    }

    /**
     * 使用图片文件路径进行 OCR 识别
     *
     * @param imagePath 图片文件路径
     * @param drawModel 是否将识别结果绘制到图片中，默认为不绘制。注意：绘制结果会增加识别时间。
     * @return OCR 识别结果
     * */
    @JvmOverloads
    fun detectImagePath(imagePath: String, drawModel: DrawModel = DrawModel.None): OcrResult? {
        val result = ocrNative.detectImagePath(imagePath) ?: return null
        val drawBitmap = if (drawModel != DrawModel.None) {
            val bitmap = BitmapFactory.decodeFile(imagePath)
            if (bitmap != null) {
                drawResults(bitmap, result, drawModel)
            } else {
                null
            }
        } else {
            null
        }
        return result.copy(drawBitmap = drawBitmap)
    }

    /**
     * 在图片上绘制识别结果
     */
    private fun drawResults(bitmap: Bitmap, result: OcrResult, drawModel: DrawModel): Bitmap {
        // 创建可变 Bitmap
        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)

        val boxPaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            textSize = 24f
            isAntiAlias = true
        }

        val bgPaint = Paint().apply {
            style = Paint.Style.FILL
            color = Color.WHITE
        }

        result.textLines.forEachIndexed { index, textLine ->
            val color = drawColors[index % drawColors.size]
            boxPaint.color = color
            textPaint.color = color

            // 绘制文本框
            if (textLine.points.size == 4) {
                val path = Path().apply {
                    moveTo(textLine.points[0].x.toFloat(), textLine.points[0].y.toFloat())
                    lineTo(textLine.points[1].x.toFloat(), textLine.points[1].y.toFloat())
                    lineTo(textLine.points[2].x.toFloat(), textLine.points[2].y.toFloat())
                    lineTo(textLine.points[3].x.toFloat(), textLine.points[3].y.toFloat())
                    close()
                }
                canvas.drawPath(path, boxPaint)
            }

            // 绘制文本（如果是 Full 模式）
            if (drawModel == DrawModel.Full && textLine.points.isNotEmpty()) {
                val text = textLine.text
                if (text.isNotEmpty()) {
                    // 计算文本位置（取框的最小 Y 和最小 X）
                    val minX = textLine.points.minOf { it.x }.toFloat()
                    val minY = textLine.points.minOf { it.y }.toFloat()

                    // 计算文本尺寸
                    val textWidth = textPaint.measureText(text)
                    val textHeight = textPaint.textSize

                    // 绘制文本背景
                    val textY = if (minY - textHeight - 5 < 0) textHeight + 5 else minY - 5
                    canvas.drawRect(
                        minX,
                        textY - textHeight,
                        minX + textWidth,
                        textY + 5,
                        bgPaint
                    )

                    // 绘制文本
                    canvas.drawText(text, minX, textY, textPaint)
                }
            }
        }

        return mutableBitmap
    }
}