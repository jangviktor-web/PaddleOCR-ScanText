# PaddleOCR-ScanText — 离线多语言 OCR 文字识别

基于 PaddleOCR 的离线文字识别 Android 应用，支持中英日繁体及韩文，无需联网即可使用。

## 下载

| 版本 | 下载 |
|------|------|
| **v2.3.0**（最新） | [PaddleOCR-v2.3.0-PP-OCRv5-v2.apk](https://github.com/jangviktor-web/PaddleOCR-ScanText/releases/download/v2.3.0/PaddleOCR-v2.3.0-PP-OCRv5-v2.apk) |
| v2.2.0 | [app-v2.2.0-more.apk](https://github.com/jangviktor-web/PaddleOCR-ScanText/releases/download/v2.2.0/app-v2.2.0-more.apk) |

> APK 大小约 61MB，已内置全部语言模型，安装即可使用。

## 支持语言

| 语言 | 模型 | 说明 |
|------|------|------|
| 中英 日PP-OCRv5 | PP-OCRv5 | 默认语言，单模型覆盖中文、英文、日文、繁体中文 |
| 한국어 | PP-OCRv3 | 韩文识别（精度约 60%） |

## 功能特性

- **离线识别** — 无需联网，本地运行 PaddleOCR 引擎
- **PP-OCRv5 引擎** — 最新 OCR 模型，单模型支持中英日繁体，支持 109 种语言
- **多语言切换** — 选择语言后自动加载对应模型
- **结巴分词** — 集成中文分词引擎，智能分词
- **三种交互模式**：
  - **框选** — 点击图片上的词组进行选择
  - **逐行** — 点击整行文字进行选择
  - **分词** — 底部 chip 网格逐词选择
- **图片触摸选择** — 直接在图片上点击选择文字，蓝色高亮反馈
- **识别历史** — 自动保存识别记录，支持查看、删除、清空
- **关于页面** — 应用信息与开源项目致谢
- **复制 & 分享** — 一键复制或分享识别结果
- **返回清空** — 返回时自动清空识别图片和结果

## 截图

<img width="2057" height="1440" alt="20260516-175830" src="https://github.com/user-attachments/assets/68be9ac2-1681-489f-a69b-bf5ad520968c" />


## 技术架构

| 组件 | 技术 |
|------|------|
| OCR 引擎 | PaddleOCR + Paddle Lite |
| Android 封装 | paddleocr4android v1.2.9 |
| 中文分词 | houbb/segment 0.3.1（结巴词库） |
| 识别历史 | Room 数据库 |
| UI 框架 | Material Design 3 + AndroidX |
| 最低版本 | Android 7.0（API 24） |

## 交互模式说明

### 框选模式
图片上每个 OCR 识别出的文字区域都有蓝色边框，点击任意词组即可选中（蓝色填充高亮），支持多选。

### 逐行模式
图片上显示行级别的矩形框，点击任意一行即可选中整行，适合快速复制大段文字。

### 分词模式
底部显示结巴分词后的词条卡片（3列网格），点击词条进行选择，同时图片上也支持点击选 box。

## 识别历史

每次识别成功后自动保存记录，包括：
- 原始图片缩略图
- 识别文字摘要
- 识别语言
- 识别时间

支持长按删除单条记录，或一键清空全部。

## 从源码构建

```bash
# 克隆仓库
git clone https://github.com/jangviktor-web/PaddleOCR-ScanText.git
cd PaddleOCR-ScanText

# 编译 Release APK（需配置签名）
./gradlew assembleRelease

# APK 输出路径
# app/build/outputs/apk/release/app-release.apk
```

## 项目结构

```
├── app/                          # 主应用模块
│   └── src/main/
│       ├── java/.../
│       │   ├── MainActivity.kt     # 主界面逻辑
│       │   ├── MoreActivity.kt     # 更多页面（历史+关于）
│       │   ├── HistoryActivity.kt  # 识别历史页面
│       │   ├── AboutDialog.kt      # 关于弹窗
│       │   ├── HistoryEntity.kt    # Room 数据实体
│       │   ├── HistoryDao.kt       # Room 数据访问
│       │   ├── HistoryDatabase.kt  # Room 数据库
│       │   ├── OcrOverlayView.kt   # 图片触摸交互覆盖层
│       │   └── WordAdapter.kt      # 分词 chip 适配器
│       ├── assets/
│       │   ├── models/             # OCR 模型文件（.nb）
│       │   └── labels/             # 字典文件
│       └── res/
│           ├── layout/             # 布局文件
│           ├── drawable/           # 图标 & 背景
│           └── values/             # 主题 & 颜色
├── PaddleOCR4Android/              # PaddleOCR SDK 模块
├── build.gradle                    # 项目构建配置
└── settings.gradle
```

## 致谢

- [PaddleOCR](https://github.com/PaddlePaddle/PaddleOCR) — 百度飞桨 OCR 识别引擎
- [Paddle Lite](https://github.com/PaddlePaddle/Paddle-Lite) — 百度飞桨移动端推理框架
- [paddleocr4android](https://github.com/equationl/paddleocr4android) — PaddleOCR Android 封装库
- [segment](https://github.com/houbb/segment) — 中文分词库
- [Material Components](https://github.com/material-components/material-components-android) — Google Material Design UI 组件
- [AndroidX](https://github.com/androidx/androidx) — Jetpack Android 基础库

## 更新日志

### v2.3.0（2026-05-16）
- 升级至 PP-OCRv5 引擎，单模型覆盖中文、英文、日文、繁体中文
- 移除单独的日语、繁体中文、中英 PP-OCRv4 模型，APK 从 102MB 降至 61MB
- 保留韩语 PP-OCRv3 选项（PP-OCRv5 不支持韩文）

### v2.2.0（2026-05-16）
- 新增「更多」页面，包含识别历史和关于功能
- 识别历史：Room 数据库自动保存记录，支持查看、删除、清空
- 关于弹窗：展示应用信息与开源项目致谢
- 修复返回按钮不清空图片和结果的问题
- 修复 APK 签名问题，使用 gradle 签名配置

### v2.1（2026-05-16）
- 内置多语言模型（中英日韩繁体），无需额外下载
- 移除模型下载功能，开箱即用
- 优化交互体验

### v2.0（2026-05-16）
- 全面升级 UI 至 Material Design 3
- 集成结巴中文分词引擎
- 新增图片触摸交互：框选、逐行、分词三种模式
- 支持多语言切换（中文、英文、日文、韩文、繁体中文）

### v1.0（2026-02-11）
- 初始版本，基础 OCR 文字识别功能
- JitPack CI 构建支持

## License

MIT License
