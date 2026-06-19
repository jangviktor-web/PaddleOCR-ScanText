<p align="center">
  <img width="200" src="https://raw.githubusercontent.com/jangviktor-web/PaddleOCR4Android/master/app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" />
</p>

<h1 align="center">PaddleOCR4Android</h1>

<p align="center">
  <strong>基于 PaddleOCR 的 Android 离线 OCR 文字识别应用</strong><br>
  无需联网 · PP-OCRv5 引擎 · 中英日韩繁体 · 内置相机 · 智能分词
</p>

<p align="center">
  <a href="https://github.com/jangviktor-web/PaddleOCR4Android/releases/latest"><img src="https://img.shields.io/github/v/release/jangviktor-web/PaddleOCR4Android?style=flat-square&label=Latest Release" /></a>
  <a href="https://github.com/jangviktor-web/PaddleOCR4Android/blob/master/LICENSE"><img src="https://img.shields.io/github/license/jangviktor-web/PaddleOCR4Android?style=flat-square" /></a>
  <a href="https://github.com/jangviktor-web/PaddleOCR4Android/stargazers"><img src="https://img.shields.io/github/stars/jangviktor-web/PaddleOCR4Android?style=flat-square" /></a>
  <a href="https://img.shields.io/badge/Android-7.0%2B-green?style=flat-square&logo=android"><img src="https://img.shields.io/badge/Android-7.0%2B-green?style=flat-square&logo=android" /></a>
</p>

<p align="center">
  <a href="https://github.com/jangviktor-web/PaddleOCR4Android/releases/latest"><img src="https://img.shields.io/badge/Download-APK-blue?style=for-the-badge&logo=android" /></a>
  <a href="https://github.com/jangviktor-web/PaddleOCR4Android"><img src="https://img.shields.io/badge/Source-Code-black?style=for-the-badge&logo=github" /></a>
</p>

<p align="center">
  <img width="100%" alt="PaddleOCR4Android" src="https://github.com/user-attachments/assets/29d823dd-fbd9-4e82-a041-647a943943db" />
</p>

<p align="center">
  <img width="100%" alt="v2.7.2.2 Features" src="https://github.com/user-attachments/assets/ba6926fc-2dcf-42ea-a9a6-70c90d28abfc" />
</p>

---

## Features

- **完全离线**：所有识别在本地完成，无需网络，保护隐私。
- **PP-OCRv5 引擎**：最新一代 OCR 模型，单模型覆盖中/英/日/繁体。
- **内置相机**：CameraX 相机，双指缩放、点击对焦、长按锁定曝光、闪光灯切换、构图网格线。
- **智能交互**：框选 / 逐行 / 分词 / 裁剪 四种模式，满足不同选取需求。
- **中文分词**：集成结巴分词引擎，按语义分词保持阅读顺序。
- **历史记录**：自动保存识别历史，支持查看、删除、清空。
- **多语言**：中/英/日/繁体（PP-OCRv5）、韩语（PP-OCRv3）。
- **设置中心**：拍照分辨率、相册图片尺寸、缓存管理。

## Download

| 版本 | 下载 | 说明 |
|:---|:---|:---|
| **v2.7.2.2** (最新) | [📥 Releases](https://github.com/jangviktor-web/PaddleOCR4Android/releases/tag/v2.7.2.2) | 分辨率同步 + 复制换行修复 |
| v2.7.2.0 | [📥 Releases](https://github.com/jangviktor-web/PaddleOCR4Android/releases/tag/v2.7.2.0) | 对焦锁定 + 闪光灯 + 网格线 |
| v2.7.1.6 | [📥 Releases](https://github.com/jangviktor-web/PaddleOCR4Android/releases/tag/v2.7.1.6) | CameraX 内置相机 + 双指缩放 |
| v2.7.0 | [📥 Releases](https://github.com/jangviktor-web/PaddleOCR4Android/releases/tag/v2.7.0) | 分词拖拽选词 + 自滚动 |

> **最低要求**：Android 7.0 (API 24) · APK 约 81MB

## Tech Stack

- [PaddleOCR](https://github.com/PaddlePaddle/PaddleOCR) + [Paddle Lite](https://github.com/PaddlePaddle/Paddle-Lite) — OCR 引擎与移动端推理
- [CameraX 1.3.1](https://developer.android.com/media/camera/camerax) — 内置相机预览、拍照、变焦
- [paddleocr4android](https://github.com/equationl/paddleocr4android) — Android API 封装
- [houbb/segment](https://github.com/houbb/segment) — 中文分词（结巴词库）
- [Room](https://developer.android.com/training/data-storage/room) — 识别历史持久化
- [Material Design 3](https://github.com/material-components/material-components-android) — UI 组件

## Build from Source

### Requirements

| Tool | Version | Note |
|:---|:---|:---|
| JDK | 17+ | Android Studio JBR recommended |
| Android SDK | API 35 | `compileSdk 35` |
| Build Tools | 35.0.1 | |
| **NDK** | **r21e** | ⚠️ Required — NDK 25+ rejects PaddleLite symbols |
| CMake | 3.22.1 | Native C++ compilation |

### Quick Start

```bash
git clone https://github.com/jangviktor-web/PaddleOCR4Android.git
cd PaddleOCR4Android
./gradlew :app:assembleRelease
# APK: app/build/outputs/apk/release/app-release.apk
```

### Setup Guide

<details>
<summary><strong>JDK Installation</strong></summary>

**Windows:** Use Android Studio JBR, or install from [Adoptium](https://adoptium.net/temurin/releases/?version=17)
**macOS:** `brew install openjdk@17`
**Linux:** `sudo apt install openjdk-17-jdk`
</details>

<details>
<summary><strong>Android SDK & NDK</strong></summary>

Via Android Studio → SDK Manager, install:
- SDK Platforms → Android 15 (API 35)
- SDK Tools → Build-Tools 35.0.1
- SDK Tools → NDK (Side by side) → **21.4.7075529**
- SDK Tools → CMake → 3.22.1

Or via command line:
```bash
sdkmanager "platforms;android-35" "build-tools;35.0.1" "ndk;21.4.7075529" "cmake;3.22.1"
```
</details>

<details>
<summary><strong>FAQ</strong></summary>

**Q: `found local symbol in global part of symbol table`**
A: Wrong NDK version. Must use NDK r21e. Set `ndkVersion '21.4.7075529'` in `PaddleOCR4Android/build.gradle`.

**Q: Gradle dependency download timeout**
A: Configure proxy in `~/.gradle/gradle.properties`:
```properties
systemProp.http.proxyHost=your_proxy_host
systemProp.http.proxyPort=your_proxy_port
systemProp.https.proxyHost=your_proxy_host
systemProp.https.proxyPort=your_proxy_port
```

**Q: `Failed to find Build Tools revision 34.0.0`**
A: Check `buildToolsVersion` in `PaddleOCR4Android/build.gradle` matches installed version.
</details>

## Project Structure

```
app/
  └── src/main/java/
      ├── MainActivity.kt          # OCR 主界面与识别逻辑
      ├── CameraActivity.kt        # CameraX 相机（对焦/缩放/闪光灯）
      ├── SettingsActivity.kt      # 设置页（分辨率/缓存）
      ├── FocusView.kt             # 对焦框 + 曝光调节自定义 View
      ├── GridOverlayView.kt       # 构图网格线自定义 View
      ├── OcrOverlayView.kt        # 图片触摸交互层
      └── FlowLayout.kt            # 分词流式布局
PaddleOCR4Android/                   # OCR SDK 模块（C++ + JNI）
```

## Changelog

<details>
<summary>v2.7.2.2 — 修复分辨率同步 + 复制换行</summary>

- 修复相机分辨率切换不同步到设置页（未持久化 SharedPreferences）
- 修复 OCR 复制文本无换行（改用 box Y 坐标分组重建行文本）
</details>

<details>
<summary>v2.7.2.0 — 对焦锁定 + 闪光灯 + 网格线</summary>

- 点击对焦 + 长按锁定对焦曝光 + 曝光补偿滑块
- 闪光灯模式切换（自动/开启/关闭）
- 构图三等分网格线
- 双指缩放 + 双击切换 1x/2x
- 文字场景自动曝光优化
- 新增 FocusView / GridOverlayView 自定义视图
</details>

<details>
<summary>v2.7.1.6 — CameraX 内置相机</summary>

- 集成 CameraX 替换系统相机，支持预览/拍照/分辨率选择
- 双指捏合变焦（1x~最大倍率）
- 区域裁剪功能
- 设置页：拍照分辨率 / 相册图片尺寸 / 清除缓存
</details>

<details>
<summary>v2.7.0 — 分词交互升级</summary>

- 分词拖拽连续选词 + 自滚动
- 流式词块布局
- 修复长文本分词乱序
</details>

<details>
<summary>v2.6.x — 交互重塑</summary>

- 框选拖拽选区 + 流式分词布局
- 修复复制文本换行问题
</details>

<details>
<summary>v2.3.0 — 引擎升级</summary>

- OCR 引擎升级至 PP-OCRv5
- APK 体积从 102MB 降至 61MB
</details>

## Credits

- [PaddlePaddle/PaddleOCR](https://github.com/PaddlePaddle/PaddleOCR)
- [PaddlePaddle/Paddle-Lite](https://github.com/PaddlePaddle/Paddle-Lite)
- [equationl/paddleocr4android](https://github.com/equationl/paddleocr4android)
- [houbb/segment](https://github.com/houbb/segment)
- [Material Components for Android](https://github.com/material-components/material-components-android)

## License

[MIT License](./LICENSE)
