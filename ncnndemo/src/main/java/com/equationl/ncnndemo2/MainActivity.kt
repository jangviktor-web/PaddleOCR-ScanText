package com.equationl.ncnndemo2

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.equationl.ncnnandroidppocr.OCR
import com.equationl.ncnnandroidppocr.bean.Device
import com.equationl.ncnnandroidppocr.bean.DrawModel
import com.equationl.ncnnandroidppocr.bean.ImageSize
import com.equationl.ncnnandroidppocr.bean.ModelType

class MainActivity : AppCompatActivity() {
    private val TAG = "el, Main"

    private lateinit var ocr: OCR

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ocr = OCR()

        val initBtn = findViewById<Button>(R.id.init_model)
        val startBtn = findViewById<Button>(R.id.start_model)
        val resultImg = findViewById<ImageView>(R.id.result_img)
        val resultText = findViewById<TextView>(R.id.result_text)

        initBtn.setOnClickListener {
            resultText.text = "开始加载模型"
            // 1. 从 assets 目录加载预置模型（无需申请存储权限，适合初次使用快速体验）
            val initResult =
                ocr.initModelFromAssert(assets, ModelType.Mobile, ImageSize.Size720, Device.CPU)

            // 2. 从指定路径加载模型（需要申请存储权限，适合使用自定义模型或不想将模型文件放置在 assets 目录的情况）
            /*val initResult2 = ocr.initModel(
                detParamPath = "/sdcard/models/PP_OCRv5_mobile_det.ncnn.param",
                detModelPath = "/sdcard/models/PP_OCRv5_mobile_det.ncnn.bin",
                recParamPath = "/sdcard/models/PP_OCRv5_mobile_rec.ncnn.param",
                recModelPath = "/sdcard/models/PP_OCRv5_mobile_rec.ncnn.bin",
                reSize = ImageSize.Size720,
                useDevice = Device.CPU,
                useFp16 = true
            )*/

            if (initResult) {
                resultText.text = "加载模型成功"
            } else {
                resultText.text = "加载模型失败"
            }
        }

        startBtn.setOnClickListener {
            resultText.text = "开始识别"
            val bitmap3 = getBitmap(R.drawable.test4)

            val result = ocr.detectBitmap(bitmap3, drawModel = DrawModel.Full)
            if (result == null) {
                resultText.text = "识别失败"
                Log.e(TAG, "onFail: 识别失败！")
            } else {
                val simpleText = result.text
                val inferenceTime = result.inferenceTime
                val outputRawResult = result.textLines

                var text = "识别文字=\n$simpleText\n识别时间=$inferenceTime ms\n更多信息=\n"

                println(text)

                outputRawResult.forEachIndexed { index, ocrResultModel ->
                    text += "$index: 文字：${ocrResultModel.text}，文字方向：${ocrResultModel.orientation}；置信度：${ocrResultModel.confidence}；文字位置：${ocrResultModel.points}；文字详情：${ocrResultModel.textList.map { "${it.text}(${it.confidence},${it.id})" }}\n"
                    println(text)
                }

                resultText.text = text
                resultImg.setImageBitmap(result.drawBitmap ?: bitmap3)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 释放
        ocr.release()
    }

    /**
     * 自定义 option 获取 Drawable 中的图片，避免获取到的 Bitmap 被缩放
     *
     * 来自：https://blog.csdn.net/qiantanlong/article/details/87712906
     * */
    private fun getBitmap(resId: Int): Bitmap {
        val options = BitmapFactory.Options()
        val value = TypedValue()
        resources.openRawResource(resId, value)
        options.inTargetDensity = value.density
        options.inScaled = false //不缩放
        return BitmapFactory.decodeResource(resources, resId, options)
    }
}