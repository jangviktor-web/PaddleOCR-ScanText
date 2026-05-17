# PaddleOCR4Android
### Offline OCR App for Android — Powered by PaddleOCR

A user-friendly Android application wrapping the powerful PaddleOCR engine. **100% offline** — supports real-time text recognition in **Chinese, English, Japanese, Korean, and Traditional Chinese**.

<p align="center">
  <img width="800" alt="PaddleOCR4Android Screenshot" src="https://github.com/user-attachments/assets/290d150a-6705-476f-8175-9c46002a58ce">
</p>

> **[中文文档](./README.md)**

## ✨ Highlights

- **Fully Offline**: All recognition runs locally — no internet needed, privacy-friendly, works anywhere.
- **PP-OCRv5 Engine**: Latest-gen OCR model with high accuracy for Chinese, English, Japanese, and Traditional Chinese in a single model.
- **Smart Interaction**: Three modes — **Box Selection, Line-by-Line, and Word Segmentation** — covering everything from paragraph extraction to precise word picking.
- **Ready to Use**: APK bundles all language models (~61MB). Install and go.
- **Developer Friendly**: Clean modular architecture and source code for easy integration into your own projects.

## 📥 Download

Download the latest APK and install it on your Android device.

| Version | Download | Highlights |
| :--- | :--- | :--- |
| **v2.7.0 (Latest)** | [PaddleOCR-v2.7.0.apk](https://github.com/jangviktor-web/PaddleOCR4Android/releases/download/v2.7.0/PaddleOCR-v2.7.0.apk) | Word segmentation upgrade: drag-to-select & auto-scroll |
| v2.6.3 | [PaddleOCR-v2.6.3.apk](https://github.com/jangviktor-web/PaddleOCR4Android/releases/download/v2.6.3/PaddleOCR-v2.6.3.apk) | Optimized long-text word order & box selection copy order |
| v2.6.1 | [PaddleOCR-v2.6.1.apk](https://github.com/jangviktor-web/PaddleOCR4Android/releases/download/v2.6.1/PaddleOCR-v2.6.1.apk) | Box selection drag & flow layout for word segmentation |
| v2.3.0 | [PaddleOCR-v2.3.0.apk](https://github.com/jangviktor-web/PaddleOCR4Android/releases/download/v2.3.0/PaddleOCR-v2.3.0-original.apk) | Core engine upgraded to PP-OCRv5 |

> **Minimum requirement**: Android 7.0 (API 24)

## 🌍 Supported Languages

| Language Group | Model | Notes |
| :--- | :--- | :--- |
| **Chinese / English / Japanese / Traditional** | PP-OCRv5 | Default — single model, high accuracy |
| **Korean (한국어)** | PP-OCRv3 | ~60% accuracy, PP-OCRv5 integration pending |

## 🛠️ Features

### Core Recognition
1. **Offline Recognition**: No network dependency — works anytime, anywhere.
2. **Multi-language Switching**: One-tap language switching in settings, auto-loads the corresponding model.
3. **Smart Chinese Word Segmentation**: Integrated Jieba segmentation engine with semantic-aware tokenization that preserves reading order.

### Three Interaction Modes

| Mode | How to Use | Best For |
| :--- | :--- | :--- |
| **🖼️ Box Selection** | **Drag** a rectangle on the image to select all text within the area. | Quickly grab irregularly laid-out text blocks. |
| **📝 Line-by-Line** | **Tap** any text line to select it. Multi-tap for cumulative selection. | Fast copy of multi-line paragraphs. |
| **🔗 Word Segmentation** | Results shown as flow-style word chips. **Tap** to select, or **horizontal drag** for continuous selection. | Precise word/phrase copy, similar to mobile WPS. |

> Selected text is highlighted in blue on the image in real-time, with results shown in the bottom panel.

### Utilities
- **Copy & Share**: One-tap copy to clipboard or share to other apps.
- **History**: Auto-saves recognition history. View, delete, or clear anytime.
- **Instant Reset**: Auto-clears content on back press, ready for next scan.

## ⚙️ Tech Stack

| Component | Technology | Purpose |
| :--- | :--- | :--- |
| **OCR Engine** | PaddleOCR + Paddle Lite | Core recognition & mobile inference |
| **Android Wrapper** | paddleocr4android | Android API for PaddleOCR |
| **Chinese Segmentation** | houbb/segment (Jieba lexicon) | Smart Chinese word segmentation |
| **Data Persistence** | Room | Recognition history storage |
| **UI** | Material Design 3 + AndroidX | Modern, beautiful UI components |

## 📂 Build from Source

```bash
# 1. Clone the repository
git clone https://github.com/jangviktor-web/PaddleOCR4Android.git
cd PaddleOCR4Android

# 2. Open in Android Studio or build with Gradle
./gradlew assembleRelease

# 3. Output APK:
# app/build/outputs/apk/release/app-release.apk
```

## 🏗️ Project Structure

```
├── app/                          # Main app module
│   └── src/main/
│       ├── java/.../             # Kotlin source
│       │   ├── MainActivity.kt     # Main UI & core logic
│       │   ├── MoreActivity.kt     # History & about page
│       │   ├── OcrOverlayView.kt   # Image touch interaction layer
│       │   └── FlowLayout.kt       # Word segmentation flow layout
│       ├── assets/models/          # Pre-bundled OCR models (.nb)
│       └── res/                    # Layouts, icons, themes
├── PaddleOCR4Android/              # PaddleOCR SDK module
└── build.gradle                    # Global build config
```

## ❤️ Credits

Built on these excellent open-source projects:
- [PaddlePaddle/PaddleOCR](https://github.com/PaddlePaddle/PaddleOCR)
- [PaddlePaddle/Paddle-Lite](https://github.com/PaddlePaddle/Paddle-Lite)
- [equationl/paddleocr4android](https://github.com/equationl/paddleocr4android)
- [houbb/segment](https://github.com/houbb/segment)
- [Material Components for Android](https://github.com/material-components/material-components-android)

## 📜 Changelog

<details>
<summary><strong>v2.7.0 (2026-05-17) — Word Segmentation Revolution</strong></summary>

- **Drag-to-select words**: Horizontal drag in the word segmentation panel to continuously select word chips.
- **FlowLayout auto-scroll**: Panel auto-scrolls when dragging beyond boundaries.
- Fixed: Long-text word segmentation losing selection state on scroll.
- Fixed: Gesture conflict between drag selection and page scrolling.
</details>

<details>
<summary><strong>v2.6.3 (2026-05-17) — Order Fix</strong></summary>

- Fixed: Word order scrambled in segmentation mode for long texts (e.g., classical Chinese essays). Now uses per-line independent segmentation.
- Fixed: Box selection copy order now follows top-to-bottom, left-to-right reading order.
</details>

<details>
<summary><strong>v2.6.1 (2026-05-17) — Interaction Redesign</strong></summary>

- **Box selection drag**: Drag to draw a rectangle, batch-select all text in the area.
- **Flow segmentation**: Word layout upgraded to WPS-style flow chips — more intuitive and beautiful.
- Fixed: Copied text appearing with one character per line.
</details>

<details>
<summary><strong>v2.3.0 (2026-05-16) — Engine Upgrade</strong></summary>

- Upgraded OCR engine from PP-OCRv4 to **PP-OCRv5** — smaller model, higher accuracy, wider language coverage.
- APK size reduced from 102MB to 61MB.
</details>

## 📄 License

This project is licensed under the [MIT License](./LICENSE).
