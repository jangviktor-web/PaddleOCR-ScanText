# PaddleOCR4Android — 离线多语言 OCR 文字识别

基于 PaddleOCR 的离线文字识别 Android 应用，支持中英日繁体及韩文，无需联网即可使用。

## 下载

| 版本 | 下载 | 说明 |
|------|------|------|
| **v2.7.0**（最新） | [PaddleOCR-v2.7.0.apk](https://github.com/jangviktor-web/PaddleOCR4Android/releases/download/v2.7.0/PaddleOCR-v2.7.0.apk) | 分词拖拽选词 + 自滚动 + 框选阅读顺序 |
| v2.6.3 | [PaddleOCR-v2.6.3.apk](https://github.com/jangviktor-web/PaddleOCR4Android/releases/download/v2.6.3/PaddleOCR-v2.6.3.apk) | 分词逐行分词 + 框选顺序修复 |
| v2.6.1 | [PaddleOCR-v2.6.1.apk](https://github.com/jangviktor-web/PaddleOCR4Android/releases/download/v2.6.1/PaddleOCR-v2.6.1.apk) | 框选拖拽 + 分词流式布局 |
| v2.3.0 | [PaddleOCR-v2.3.0.apk](https://github.com/jangviktor-web/PaddleOCR4Android/releases/download/v2.3.0/PaddleOCR-v2.3.0-original.apk) | PP-OCRv5 引擎升级 |

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
- **结巴分词** — 集成中文分词引擎，按原文行结构智能分词，保留阅读顺序
- **三种交互模式**：
  - **框选** — 手指拖动画矩形，框选区域内所有文字一次性选中
  - **逐行** — 点击一行选中整行，支持连续点击累加多行
  - **分词** — WPS 风格流式词块布局，支持点击勾选和拖拽连续选词
- **图片触摸选择** — 直接在图片上点击选择文字，蓝色高亮反馈
- **识别历史** — 自动保存识别记录，支持查看、删除、清空
- **关于页面** — 应用信息与开源项目致谢
- **复制 & 分享** — 一键复制或分享识别结果
- **返回清空** — 返回时自动清空识别图片和结果

## 截图

<img width="2592" height="1440" alt="Image" src="https://github.com/user-attachments/assets/290d150a-6705-476f-8175-9c46002a58ce" />


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
在图片上用手指拖动画出一个矩形区域，区域内所有 OCR 识别出的文字框都会被选中（蓝色填充高亮）。框选完成后，底部面板自动显示识别出的文字内容。也支持点击单个词框进行切换选中/取消。

### 逐行模式
图片上显示行级别的矩形框，点击任意一行即可选中整行。支持连续点击不同行进行累加选择，适合快速复制大段文字中的多个段落。再次点击已选中的行可以取消选中。

### 分词模式
识别结果以流式词块卡片展示（类似手机 WPS 的文字选词功能），每个词块可独立点击勾选。支持拖拽手势连续选中多个相邻词块，适合快速选取词组或短语。内置自滚动功能，长文本无需翻页即可连续选词。

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
git clone https://github.com/jangviktor-web/PaddleOCR4Android.git
cd PaddleOCR4Android

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
│       │   └── FlowLayout.kt       # 分词流式词块布局
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

---

## 更新日志

### v2.7.0（2026-05-17）

**FlowLayout 自滚动 + 拖拽连续选词**

这一版本彻底解决了分词模式下跨页选词丢失和无法拖拽批量选词的问题。

**新增功能：**
- **分词拖拽连续选词**：在分词面板中，手指从一个词块开始水平拖拽到另一个词块，松手后区间内所有词块自动选中。适合快速选取"望庐山瀑布"这类连续词组，无需逐字点击
- **FlowLayout 内置自滚动**：分词面板不再依赖外部 ScrollView，自行管理滚动状态。拖拽选词时手指超出面板边界会自动滚动，解决了之前 ScrollView 拦截触摸事件导致选中状态丢失的问题

**修复：**
- 修复分词模式下跨页（滚动前后）选中内容丢失的问题。原因是 ScrollView 拦截了 FlowLayout 的触摸事件，导致 FlowLayout 无法收到 ACTION_UP 事件来确认选中状态
- 修复拖拽选词与滚动操作的冲突。FlowLayout 现在根据手势方向自动判断：垂直拖拽为滚动，水平拖拽为选词

**技术细节：**
- FlowLayout 从 ScrollView 包裹改为独立自管理 View，在 `onMeasure` 中计算完整内容高度和 `maxScrollY`
- 触摸事件处理：`ACTION_DOWN` 记录起始位置和锚点词块；`ACTION_MOVE` 判断手势方向（垂直 > 1.5x 水平 = 滚动，否则 = 选词）；`ACTION_UP` 确认选中或触发惯性滚动
- 绘制通过 `canvas.translate(0f, -scrollY.toFloat())` 实现滚动偏移，仅绘制可见区域内的词块
- 惯性滚动基于 ACTION_UP 时的滑动速度，300ms 内线性衰减

---

### v2.6.3（2026-05-17）

**分词逐行分词 + 框选阅读顺序修复**

这一版本修复了两个核心顺序 bug：长文本分词乱序和框选复制顺序不符合阅读习惯。

**修复：**
- **分词顺序乱序**：修复《兰亭集序》等长文本进入分词模式后文字乱序、词块错位的问题。原因是之前将所有识别文字拼接成一个字符串后交给分词器处理，分词器重新分词打乱了原始行结构。改为逐行独立分词，每行文字单独交给分词器处理后再映射回对应的 box 索引
- **框选复制顺序**：修复框选模式下复制出的文字不按阅读顺序排列的问题。之前直接用 box 索引排序（检测顺序），现在改为按阅读顺序排列：先按行号从上到下，再按行内位置从左到右

**技术细节：**
- `segmentByLine()` 新方法替代原来的 `mapSegmentToBoxes()`：遍历每行分组，独立调用结巴分词器，保留每行内的 `lineIndex` 和 `positionInLine` 位置信息
- `OcrOverlayView.sortByReadingOrder()` 方法：通过 `lineIndices` 映射表将 box 索引转换为 `(lineIdx, posInLine)` 坐标对，再用 `compareBy` 排序
- `OcrOverlayView` 中新增 `onMultiBoxSelected` 回调，在框选拖拽完成时触发

---

### v2.6.1（2026-05-17）

**框选拖拽选区 + 分词流式布局 + 复制修复**

这一版本重新定义了框选模式的交互方式，并将分词模式从 RecyclerView 升级为自定义 FlowLayout。

**新增功能：**
- **框选拖拽选区**：框选模式改为手指在图片上拖动画出虚线矩形，矩形区域内所有文字框自动选中（蓝色虚线边框 + 半透明填充），选中后底部面板显示文字内容
- **分词流式布局**：分词模式从 RecyclerView 3列网格改为 WPS 风格的流式词块卡片布局，每个词块按语义拆分后流式排列，视觉效果更接近手机版 WPS 的文字选词功能
- **逐行模式累加选中**：逐行模式支持连续点击不同行进行多行累加选择，再次点击已选中的行可取消

**修复：**
- 修复复制粘贴时每个字符单独一行的问题。PP-OCR 的 `simpleText` 返回 `\r\n` 行尾符，`split("\n")` 后每行末尾残留 `\r`，粘贴到微信等应用时导致每个字符被当作独立行。在所有文本处理入口添加 `.replace("\r", "")` 清理

**UI 调整：**
- 图片预览区背景从纯黑色 (#000000) 改为浅灰白色（与顶部工具栏一致）
- 新增 OcrOverlayView 中的拖拽选区绘制（`dragRectFillPaint` 半透明蓝色填充 + `dragRectStrokePaint` 蓝色虚线边框）

---

### v2.3.0（2026-05-16）

**PP-OCRv5 引擎升级**

从 PP-OCRv4 升级至 PP-OCRv5，大幅减小 APK 体积并提升识别精度。

**变更：**
- 引擎从 PP-OCRv4 升级至 PP-OCRv5，单模型覆盖中文、英文、日文、繁体中文，支持 109 种语言
- 移除单独的日语模型、繁体中文模型、中英 PP-OCRv4 模型，APK 从 102MB 降至 61MB
- 保留韩语 PP-OCRv3 选项（PP-OCRv5 不支持韩文）
- 新增韩文词典文件 `ppocrv5_korean_dict.txt`（11945 行）

**技术细节：**
- PP-OCRv5 模型文件：det.nb (4.8MB)、rec.nb (15.9MB)、cls.nb (1.0MB)
- 模型来源：paddlelite-demo 官方预转换 .nb 格式

---

### v2.2.0（2026-05-16）

**更多页面 + 识别历史 + 关于**

新增应用管理功能，识别结果自动保存。

**新增功能：**
- **更多页面**：点击工具栏右侧菜单按钮进入，包含识别历史和关于两个入口
- **识别历史**：使用 Room 数据库自动保存每次识别的记录，包括图片缩略图、识别文字摘要、语言和时间。支持长按删除和一键清空
- **关于弹窗**：展示应用版本信息、功能介绍、开源项目致谢

**修复：**
- 修复返回按钮不清空识别图片和结果数据的问题
- 修复 APK 签名问题，改用 gradle 内置 debug keystore 签名配置

---

### v2.1.0（2026-05-16）

**多语言模型内置**

所有语言模型内置到 APK，开箱即用。

**变更：**
- 内置中英日韩繁体五种语言的 OCR 模型到 APK 中
- 移除在线模型下载功能，无需网络即可使用
- 优化多语言切换体验

---

### v2.0.0（2026-05-16）

**全面升级 — Material Design 3 + 分词 + 触摸交互**

应用架构全面重构，从基础 OCR 工具升级为功能完整的文字识别应用。

**新增功能：**
- **Material Design 3 UI**：全新设计语言，深色/浅色主题自适应
- **结巴中文分词**：集成 houbb/segment 分词引擎，支持智能分词
- **图片触摸交互**：直接在图片上点击/框选文字，三种模式（框选、逐行、分词）
- **多语言切换**：支持中文、英文、日文、韩文、繁体中文切换
- **复制 & 分享**：一键复制识别结果或分享到其他应用

---

### v1.0（2026-02-11）

**初始版本**

- 基础 OCR 文字识别功能
- JitPack CI 构建支持
- 支持中英文识别

## License

MIT License
