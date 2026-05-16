package com.equationl.paddleocr4android.app

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.equationl.paddleocr4android.CpuPowerMode
import com.equationl.paddleocr4android.OCR
import com.equationl.paddleocr4android.OcrConfig
import com.equationl.paddleocr4android.bean.OcrResult
import com.equationl.paddleocr4android.callback.OcrInitCallback
import com.equationl.paddleocr4android.callback.OcrRunCallback
import com.github.houbb.segment.bs.SegmentBs
import com.github.houbb.segment.support.segment.result.impl.SegmentResultHandlers
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

data class WordInfo(
    val text: String,
    val boxIndices: List<Int>
)

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "OCRDemo"
        private const val PREFS_NAME = "ocr_models"
        private const val KEY_DOWNLOADED = "downloaded_models"
    }

    // ======================== 语言配置 ========================

    private val bundledLanguages = setOf("ch_PP-OCRv4")

    /**
     * 模型来源：
     * - 中文：assets 预装 .nb 格式
     * - 英文：可下载 slim .nb 格式（3.3MB），det/cls 复用中文
     * - 其他语言：仅提供 inference 格式，PaddleLite 无法加载，暂不支持
     */
    private val languageConfig = linkedMapOf(
        "中文 + English" to LangModel(
            dir = "ch_PP-OCRv4",
            recUrl = null,
            label = "已预装"
        ),
        "English" to LangModel(
            dir = "en_PP-OCRv3_slim",
            recUrl = "https://paddleocr.bj.bcebos.com/PP-OCRv3/english/en_PP-OCRv3_rec_slim_infer.nb",
            label = "可下载 · 3.3MB"
        ),
    )

    data class LangModel(
        val dir: String,
        val recUrl: String?,
        val label: String
    )

    // UI
    private lateinit var ivPreview: ImageView
    private lateinit var ocrOverlay: OcrOverlayView
    private lateinit var emptyState: LinearLayout
    private lateinit var loadingOverlay: LinearLayout
    private lateinit var tvLoading: TextView
    private lateinit var modeSwitchBar: LinearLayout
    private lateinit var btnModeBox: TextView
    private lateinit var btnModeLine: TextView
    private lateinit var btnModeWord: TextView
    private lateinit var bottomPanel: LinearLayout
    private lateinit var panelHome: LinearLayout
    private lateinit var panelResult: LinearLayout
    private lateinit var tvStatus: TextView
    private lateinit var tvModelLabel: TextView
    private lateinit var tvResult: TextView
    private lateinit var tvTime: TextView
    private lateinit var wordContainer: FrameLayout
    private lateinit var resultScroll: ScrollView
    private lateinit var rvWords: RecyclerView
    private lateinit var btnSelectAll: TextView
    private lateinit var btnBack: ImageView
    private lateinit var spinnerLang: Spinner
    private lateinit var tvWordsBtnLabel: TextView
    private lateinit var btnDownloadModel: TextView

    // 逻辑
    private lateinit var ocr: OCR
    private lateinit var wordAdapter: WordAdapter
    private var currentBitmap: Bitmap? = null
    private var isModelLoaded = false
    private var currentMode = 0
    private var allResultBoxes: List<FloatArray> = emptyList()
    private var allResultWords: List<String> = emptyList()
    private var allResultLines: List<String> = emptyList()
    private var segmentedWords: List<WordInfo> = emptyList()
    private var currentPhotoUri: Uri? = null
    private var resultText: String = ""
    private var selectedText: String = ""

    // 下载
    private val downloadExecutor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())
    private var isDownloading = false
    private val downloadedModels = mutableSetOf<String>()

    // 分词器
    private val segmentBs: SegmentBs by lazy { SegmentBs.newInstance() }

    // ---- Activity Result ----
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && currentPhotoUri != null) {
            loadBitmapFromUri(currentPhotoUri!!)?.let { showImage(it) }
        }
    }
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { loadBitmapFromUri(it)?.let { showImage(it) } }
    }
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchCamera() else tvStatus.text = "需要相机权限"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ocr = OCR(this)
        wordAdapter = WordAdapter { onWordAdapterSelectionChanged() }
        loadDownloadedModels()
        initViews()
        setupListeners()
        updateDownloadButton()
    }

    // ======================== 模型下载持久化 ========================

    private fun loadDownloadedModels() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val saved = prefs.getStringSet(KEY_DOWNLOADED, emptySet()) ?: emptySet()
        downloadedModels.addAll(saved)
        downloadedModels.retainAll { isModelAvailable(it) }
    }

    private fun saveDownloadedModels() {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_DOWNLOADED, downloadedModels.toSet())
            .apply()
    }

    private fun isModelAvailable(modelDir: String): Boolean {
        // 1. 检查 assets
        try {
            val files = assets.list("models/$modelDir")
            if (files != null && files.any { it.endsWith(".nb") }) return true
        } catch (_: Exception) {}

        // 2. 检查内部存储
        val dir = File(filesDir, "models/$modelDir")
        return dir.exists() && dir.listFiles()?.any { it.name.endsWith(".nb") } == true
    }

    /**
     * 获取模型目录的实际路径
     * 以 "/" 开头的路径会被 PaddleLite 直接从文件系统加载
     */
    private fun getModelPath(modelDir: String): String {
        // 优先内部存储（下载的模型）— 用绝对路径
        val internalDir = File(filesDir, "models/$modelDir")
        if (internalDir.exists() && internalDir.listFiles()?.any { it.name.endsWith(".nb") } == true) {
            return internalDir.absolutePath
        }
        // assets 中的模型 — 不以 "/" 开头，PaddleLite 会从 assets 加载
        return "models/$modelDir"
    }

    // ======================== 下载按钮逻辑 ========================

    private fun updateDownloadButton() {
        val selectedLang = spinnerLang.selectedItem as? String ?: return
        val langKey = selectedLang.removePrefix("✓ ").removePrefix("↓ ").removePrefix("✗ ")
        val config = languageConfig[langKey] ?: return

        when {
            isModelAvailable(config.dir) -> {
                btnDownloadModel.visibility = View.GONE
            }
            config.recUrl != null -> {
                // 可下载
                btnDownloadModel.visibility = View.VISIBLE
                btnDownloadModel.isEnabled = !isDownloading
                btnDownloadModel.text = if (isDownloading) "下载中..." else "下载"
            }
            else -> {
                // 不支持
                btnDownloadModel.visibility = View.GONE
            }
        }
    }

    private fun startDownload(modelConfig: LangModel) {
        if (isDownloading) return
        val url = modelConfig.recUrl ?: return

        isDownloading = true
        btnDownloadModel.text = "0%"
        btnDownloadModel.isEnabled = false
        tvStatus.text = "正在下载 ${modelConfig.dir} 模型..."

        downloadExecutor.execute {
            try {
                val modelDir = File(filesDir, "models/${modelConfig.dir}")
                modelDir.mkdirs()

                // 下载 .nb 文件（直接下载，无需解压）
                val nbFile = File(modelDir, "rec.nb")
                downloadFile(url, nbFile) { progress ->
                    handler.post {
                        btnDownloadModel.text = "$progress%"
                    }
                }

                // 从 assets 复制共享模型（det.nb + cls.nb）
                copySharedModels(modelDir)

                // 验证
                val hasAll = modelDir.listFiles()?.filter { it.name.endsWith(".nb") }?.size == 3

                handler.post {
                    isDownloading = false
                    if (hasAll) {
                        downloadedModels.add(modelConfig.dir)
                        saveDownloadedModels()
                        tvStatus.text = "✅ ${modelConfig.dir} 下载完成"
                        Toast.makeText(this, "模型下载完成", Toast.LENGTH_SHORT).show()
                    } else {
                        tvStatus.text = "❌ 模型文件不完整"
                        Toast.makeText(this, "下载失败：模型文件不完整", Toast.LENGTH_SHORT).show()
                    }
                    updateDownloadButton()
                }
            } catch (e: Exception) {
                Log.e(TAG, "下载失败", e)
                handler.post {
                    isDownloading = false
                    tvStatus.text = "❌ 下载失败: ${e.message}"
                    Toast.makeText(this, "下载失败: ${e.message}", Toast.LENGTH_SHORT).show()
                    updateDownloadButton()
                }
            }
        }
    }

    private fun copySharedModels(targetDir: File) {
        val sharedModels = listOf("det.nb", "cls.nb")
        for (name in sharedModels) {
            val target = File(targetDir, name)
            if (target.exists()) continue
            try {
                assets.open("models/ch_PP-OCRv4/$name").use { input ->
                    target.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "复制共享模型 $name 失败", e)
            }
        }
    }

    @Throws(IOException::class)
    private fun downloadFile(urlStr: String, destFile: File, onProgress: (Int) -> Unit) {
        val url = URL(urlStr)
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 15000
        conn.readTimeout = 60000
        conn.connect()

        val total = conn.contentLength
        var downloaded = 0
        val buffer = ByteArray(8192)

        conn.inputStream.use { input ->
            destFile.outputStream().use { output ->
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                    downloaded += read
                    if (total > 0) {
                        val pct = (downloaded * 100 / total)
                        onProgress(pct)
                    }
                }
            }
        }
    }

    // ======================== UI 初始化 ========================

    private fun initViews() {
        ivPreview = findViewById(R.id.iv_preview)
        ocrOverlay = findViewById(R.id.ocr_overlay)
        emptyState = findViewById(R.id.empty_state)
        loadingOverlay = findViewById(R.id.loading_overlay)
        tvLoading = findViewById(R.id.tv_loading)
        modeSwitchBar = findViewById(R.id.mode_switch_bar)
        btnModeBox = findViewById(R.id.btn_mode_box)
        btnModeLine = findViewById(R.id.btn_mode_line)
        btnModeWord = findViewById(R.id.btn_mode_word)
        bottomPanel = findViewById(R.id.bottom_panel)
        panelHome = findViewById(R.id.panel_home)
        panelResult = findViewById(R.id.panel_result)
        tvStatus = findViewById(R.id.tv_status)
        tvModelLabel = findViewById(R.id.tv_model_label)
        tvResult = findViewById(R.id.tv_result)
        tvTime = findViewById(R.id.tv_time)
        wordContainer = findViewById(R.id.word_container)
        resultScroll = findViewById(R.id.result_scroll)
        rvWords = findViewById(R.id.rv_words)
        btnSelectAll = findViewById(R.id.btn_select_all)
        btnBack = findViewById(R.id.btn_back)
        spinnerLang = findViewById(R.id.spinner_lang)
        tvWordsBtnLabel = findViewById(R.id.tv_words_btn_label)
        btnDownloadModel = findViewById(R.id.btn_download_model)

        rvWords.layoutManager = GridLayoutManager(this, 3)
        rvWords.adapter = wordAdapter

        // 语言 Spinner 适配器
        val langDisplayNames = languageConfig.keys.map { name ->
            val cfg = languageConfig[name]!!
            when {
                isModelAvailable(cfg.dir) -> "✓ $name"
                cfg.recUrl != null -> "↓ $name"
                else -> "✗ $name"
            }
        }
        spinnerLang.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, langDisplayNames)

        spinnerLang.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateDownloadButton()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        ocrOverlay.onLineSelected = { lineIndex, lineText ->
            selectedText = if (lineIndex >= 0) lineText else ""
            updateTvResultHighlight()
        }

        ocrOverlay.onBoxSelected = { _, _ ->
            val indices = ocrOverlay.getSelectedIndices()
            selectedText = if (indices.isNotEmpty()) {
                indices.sorted().joinToString("") { idx ->
                    if (idx < allResultWords.size) allResultWords[idx] else ""
                }
            } else ""
            updateTvResultHighlight()
        }
    }

    private fun setupListeners() {
        findViewById<View>(R.id.btn_load_model).setOnClickListener { loadModel() }

        findViewById<View>(R.id.btn_camera_home).setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                launchCamera()
            } else {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        findViewById<View>(R.id.btn_gallery_home).setOnClickListener { galleryLauncher.launch("image/*") }

        btnBack.setOnClickListener { resetToCapture() }

        btnModeBox.setOnClickListener { switchMode(0) }
        btnModeLine.setOnClickListener { switchMode(1) }
        btnModeWord.setOnClickListener { switchMode(2) }

        findViewById<View>(R.id.btn_action_copy).setOnClickListener { copyResult() }
        findViewById<View>(R.id.btn_action_share).setOnClickListener { shareResult() }
        findViewById<View>(R.id.btn_action_words).setOnClickListener { switchMode(2) }

        val selectAllListener = View.OnClickListener {
            when (currentMode) {
                0 -> selectAllBoxes()
                1 -> selectAllLines()
                2 -> { if (wordAdapter.itemCount > 0) wordAdapter.selectAll() }
            }
        }
        findViewById<View>(R.id.btn_action_select_all).setOnClickListener(selectAllListener)
        btnSelectAll.setOnClickListener(selectAllListener)

        btnDownloadModel.setOnClickListener {
            val selectedLang = spinnerLang.selectedItem as? String ?: return@setOnClickListener
            val langKey = selectedLang.removePrefix("✓ ").removePrefix("↓ ").removePrefix("✗ ")
            val config = languageConfig[langKey] ?: return@setOnClickListener
            startDownload(config)
        }
    }

    // ======================== 文本高亮 ========================

    private fun updateTvResultHighlight() {
        if (selectedText.isEmpty()) {
            tvResult.text = allResultLines.joinToString("\n")
            return
        }
        val fullText = allResultLines.joinToString("\n")
        val spannable = android.text.SpannableString(fullText)
        var searchFrom = 0
        while (searchFrom < fullText.length) {
            val start = fullText.indexOf(selectedText, searchFrom)
            if (start < 0) break
            val end = start + selectedText.length
            spannable.setSpan(
                android.text.style.BackgroundColorSpan(Color.parseColor("#662196F3")),
                start, end,
                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            searchFrom = end
        }
        tvResult.text = spannable
    }

    // ======================== 模型 & 识别 ========================

    private fun loadModel() {
        val selectedLang = spinnerLang.selectedItem as? String ?: return
        val langKey = selectedLang.removePrefix("✓ ").removePrefix("↓ ").removePrefix("✗ ")
        val config = languageConfig[langKey] ?: return
        val modelDir = config.dir

        if (!isModelAvailable(modelDir)) {
            if (config.recUrl != null) {
                tvStatus.text = "请先下载 $langKey 模型"
                Toast.makeText(this, "请先点击下载按钮获取模型", Toast.LENGTH_SHORT).show()
            } else {
                tvStatus.text = "$langKey 暂不支持"
                Toast.makeText(this, "该语言暂无可用的 PaddleLite 模型", Toast.LENGTH_SHORT).show()
            }
            return
        }

        val btnLoad = findViewById<View>(R.id.btn_load_model)
        btnLoad.isEnabled = false
        tvStatus.text = "正在加载 $langKey 模型..."
        tvModelLabel.text = "加载中..."

        val configOcr = OcrConfig().apply {
            modelPath = getModelPath(modelDir)
            clsModelFilename = "cls.nb"
            detModelFilename = "det.nb"
            recModelFilename = "rec.nb"
            isRunDet = true; isRunCls = true; isRunRec = true
            cpuPowerMode = CpuPowerMode.LITE_POWER_FULL
            isDrwwTextPositionBox = true
        }

        if (isModelLoaded) { ocr.releaseModel(); isModelLoaded = false }

        ocr.initModel(configOcr, object : OcrInitCallback {
            override fun onSuccess() = runOnUiThread {
                isModelLoaded = true
                tvStatus.text = "✅ $langKey 就绪"
                tvModelLabel.text = "✅ 已加载"
                btnLoad.isEnabled = true
            }
            override fun onFail(e: Throwable) = runOnUiThread {
                tvStatus.text = "❌ 加载失败: ${e.message}"
                tvModelLabel.text = "模型"
                btnLoad.isEnabled = true
            }
        })
    }

    private fun recognizeImage() {
        val bitmap = currentBitmap ?: return
        if (!isModelLoaded) { tvStatus.text = "请先加载模型"; return }

        loadingOverlay.visibility = View.VISIBLE
        tvLoading.text = "正在识别..."

        val t0 = System.currentTimeMillis()

        ocr.run(bitmap, object : OcrRunCallback {
            override fun onSuccess(result: OcrResult) {
                val elapsed = System.currentTimeMillis() - t0
                val wordLabels = ocr.getWordLabels()
                val lines = result.simpleText.split("\n").filter { it.isNotBlank() }

                val words = mutableListOf<String>()
                val boxes = mutableListOf<FloatArray>()
                val charPositions = mutableListOf<Int>()
                var charOffset = 0

                result.outputRawResult.forEachIndexed { _, item ->
                    val sb = StringBuilder()
                    item.wordIndex.forEach { idx -> sb.append(wordLabels[idx]) }
                    val word = sb.toString()
                    if (word.isNotBlank()) {
                        words.add(word)
                        charPositions.add(charOffset)
                        charOffset += word.length
                        val pts = item.points
                        val fPts = FloatArray(pts.size * 2)
                        for (i in pts.indices) {
                            fPts[i * 2] = pts[i].x.toFloat()
                            fPts[i * 2 + 1] = pts[i].y.toFloat()
                        }
                        boxes.add(fPts)
                    }
                }

                val fullText = words.joinToString("")
                val segResult = try {
                    segmentBs.segment(fullText, SegmentResultHandlers.word()) as List<String>
                } catch (e: Exception) {
                    Log.e(TAG, "分词失败", e)
                    words
                }
                val segWords = mapSegmentToBoxes(segResult, words, charPositions)
                val lineBoxGroups = computeLineBoxGroups(boxes)

                runOnUiThread {
                    loadingOverlay.visibility = View.GONE
                    allResultBoxes = boxes
                    allResultWords = words
                    allResultLines = lines
                    segmentedWords = segWords
                    resultText = lines.joinToString("\n")
                    showResultUI(result, lines, elapsed, lineBoxGroups)
                }
            }

            override fun onFail(e: Throwable) = runOnUiThread {
                loadingOverlay.visibility = View.GONE
                tvStatus.text = "❌ 识别失败: ${e.message}"
                Log.e(TAG, "识别失败", e)
            }
        })
    }

    private fun computeLineBoxGroups(boxes: List<FloatArray>): List<List<Int>> {
        if (boxes.isEmpty()) return emptyList()
        data class BoxYInfo(val index: Int, val yMedian: Float)
        val boxYInfos = boxes.mapIndexed { idx, box ->
            val ys = floatArrayOf(box[1], box[3], box[5], box[7])
            BoxYInfo(idx, ys.sorted().let { (it[1] + it[2]) / 2f })
        }.sortedBy { it.yMedian }

        val groups = mutableListOf<MutableList<Int>>()
        var currentGroup = mutableListOf(boxYInfos[0].index)
        var currentY = boxYInfos[0].yMedian
        val threshold = 15f

        for (i in 1 until boxYInfos.size) {
            val info = boxYInfos[i]
            if (kotlin.math.abs(info.yMedian - currentY) < threshold) {
                currentGroup.add(info.index)
            } else {
                groups.add(currentGroup)
                currentGroup = mutableListOf(info.index)
                currentY = info.yMedian
            }
        }
        groups.add(currentGroup)

        return groups.map { group ->
            group.sortedBy { idx ->
                val box = boxes[idx]
                minOf(box[0], box[2], box[4], box[6])
            }
        }
    }

    private fun mapSegmentToBoxes(
        segWords: List<String>,
        ocrWords: List<String>,
        charPositions: List<Int>
    ): List<WordInfo> {
        if (ocrWords.isEmpty()) return emptyList()
        val result = mutableListOf<WordInfo>()
        var segCharOffset = 0
        for (segWord in segWords) {
            if (segWord.isBlank()) { segCharOffset += segWord.length; continue }
            val boxIndices = mutableListOf<Int>()
            for (i in ocrWords.indices) {
                val ocrStart = charPositions[i]
                val ocrEnd = ocrStart + ocrWords[i].length
                if (ocrStart < segCharOffset + segWord.length && ocrEnd > segCharOffset) {
                    boxIndices.add(i)
                }
            }
            result.add(WordInfo(segWord, boxIndices))
            segCharOffset += segWord.length
        }
        return result
    }

    // ======================== UI 状态管理 ========================

    private fun showImage(bitmap: Bitmap) {
        currentBitmap = bitmap
        ivPreview.setImageBitmap(bitmap)
        emptyState.visibility = View.GONE
        if (isModelLoaded) recognizeImage() else tvStatus.text = "请先加载模型"
    }

    private fun showResultUI(result: OcrResult, lines: List<String>, elapsed: Long, lineBoxGroups: List<List<Int>>) {
        panelHome.visibility = View.GONE
        panelResult.visibility = View.VISIBLE
        modeSwitchBar.visibility = View.VISIBLE
        btnBack.visibility = View.VISIBLE
        emptyState.visibility = View.GONE
        ivPreview.setImageBitmap(result.imgWithBox)
        currentBitmap = result.imgWithBox
        setupOverlayBoxes()
        ocrOverlay.setTexts(allResultWords, lines)
        ocrOverlay.setLineIndices(lineBoxGroups)
        tvTime.text = "${elapsed}ms · ${segmentedWords.size} 个词 · ${lines.size} 行"
        tvResult.text = lines.joinToString("\n")
        switchMode(0)
    }

    private fun setupOverlayBoxes() {
        val iv = ivPreview
        val matrix = Matrix()
        val drawable = iv.drawable ?: return
        val viewW = iv.width.toFloat()
        val viewH = iv.height.toFloat()
        val imgW = drawable.intrinsicWidth.toFloat()
        val imgH = drawable.intrinsicHeight.toFloat()
        val scale = minOf(viewW / imgW, viewH / imgH)
        val dx = (viewW - imgW * scale) / 2f
        val dy = (viewH - imgH * scale) / 2f
        matrix.setScale(scale, scale)
        matrix.postTranslate(dx, dy)
        val imageRect = RectF(dx, dy, dx + imgW * scale, dy + imgH * scale)
        ocrOverlay.setBoxes(allResultBoxes, matrix, imageRect)
    }

    private fun resetToCapture() {
        panelHome.visibility = View.VISIBLE
        panelResult.visibility = View.GONE
        wordContainer.visibility = View.GONE
        modeSwitchBar.visibility = View.GONE
        btnBack.visibility = View.GONE
        emptyState.visibility = View.VISIBLE
        ocrOverlay.clear()
        tvStatus.text = if (isModelLoaded) "拍照或从相册选图" else "请先加载模型"
        currentMode = 0
        selectedText = ""
    }

    // ======================== 模式切换 ========================

    private fun switchMode(mode: Int) {
        currentMode = mode
        selectedText = ""
        when (mode) {
            0 -> {
                btnModeBox.setBackgroundResource(R.drawable.bg_mode_active)
                btnModeBox.setTextColor(Color.WHITE)
                btnModeLine.setBackgroundResource(0); btnModeLine.setTextColor(Color.parseColor("#80FFFFFF"))
                btnModeWord.setBackgroundResource(0); btnModeWord.setTextColor(Color.parseColor("#80FFFFFF"))
            }
            1 -> {
                btnModeLine.setBackgroundResource(R.drawable.bg_mode_active)
                btnModeLine.setTextColor(Color.WHITE)
                btnModeBox.setBackgroundResource(0); btnModeBox.setTextColor(Color.parseColor("#80FFFFFF"))
                btnModeWord.setBackgroundResource(0); btnModeWord.setTextColor(Color.parseColor("#80FFFFFF"))
            }
            2 -> {
                btnModeWord.setBackgroundResource(R.drawable.bg_mode_active)
                btnModeWord.setTextColor(Color.WHITE)
                btnModeBox.setBackgroundResource(0); btnModeBox.setTextColor(Color.parseColor("#80FFFFFF"))
                btnModeLine.setBackgroundResource(0); btnModeLine.setTextColor(Color.parseColor("#80FFFFFF"))
            }
        }
        when (mode) {
            0 -> {
                wordContainer.visibility = View.GONE; resultScroll.visibility = View.VISIBLE
                tvResult.text = allResultLines.joinToString("\n")
                ocrOverlay.setMode(OcrOverlayView.OcrMode.FRAME)
                ocrOverlay.clearSelection(); tvWordsBtnLabel.text = "分词"
            }
            1 -> {
                wordContainer.visibility = View.GONE; resultScroll.visibility = View.VISIBLE
                tvResult.text = allResultLines.joinToString("\n")
                ocrOverlay.setMode(OcrOverlayView.OcrMode.LINE)
                ocrOverlay.clearSelection(); tvWordsBtnLabel.text = "分词"
            }
            2 -> {
                wordContainer.visibility = View.VISIBLE; resultScroll.visibility = View.GONE
                wordAdapter.setWordInfos(segmentedWords)
                ocrOverlay.setMode(OcrOverlayView.OcrMode.WORD)
                ocrOverlay.clearSelection(); tvWordsBtnLabel.text = "逐行"
            }
        }
    }

    private fun onWordAdapterSelectionChanged() {
        val selectedInfos = wordAdapter.getSelectedWordInfos()
        if (selectedInfos.isNotEmpty()) {
            val boxIndices = mutableSetOf<Int>()
            selectedInfos.forEach { info -> info.boxIndices.forEach { boxIndices.add(it) } }
            ocrOverlay.setSelectedIndices(boxIndices)
            selectedText = wordAdapter.getSelectedText()
        } else {
            ocrOverlay.setSelectedIndices(emptySet())
            selectedText = ""
        }
    }

    private fun selectAllBoxes() {
        val all = (allResultWords.indices).toSet()
        ocrOverlay.setSelectedIndices(all)
        selectedText = allResultWords.joinToString("")
        updateTvResultHighlight()
    }

    private fun selectAllLines() {
        val all = (allResultLines.indices).toSet()
        ocrOverlay.setSelectedIndices(all)
        selectedText = allResultLines.joinToString("\n")
        updateTvResultHighlight()
    }

    // ======================== 复制 & 分享 ========================

    private fun copyResult() {
        val text = when {
            selectedText.isNotEmpty() -> selectedText
            currentMode == 2 -> segmentedWords.joinToString("") { it.text }
            else -> resultText
        }
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("OCR结果", text))
        Toast.makeText(this, "✅ 已复制", Toast.LENGTH_SHORT).show()
    }

    private fun shareResult() {
        val text = when {
            selectedText.isNotEmpty() -> selectedText
            currentMode == 2 -> segmentedWords.joinToString("") { it.text }
            else -> resultText
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(intent, "分享识别结果"))
    }

    // ======================== 工具方法 ========================

    private fun launchCamera() {
        val photoFile = File(cacheDir, "ocr_${System.currentTimeMillis()}.jpg")
        currentPhotoUri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", photoFile)
        cameraLauncher.launch(currentPhotoUri!!)
    }

    private fun loadBitmapFromUri(uri: Uri): Bitmap? = try {
        val ins: InputStream? = contentResolver.openInputStream(uri)
        val bmp = BitmapFactory.decodeStream(ins)
        ins?.close()
        val max = 2048
        if (bmp.width > max || bmp.height > max) {
            val s = max.toFloat() / maxOf(bmp.width, bmp.height)
            Bitmap.createScaledBitmap(bmp, (bmp.width * s).toInt(), (bmp.height * s).toInt(), true)
        } else bmp
    } catch (e: Exception) {
        tvStatus.text = "加载图片失败: ${e.message}"
        null
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isModelLoaded) ocr.releaseModel()
        try { segmentBs.destroy() } catch (_: Exception) {}
        downloadExecutor.shutdownNow()
    }
}
