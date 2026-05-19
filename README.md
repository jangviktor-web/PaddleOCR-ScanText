# PaddleOCR4Android
 **[English Version](./README_EN.md)**
### 基于PaddleOCR的Android离线OCR文字识别应用

一款将强大PaddleOCR引擎封装为易于使用的Android应用，**无需联网**，支持**中、英、日、韩及繁体中文**等多语言实时识别。

<p align="center">
  <img width="800" alt="PaddleOCR4Android 截图" src="https://github.com/user-attachments/assets/290d150a-6705-476f-8175-9c46002a58ce">
</p>

## ✨ 核心亮点

- **完全离线**：所有识别在本地完成，无需网络，保护隐私且不受环境限制。
- **PP-OCRv5 引擎**：搭载最新一代OCR模型，单模型精准识别中、英、日、繁体中文。
- **智能交互**：提供 **框选、逐行、分词** 三种模式，满足从段落摘录到精准选词的多样化需求。
- **开箱即用**：APK内置全部语言模型（约61MB），安装即享识别服务。
- **无缝集成**：为Android开发者提供清晰的架构和源码，便于集成到自己的项目中。

## 📥 下载安装

直接下载最新版APK，安装到您的Android设备即可使用。

| 版本 | 下载链接 | 更新重点 |
| :--- | :--- | :--- |
| **v2.7.0 (最新)** | [PaddleOCR-v2.7.0.apk](https://github.com/jangviktor-web/PaddleOCR4Android/releases/download/v2.7.0/PaddleOCR-v2.7.0.apk) | 🚀 **分词模式升级**：支持拖拽连续选词与自滚动，选词更流畅 |
| v2.6.3 | [PaddleOCR-v2.6.3.apk](https://github.com/jangviktor-web/PaddleOCR4Android/releases/download/v2.6.3/PaddleOCR-v2.6.3.apk) | 📄 优化长文本分词顺序与框选复制顺序 |
| v2.6.1 | [PaddleOCR-v2.6.1.apk](https://github.com/jangviktor-web/PaddleOCR4Android/releases/download/v2.6.1/PaddleOCR-v2.6.1.apk) | 🖼️ 引入框选拖拽选区与流式分词布局 |
| v2.3.0 | [PaddleOCR-v2.3.0.apk](https://github.com/jangviktor-web/PaddleOCR4Android/releases/download/v2.3.0/PaddleOCR-v2.3.0-original.apk) | ⚙️ 核心引擎升级至PP-OCRv5 |

> **最低系统要求**：Android 7.0 (API 24)

## 🌍 支持语言

| 语言组 | 使用的模型 | 说明 |
| :--- | :--- | :--- |
| **中/英/日/繁体** | PP-OCRv5 | **默认语言**，单模型覆盖，识别率高。 |
| **한국어 (韩语)** | PP-OCRv3 | 韩语识别（精度约60%，暂未集成PP-OCRv5模型）。 |

## 🛠️ 功能详解

### 核心识别能力
1.  **离线识别**：摆脱网络依赖，随时随地可用。
2.  **多语言切换**：在设置中一键切换识别语言，自动加载对应模型。
3.  **智能中文分词**：集成结巴分词引擎，按原文结构进行语义分词，保持阅读顺序。

### 三种创新交互模式
在识别结果界面，您可以通过以下三种模式与文字互动：

| 模式 | 操作方式 | 适用场景 |
| :--- | :--- | :--- |
| **🖼️ 框选模式** | 在图片上**拖拽**绘制矩形区域，选中区域内所有文字。 | 快速选取一大段不规则排版的文字。 |
| **📝 逐行模式** | **点击**任意一行文字即可选中整行，支持多行累加选择。 | 快速复制多行段落内容。 |
| **🔗 分词模式** | 结果以流式词块呈现，**点击**选词，或**水平拖拽**连续选词。 | 精准复制词语、短语，操作类似手机WPS。 |

> 选中状态会实时在图片上以蓝色高亮显示，并直接在底部面板展示文本结果。

### 实用功能
- **复制与分享**：一键将识别结果复制到剪贴板或分享至其他应用。
- **历史记录**：自动保存识别历史，可随时查看、删除或清空。
- **即时反馈**：返回时自动清空当前内容，为下次识别做好准备。

## ⚙️ 技术架构

本项目采用模块化设计，技术栈清晰：

| 组件 | 技术选型 | 作用 |
| :--- | :--- | :--- |
| **OCR 引擎** | PaddleOCR + Paddle Lite | 核心识别与移动端推理。 |
| **Android 封装** | paddleocr4android | 提供便捷的Android API调用接口。 |
| **中文分词** | houbb/segment (结巴词库) | 实现智能中文分词功能。 |
| **数据持久化** | Room | 存储识别历史记录。 |
| **用户界面** | Material Design 3 + AndroidX | 提供现代、美观的UI组件。 |

## 📂 从源码构建

欢迎开发者克隆、研究和贡献代码！

```bash
# 1. 克隆仓库
git clone https://github.com/jangviktor-web/PaddleOCR4Android.git
cd PaddleOCR4Android

# 2. 在Android Studio中打开项目，或使用Gradle命令行构建
./gradlew assembleRelease

# 3. 生成的APK位于：
# app/build/outputs/apk/release/app-release.apk
```

## 🏗️ 项目结构

```
├── app/                          # 主应用模块
│   └── src/main/
│       ├── java/.../             # Kotlin源码
│       │   ├── MainActivity.kt     # 主界面与核心逻辑
│       │   ├── MoreActivity.kt     # 历史与关于页面
│       │   ├── OcrOverlayView.kt   # 图片触摸交互层
│       │   └── FlowLayout.kt       # 分词流式布局
│       ├── assets/models/          # 预置的OCR模型(.nb)
│       └── res/                    # 布局、图标、主题资源
├── PaddleOCR4Android/              # PaddleOCR SDK 模块
└── build.gradle                    # 全局构建配置
```

## ❤️ 致谢

本项目基于以下优秀的开源项目构建，特此感谢：
- [PaddlePaddle/PaddleOCR](https://github.com/PaddlePaddle/PaddleOCR)
- [PaddlePaddle/Paddle-Lite](https://github.com/PaddlePaddle/Paddle-Lite)
- [equationl/paddleocr4android](https://github.com/equationl/paddleocr4android)
- [houbb/segment](https://github.com/houbb/segment)
- [Material Components for Android](https://github.com/material-components/material-components-android)

## 📜 更新日志

<details>
<summary><strong>v2.7.0 (2026-05-17) - 分词交互革命</strong></summary>

**主要特性：**
- **分词拖拽选词**：在分词面板水平拖拽，可连续选中区间内所有词块，实现高效词组选择。
- **FlowLayout自滚动**：分词面板内置滚动管理，拖拽选词时超出边界会自动滚动，体验一气呵成。

**问题修复：**
- 彻底解决了长文本分词模式下，滚动导致选中状态丢失的顽疾。
- 优化了拖拽选词与页面滚动的手势冲突识别。
</details>

<details>
<summary><strong>v2.6.3 (2026-05-17) - 顺序优化</strong></summary>

**问题修复：**
- **分词顺序**：修复《兰亭集序》等长文本进入分词模式后文字乱序的问题。采用逐行独立分词策略。
- **框选顺序**：修复框选模式复制的文字不符合从上到下、从左到右阅读顺序的问题。
</details>

<details>
<summary><strong>v2.6.1 (2026-05-17) - 交互重塑</strong></summary>

**重大更新：**
- **框选拖拽**：框选模式变为拖拽绘制矩形区域，批量选中区域内文字。
- **流式分词**：分词布局升级为WPS风格的流式词块，更美观易用。
- **修复**：解决复制文本时每个字符单独一行的问题。
</details>

<details>
<summary><strong>v2.3.0 (2026-05-16) - 引擎升级</strong></summary>

**核心升级：**
- 将OCR引擎从PP-OCRv4升级至 **PP-OCRv5**，模型更小、精度更高、语言覆盖更广。
- APK体积从102MB降至61MB。
</details>

## 📄 许可证

本项目采用 [MIT 许可证](./LICENSE) 开源。
