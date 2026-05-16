# ScanText — 离线 OCR 文字识别

基于 PaddleOCR 的离线文字识别 Android 应用，支持 7 种语言，无需联网即可使用。

## 下载

| 版本 | 下载 |
|------|------|
| **v2.0.0**（最新） | [ScanText-v2.0.0-debug.apk](https://github.com/jangviktor-web/PaddleOCR-ScanText/releases/download/v2.0.0/ScanText-v2.0.0-debug.apk) |

> APK 大小约 48MB，包含 PaddleOCR 模型文件。

## 功能特性

- **离线识别** — 无需联网，本地运行 PaddleOCR 引擎
- **7 种语言** — 中文、英文、日语、韩语、法语、德语、俄语
- **结巴分词** — 集成中文分词引擎，智能分词
- **三种交互模式**：
  - **框选** — 点击图片上的词组进行选择
  - **逐行** — 点击整行文字进行选择
  - **分词** — 底部 chip 网格逐词选择
- **图片触摸选择** — 直接在图片上点击选择文字，蓝色高亮反馈
- **复制 & 分享** — 一键复制或分享识别结果
- **Material Design 3** — 支持深色/浅色主题

## 截图

![OCR 识别界面](/doc/screenshot4.png)

## 技术架构

| 组件 | 技术 |
|------|------|
| OCR 引擎 | PaddleOCR v1.2.9（Paddle Lite） |
| 中文分词 | houbb/segment 0.3.1（结巴词库） |
| UI 框架 | Material Design 3 + AndroidX |
| 最低版本 | Android 7.0（API 24） |
| 构建工具 | AGP 8.7.3 + Kotlin 1.9.24 |

## 交互模式说明

### 框选模式
图片上每个 OCR 识别出的文字区域都有蓝色边框，点击任意词组即可选中（蓝色填充高亮），支持多选。

### 逐行模式
图片上显示行级别的矩形框，点击任意一行即可选中整行，适合快速复制大段文字。

### 分词模式
底部显示结巴分词后的词条卡片（3列网格），点击词条进行选择，同时图片上也支持点击选 box。

## 从源码构建

```bash
# 克隆仓库
git clone https://github.com/jangviktor-web/PaddleOCR-ScanText.git
cd PaddleOCR-ScanText

# 编译 Debug APK
./gradlew assembleDebug

# APK 输出路径
# app/build/outputs/apk/debug/app-debug.apk
```

## 项目结构

```
├── app/                          # 主应用模块
│   └── src/main/
│       ├── java/.../
│       │   ├── MainActivity.kt   # 主界面逻辑
│       │   ├── OcrOverlayView.kt # 图片触摸交互覆盖层
│       │   └── WordAdapter.kt    # 分词 chip 适配器
│       └── res/
│           ├── layout/           # 布局文件
│           ├── drawable/         # 图标 & 背景
│           └── values/           # 主题 & 颜色
├── PaddleOCR4Android/            # PaddleOCR SDK 模块
├── build.gradle                  # 项目构建配置
└── settings.gradle
```

## 致谢

- [PaddleOCR](https://github.com/PaddlePaddle/PaddleOCR) — 百度开源 OCR 引擎
- [paddleocr4android](https://github.com/equationl/paddleocr4android) — Android 封装
- [houbb/segment](https://github.com/houbb/segment) — 中文分词库

## License

MIT License
