[![](https://jitpack.io/v/equationl/paddleocr4android.svg)](https://jitpack.io/#equationl/paddleocr4android)

# 简介

该库是对 [ncnn-android-ppocrv5](https://github.com/nihui/ncnn-android-ppocrv5) 进行二次封装的库。

对于只想体验或者快速上手使用的安卓开发者，该库对 [ncnn-android-ppocrv5](https://github.com/nihui/ncnn-android-ppocrv5) 样例进行了简单的封装，使其可以直接上手使用，而无需关心 PaddleOCR 的实现，亦无需进行繁琐的配置。

基于 *Ncnn* 部署

截图：

![截图](/doc/screenshot4.png)

# 注意

目前仅测试了 ppocrv5 模型可用，其他版本模型是否可用需要自行测试。

另外，相较于原样例，该库做了如下修改：

1. 为了精简包大小，移除了 C++ 代码中所有相机相关的内容，改为传入图片路径或 Bitmap 识别。如果需要使用相机实时识别的话，请参考原项目添加回去或结合自身业务自行在 kotlin 层实现。
2. 加载模型方式增加从本地任意路径加载的方法（可能需要申请储存权限，建议放到 APP 私有目录，避免权限问题导致加载失败）；原 demo 中的 assets 加载方式仍然保留，方便使用 assets 中的模型。

# 使用方法

因为模型较大，没有放到项目中，请参考下载模型、加载模型两节内容放置好模型后，直接运行 demo （[ncnndemo](../ncnndemo)）即可体验。

如需集成至您自己的项目中，请按下述步骤进行：

## 1.下载依赖

首先，根据你使用的 Gradle 版本在项目级 *build.gradle* 或 *settings.gradle* 文件添加 jitpack 远程仓库：

```gradle
allprojects {
	repositories {
		...
		maven { url 'https://jitpack.io' }
	}
}
```

然后在 Module 级 *build.gradle* 文件添加依赖：

```gradle
dependencies {
    implementation 'com.github.equationl.paddleocr4android:ncnnandroidppocr:v1.3.0'
}
```

## 2.下载模型

### 下载模型的渠道

1. 预设模型

预设了几个可直接使用的模型：

[模型下载](https://github.com/equationl/ncnn-android-ppocrv5/tree/master/app/src/main/assets)

其中 PP_OCRv5_mobile_xxx 为端侧模型，PP_OCRv5_server_xxx 为服务器模型。

建议使用端侧模型，虽然识别率可能略有下降，但速度会更快，体积更小，且更适合在手机上使用。

2. 自行转换模型

可按照此处教程自行转换模型：[模型转换教程](https://github.com/equationl/ncnn-android-ppocrv5?tab=readme-ov-file#guidelines-for-converting-ppocrv5-models)

## 3.加载模型

目前支持两种模型加载方式，一种是直接加载 assets 中的模型，另一种是从本地任意路径加载模型。

从 assets 加载需要先把模型放到 assets 中，且严格按照以下文件名命名对应的模型文件：

端侧模型命令：

- PP_OCRv5_mobile_det.ncnn.bin
- PP_OCRv5_mobile_det.ncnn.param
- PP_OCRv5_mobile_rec.ncnn.bin
- PP_OCRv5_mobile_rec.ncnn.param

服务端模型命名：

- PP_OCRv5_server_det.ncnn.bin
- PP_OCRv5_server_det.ncnn.param
- PP_OCRv5_server_rec.ncnn.bin
- PP_OCRv5_server_rec.ncnn.param

模型放置完成后，使用下述代码加载模型：

```kotlin
ocr = OCR()
// 1. 从 assets 目录加载预置模型（无需申请存储权限，适合初次使用快速体验）
val initResult = ocr.initModelFromAssert(assets, ModelType.Mobile, ImageSize.Size720, Device.CPU)

if (initResult) {
    // 加载模型成功
} else {
    // 加载模型失败
}
```

你也可以自行处理模型的下载，然后将模型的本地路径传递给 OCR 进行加载：

```kotlin
ocr = OCR()
// 2. 从指定路径加载模型（需要申请存储权限，适合使用自定义模型或不想将模型文件放置在 assets 目录的情况）
val initResult2 = ocr.initModel(
    detParamPath = "/sdcard/models/PP_OCRv5_mobile_det.ncnn.param",
    detModelPath = "/sdcard/models/PP_OCRv5_mobile_det.ncnn.bin",
    recParamPath = "/sdcard/models/PP_OCRv5_mobile_rec.ncnn.param",
    recModelPath = "/sdcard/models/PP_OCRv5_mobile_rec.ncnn.bin",
    reSize = ImageSize.Size720,
    useDevice = Device.CPU,
    useFp16 = true
)
if (initResult) {
    // 加载模型成功
} else {
    // 加载模型失败
}
```

## 4.开始使用

直接从 Bitmap 识别：

```kotlin
val result = ocr.detectBitmap(bitmap3, drawModel = DrawModel.Full)  // drawModel = DrawModel.Full 表示要将识别结果绘制在 Bitmap 上返回，使用时建议设置为 DrawModel.None
if (result == null) {
    resultText.text = "识别失败"
    Log.e(TAG, "onFail: 识别失败！")
}
else {
    val simpleText = result.text
    val inferenceTime = result.inferenceTime
    val outputRawResult = result.textLines

    var text = "识别文字=\n$simpleText\n识别时间=$inferenceTime ms\n更多信息=\n"

    outputRawResult.forEachIndexed { index, ocrResultModel ->
        text += "$index: 文字：${ocrResultModel.text}，文字方向：${ocrResultModel.orientation}；置信度：${ocrResultModel.confidence}；文字位置：${ocrResultModel.points}\n"
    }

    resultText.text = text
    resultImg.setImageBitmap(result.drawBitmap ?: bitmap3)
}
```

从本地图片路径识别：

```kotlin
val result = ocr.detectImagePath("/sdcard/test.jpg", drawModel = DrawModel.Full)  // drawModel = DrawModel.Full 表示要将识别结果绘制在 Bitmap 上返回，使用时建议设置为 DrawModel.None
if (result == null) {
    resultText.text = "识别失败"
    Log.e(TAG, "onFail: 识别失败！")
}
else {
    val simpleText = result.text
    val inferenceTime = result.inferenceTime
    val outputRawResult = result.textLines

    var text = "识别文字=\n$simpleText\n识别时间=$inferenceTime ms\n更多信息=\n"

    outputRawResult.forEachIndexed { index, ocrResultModel ->
        text += "$index: 文字：${ocrResultModel.text}，文字方向：${ocrResultModel.orientation}；置信度：${ocrResultModel.confidence}；文字位置：${ocrResultModel.points}\n"
    }

    resultText.text = text
    resultImg.setImageBitmap(result.drawBitmap ?: bitmap3)
}
```

## 5.其他

有任何问题请先尝试 demo 或阅读源码，如果无法解决请提 issue


# 更新记录
**v1.3.0**
- 初始版本，基于 ncnn-android-ppocrv5 样例进行二次封装，提供更简单的接口供安卓开发者使用