# PaddleOCR4Android
 **[English Version](./README_EN.md)**
### 基于PaddleOCR的Android离线OCR文字识别应用

一款将强大PaddleOCR引擎封装为易于使用的Android应用，**无需联网**，支持**中、英、日、韩及繁体中文**等多语言实时识别。

<p align="center">

</p>
<img width="3000" height="2160" alt="微信图片_20260525100310_33_134" src="https://github.com/user-attachments/assets/29d823dd-fbd9-4e82-a041-647a943943db" />

## ✨ 核心亮点

- **完全离线**：所有识别在本地完成，无需网络，保护隐私且不受环境限制。
- **PP-OCRv5 引擎**：搭载最新一代OCR模型，单模型精准识别中、英、日、繁体中文。
- **智能交互**：提供 **框选、逐行、分词、裁剪** 四种模式，满足从段落摘录到精准选词的多样化需求。
- **内置相机**：集成CameraX相机，支持双指缩放变焦、多分辨率拍照、前后摄切换。
- **开箱即用**：APK内置全部语言模型（约81MB），安装即享识别服务。
- **无缝集成**：为Android开发者提供清晰的架构和源码，便于集成到自己的项目中。

## 📥 下载安装

直接下载最新版APK，安装到您的Android设备即可使用。

| 版本 | 下载链接 | 更新重点 |
| :--- | :--- | :--- |
| **v2.7.2.0 (最新)** | [PaddleOCR-v2.7.2.0.apk](https://github.com/jangviktor-web/PaddleOCR4Android/releases/download/v2.7.2.0/PaddleOCR-v2.7.2.0.apk) | 🎯 **对焦锁定 + 闪光灯 + 网格线 + 曝光优化** |
| v2.7.1.6 | [PaddleOCR-v2.7.1.6.apk](https://github.com/jangviktor-web/PaddleOCR4Android/releases/download/v2.7.1.6/PaddleOCR-v2.7.1.6.apk) | 📷 CameraX内置相机 + 双指缩放 + 区域裁剪 |
| v2.7.0 | [PaddleOCR-v2.7.0.apk](https://github.com/jangviktor-web/PaddleOCR4Android/releases/download/v2.7.0/PaddleOCR-v2.7.0.apk) | 🚀 **分词模式升级**：支持拖拽连续选词与自滚动，选词更流畅 |
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
- **区域裁剪**：识别结果界面底部新增「裁剪」按钮，可对图片进行自由区域裁剪，裁剪后自动重新识别。
- **历史记录**：自动保存识别历史，可随时查看、删除或清空。
- **即时反馈**：返回时自动清空当前内容，为下次识别做好准备。

### 设置功能
- **拍照分辨率**：可选640×480 ~ 最高分辨率，CameraX按4:3比例输出，不裁剪不变形。
- **相册图片最大尺寸**：控制相册上传图片的缩放上限（1024/2048/3072/4096 px）。
- **清除缓存**：一键清除拍照缓存和历史图片，释放存储空间。

## ⚙️ 技术架构

本项目采用模块化设计，技术栈清晰：

| 组件 | 技术选型 | 作用 |
| :--- | :--- | :--- |
| **OCR 引擎** | PaddleOCR + Paddle Lite | 核心识别与移动端推理。 |
| **Android 封装** | paddleocr4android | 提供便捷的Android API调用接口。 |
| **相机** | CameraX 1.3.1 | 内置相机预览、拍照、变焦、分辨率控制。 |
| **中文分词** | houbb/segment (结巴词库) | 实现智能中文分词功能。 |
| **数据持久化** | Room + SharedPreferences | 存储识别历史记录和用户设置。 |
| **用户界面** | Material Design 3 + AndroidX | 提供现代、美观的UI组件。 |
| **构建工具** | NDK r21e + Gradle 8.9 | 原生C++编译与项目构建。 |

## 📂 从源码构建

欢迎开发者克隆、研究和贡献代码！

### 环境要求

| 工具 | 版本要求 | 说明 |
| :--- | :--- | :--- |
| **JDK** | 17+ (推荐 21) | Android Gradle Plugin 8.x 需要 JDK 17+ |
| **Android SDK** | API 35 | `compileSdk 35`，通过 Android Studio 或 `sdkmanager` 安装 |
| **Build Tools** | 35.0.1 | `buildToolsVersion "35.0.1"` |
| **NDK** | **r21e** (21.4.7075529) | ⚠️ 必须使用 r21e，NDK 25+ 的链接器会拒绝 PaddleLite 预编译库 |
| **CMake** | 3.22.1 | 编译 PaddleOCR 原生 C++ 代码 |
| **Gradle** | 8.9 | 项目自带 `gradlew`，无需手动安装 |
| **Kotlin** | 1.9.24 | 项目自带，无需手动安装 |

### 安装步骤

#### 1. 安装 JDK 17+

**Windows：**
```bash
# 方式一：使用 Android Studio 自带的 JBR（推荐）
# Android Studio 安装目录/jbr/bin/java -version

# 方式二：手动安装 OpenJDK
# 下载 https://adoptium.net/temurin/releases/?version=17
# 安装后设置 JAVA_HOME 环境变量
```

**macOS：**
```bash
brew install openjdk@17
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
```

**Linux：**
```bash
sudo apt install openjdk-17-jdk
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
```

#### 2. 安装 Android SDK

**方式一：使用 Android Studio（推荐）**
1. 下载安装 [Android Studio](https://developer.android.com/studio)
2. 打开 Android Studio → Settings → SDK Manager
3. 安装以下组件：
   - SDK Platforms → Android 15 (API 35)
   - SDK Tools → Android SDK Build-Tools 35.0.1
   - SDK Tools → NDK (Side by side) → 21.4.7075529
   - SDK Tools → CMake → 3.22.1

**方式二：使用命令行 sdkmanager**
```bash
# 下载 Android Command Line Tools
# https://developer.android.com/studio#command-line-tools-only

# 设置环境变量
export ANDROID_HOME=$HOME/Android/Sdk

# 安装必要组件
sdkmanager "platforms;android-35" "build-tools;35.0.1" "ndk;21.4.7075529" "cmake;3.22.1"
```

#### 3. 配置 NDK 路径

在项目根目录的 `local.properties` 中设置：
```properties
sdk.dir=/path/to/your/Android/Sdk
ndk.dir=/path/to/your/Android/Sdk/ndk/21.4.7075529
```

⚠️ **NDK 版本注意事项：**
- 本项目使用 PaddleLite 预编译的 `.so` 库，其中包含 NDK r21 编译的本地符号
- NDK 25+ 的 `lld` 链接器会拒绝这些符号（报错 `found local symbol in global part of symbol table`）
- **必须使用 NDK r21e**，否则无法编译

### 构建命令

```bash
# 1. 克隆仓库
git clone https://github.com/jangviktor-web/PaddleOCR4Android.git
cd PaddleOCR4Android

# 2. 构建 Release APK（命令行方式）
./gradlew :app:assembleRelease

# 3. 生成的 APK 位于：
# app/build/outputs/apk/release/app-release.apk

# 4. 构建 Debug APK（用于调试）
./gradlew :app:assembleDebug
```

### 在 Android Studio 中构建

1. 打开 Android Studio → File → Open → 选择项目目录
2. 等待 Gradle 同步完成（首次需要下载依赖）
3. 菜单 Build → Build Bundle(s) / APK(s) → Build APK(s)
4. APK 输出路径：`app/build/outputs/apk/debug/app-release.apk`

### 常见问题

**Q: 构建报错 `Failed to find Build Tools revision 34.0.0`**
A: 检查 `PaddleOCR4Android/build.gradle` 中的 `buildToolsVersion`，确保与已安装的版本匹配。

**Q: 构建报错 `found local symbol in global part of symbol table`**
A: NDK 版本不对，必须使用 NDK r21e。在 `PaddleOCR4Android/build.gradle` 中设置 `ndkVersion '21.4.7075529'`。

**Q: Gradle 依赖下载超时**
A: 配置代理，在 `~/.gradle/gradle.properties` 中添加：
```properties
systemProp.http.proxyHost=127.0.0.1
systemProp.http.proxyPort=7897
systemProp.https.proxyHost=127.0.0.1
systemProp.https.proxyPort=7897
```

**Q: APK 体积多大？**
A: Release APK 约 81MB（包含 CameraX 相机、OCR 模型、OpenCV 等）。

## 🏗️ 项目结构

```
├── app/                          # 主应用模块
│   └── src/main/
│       ├── java/.../             # Kotlin源码
│       │   ├── MainActivity.kt     # 主界面与核心逻辑
│       │   ├── CameraActivity.kt   # CameraX内置相机（缩放、拍照）
│       │   ├── SettingsActivity.kt # 设置页（照片大小、缓存清理）
│       │   ├── MoreActivity.kt     # 历史与关于页面
│       │   ├── HistoryActivity.kt  # 识别历史列表
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
<summary><strong>v2.7.2.0 (2026-06-16) - 对焦锁定 + 闪光灯 + 网格线 + 曝光优化</strong></summary>

**新增功能：**
- **点击对焦**：点击相机预览区任意位置，弹出圆形白色对焦框，自动对焦该区域。对焦框带呼吸动画，3秒后自动消失。
- **长按锁定对焦和曝光**：长按500ms切换锁定状态，对焦框变为金色方形，对焦+曝光锁定不自动取消（5分钟）。再次长按解锁。
- **曝光补偿调节条**：锁定对焦后，对焦框右侧显示垂直滑块（-3 ~ +3 EV），拖动可实时调节曝光补偿，解决逆光/过曝场景。
- **闪光灯模式切换**：顶部新增⚡按钮，点击循环切换三种模式：自动（⚡AUTO）、强制开启（⚡ON）、关闭（⚡OFF），Toast提示当前状态。
- **构图辅助网格线**：顶部新增📊按钮，切换三等分网格线（Rule of Thirds），白色半透明线条，帮助构图。
- **文字场景自动曝光优化**：相机启动时自动检测曝光状态，若接近过曝（曝光值接近上限），自动降低曝光补偿，优化文字识别场景。
- **自定义FocusView**：新建`FocusView`自定义视图，集成对焦框绘制、曝光滑块、手势处理（拖动/长按/缩放）。
- **自定义GridOverlayView**：新建构图网格线自定义视图。

**问题修复：**
- 修复了双指缩放闪退问题：`autoCancelDuration must be at least 1`，锁定对焦时`setAutoCancelDuration`最小值为1秒。
- 修复了AE扫描不支持的崩溃：`MeteringPoints is not supported on this camera`，改为直接读取曝光状态检测过曝。
- 修复了`FocusView`中`scaleGestureDetector?.isInProgress!!`空指针异常。
- 修复了双指缩放手势被`FocusView`拦截的问题：将缩放手势检测移入`FocusView`内部，通过`onZoomChanged`回调连接到相机缩放控制。

**技术改进：**
- `FocusView`同时处理对焦（单指）和缩放（双指）手势，互不干扰。
- 新增图标资源：`ic_grid.xml`、`ic_flash_auto.xml`、`ic_flash_on.xml`、`ic_flash_off.xml`。
- 备份核心文件：`CameraActivity.kt.bak`、`activity_camera.xml.bak`。

</details>

<details>
<summary><strong>v2.7.1.6 (2026-06-16) - CameraX内置相机 + 双指缩放 + 区域裁剪</strong></summary>

**新增功能：**
- **CameraX内置相机**：替换系统相机Intent，集成CameraX相机预览，支持实时取景、前后摄切换、闪光灯控制。
- **双指捏合变焦**：在相机预览界面双指缩放调节焦距，支持1x~最大倍率连续变焦，顶部实时显示当前倍率（如"2.0x"）+ 水平进度条。双击可在1x和2x之间快速切换。
- **拍照分辨率设置**：设置页新增「拍照分辨率」选项，可选640×480 ~ 最高分辨率，CameraX按4:3比例输出，不裁剪不变形。
- **区域裁剪功能**：识别结果界面底部操作栏新增独立「裁剪」按钮（独立于框选/逐行/分词），调用系统裁剪工具，支持自由拖拽裁剪框，裁剪后自动替换图片并重新识别。

**问题修复：**
- 修复了使用系统相机拍照后，图片方向不正确的问题（左横屏/右横屏拍摄时图片旋转错误）。
- 修复了识别结果界面复制文字时，不同行的文本没有换行符的问题。框选多行文字后复制，现在会按原始行结构自动插入换行。

**技术改进：**
- 移除了不可靠的传感器旋转计算（`calculateRotationFromSensors`），避免叠加多余旋转导致图片方向混乱。
- OCR处理的bitmap不再进行旋转，确保识别内容准确不受影响。
- 新增CameraX依赖：`camera-core`、`camera-camera2`、`camera-lifecycle`、`camera-view`（1.3.1）。
- 新增`CameraActivity`：完整的CameraX相机实现，含预览、拍照、分辨率选择、变焦控制。
- 新增`ic_crop.xml`、`ic_camera.xml`等矢量图标。

</details>

<details>
<summary><strong>v2.7.1.5 (2026-06-16) - 旋转回退修复</strong></summary>

**问题修复：**
- 回退了之前版本中不稳定的图片旋转逻辑，确保OCR识别内容不被旋转操作干扰。
- 拍照后的图片保持原始方向显示，OCR识别准确率恢复正常。

</details>

<details>
<summary><strong>v2.7.1.1 (2026-06-16) - 自动方向校正（传感器）</strong></summary>

**新增功能：**
- 集成设备姿态传感器（`TYPE_ROTATION_VECTOR`），通过加速度计和陀螺仪融合数据检测设备实际姿态。
- 根据传感器数据计算拍照后的校正角度，支持竖屏、横屏、倒置等多种握持方式。

**技术改进：**
- CameraActivity实现`SensorEventListener`接口，`onResume`/`onPause`自动注册/注销传感器监听。
- 新增`calculateRotationFromSensors()`方法，根据pitch/roll角度判断设备姿态。
- 综合传感器姿态+相机传感器角度+显示旋转计算最终校正角度。

</details>

<details>
<summary><strong>v2.7.0.9 (2026-06-16) - EXIF旋转修复</strong></summary>

**问题修复：**
- 使用`windowManager.defaultDisplay.rotation`替代`previewView.display.rotation`获取更可靠的显示旋转角度。
- 拍照前更新`imageCapture.targetRotation`确保旋转信息写入EXIF。
- `loadBitmapFromPath`读取EXIF旋转标签并应用到bitmap。

</details>

<details>
<summary><strong>v2.7.0.8 (2026-06-16) - 拍照分辨率设置</strong></summary>

**新增功能：**
- 设置页新增「拍照分辨率」选项，可选：640×480、1280×720、1920×1080、2560×1440（默认）、3840×2160、最高分辨率。
- CameraX `ImageCapture`使用`setTargetResolution`控制拍照输出分辨率。
- 新增`ic_language.xml`矢量图标（地球图标，用于语言切换设置）。

**问题修复：**
- 修复了拍照后图片被裁剪变小的问题（CameraX `setTargetResolution`会裁剪图片宽高比）。
- 改用`setTargetAspectRatio(RATIO_4_3)`保持原始比例不裁剪。
- 设置页「照片最大尺寸」改为仅作用于相册上传的图片，默认值改为4096（不缩放原图）。

</details>

<details>
<summary><strong>v2.7.0.7 (2026-06-16) - 框选复制换行修复</strong></summary>

**问题修复：**
- 修复了框选模式下复制文字时，不同行的文本被拼接在一起没有换行的问题。
- 新增`OcrOverlayView.buildSelectedText()`方法，根据box所在行自动在不同行之间插入`\n`换行符。
- 全选/逐行模式的复制功能不受影响。

</details>

<details>
<summary><strong>v2.7.0.6 (2026-06-16) - CameraX集成与拍照功能</strong></summary>

**重大更新：**
- **集成CameraX**：添加CameraX相机依赖（camera-core、camera-camera2、camera-lifecycle、camera-view 1.3.1），替换系统相机Intent。
- **新建CameraActivity**：完整的CameraX相机页面，支持实时预览、拍照、分辨率选择、前后摄切换。
- **设置页新增两项设置**：
  - 「照片最大尺寸」：控制相册上传图片的缩放上限（1024/2048/3072/4096 px）。
  - 「清除缓存」：显示缓存大小，一键清除拍照缓存和历史图片。
- **新建SettingsActivity**：独立设置页面，含SharedPreferences持久化存储。
- **MoreActivity添加设置入口**：更多页面新增「设置」选项，跳转到SettingsActivity。

**技术改进：**
- 使用NDK r21e（LLD 9.0.9）构建，解决NDK 25+链接器拒绝PaddleLite预编译.so符号表的问题。
- CameraActivity使用`windowManager.defaultDisplay.rotation`获取显示旋转角度。

</details>

<details>
<summary><strong>v2.7.0.5 (2026-06-16) - 图片旋转修复</strong></summary>

**问题修复：**
- 修复了拍照后识别结果界面图片方向不正确的问题。
- 使用`android.media.ExifInterface`读取JPEG文件的EXIF旋转标签（90°/180°/270°），自动修正图片方向。
- 缩放后正确回收旧Bitmap，减少内存占用。

</details>

<details>
<summary><strong>v2.7.0.4 (2026-06-16) - CameraX旋转角度修复</strong></summary>

**问题修复：**
- 使用`windowManager.defaultDisplay.rotation`获取更可靠的显示旋转角度，替代`previewView.display.rotation`。
- 拍照前更新`imageCapture.targetRotation`确保旋转信息正确写入。

</details>

<details>
<summary><strong>v2.7.0.3 (2026-06-16) - 图片方向修复</strong></summary>

**问题修复：**
- 使用`android.media.ExifInterface`读取拍照文件的EXIF旋转标签。
- 根据EXIF方向信息（ORIENTATION_ROTATE_90/180/270）自动旋转图片到正确方向。
- 缩放后回收旧Bitmap，避免内存泄漏。

</details>

<details>
<summary><strong>v2.7.0.2 (2026-06-16) - 拍照功能修复</strong></summary>

**问题修复：**
- 修复了切换拍照分辨率后拍照失败的问题（`ImageCapture`未重新绑定到相机）。
- 新增`rebindCamera()`方法，切换分辨率时先`unbindAll()`再重新绑定。

</details>

<details>
<summary><strong>v2.7.0.1 (2026-06-16) - 设置页与缓存功能</strong></summary>

**新增功能：**
- **新建设置页**（SettingsActivity）：
  - 「照片最大尺寸」：可选1024/2048/3072/4096 px，默认2048，控制拍照后图片缩放上限。
  - 「清除缓存」：显示当前缓存大小（拍照缓存+历史图片），一键清除。
- **MoreActivity添加设置入口**：更多页面新增「设置」选项。
- **SharedPreferences持久化**：照片大小设置通过SharedPreferences存储，重启应用后保留。
- **新建图标**：`ic_settings.xml`（齿轮）、`ic_delete.xml`（垃圾桶）矢量图标。
- **AndroidManifest注册**：添加SettingsActivity声明。

**技术改进：**
- `loadBitmapFromUri`读取SharedPreferences中的照片大小设置，替代硬编码的2048。
- 清除缓存时删除`cacheDir`（拍照缓存）和`filesDir/history_images/`（历史图片）。

</details>

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
