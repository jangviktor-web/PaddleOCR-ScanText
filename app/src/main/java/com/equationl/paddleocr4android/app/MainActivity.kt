package com.equationl.paddleocr4android.app

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.net.Uri
import android.os.Bundle
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
import java.io.File
import java.io.InputStream
import kotlin.math.abs

/**
 * 分词结果：一个词 + 对应的 OCR box 索引列表
 */
data class WordInfo(
    val text: String,
    val boxIndices: List<Int> // 对应 allResultBoxes 中的索引
)

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "OCRDemo"
    }

    private val languageMap = linkedMapOf(
        "中文 + English" to "ch_PP-OCRv4",
        "English" to "en_PP-OCRv4",
        "日本語" to "japan_PP-OCRv4",
        "한국어" to "korean_PP-OCRv4",
        "Français" to "french_PP-OCRv4",
        "Deutsch" to "german_PP-OCRv4",
        "Русский" to "russian_PP-OCRv4",
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

    // 逻辑
    private lateinit var ocr: OCR
    private lateinit var wordAdapter: WordAdapter
    private var currentBitmap: Bitmap? = null
    private var isModelLoaded = false
    private var currentMode = 0 // 0=框选, 1=逐行, 2=分词
    private var allResultBoxes: List<FloatArray> = emptyList()
    private var allResultWords: List<String> = emptyList()
    private var allResultLines: List<String> = emptyList()
    private var segmentedWords: List<WordInfo> = emptyList() // 结巴分词结果
    private var currentPhotoUri: Uri? = null
    private var resultText: String = ""      // 识别后的完整文本（复制用）
    private var selectedText: String = ""    // 当前选中的文本（复制/分享用）

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
        initViews()
        setupListeners()
    }

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

        rvWords.layoutManager = GridLayoutManager(this, 3)
        rvWords.adapter = wordAdapter

        val langNames = languageMap.keys.toList()
        spinnerLang.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, langNames)

        // 图片上点击行 → 选中/取消
        ocrOverlay.onLineSelected = { lineIndex, lineText ->
            selectedText = if (lineIndex >= 0) lineText else ""
            updateTvResultHighlight()
        }

        // 图片上点击单个 box → 选中/取消
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

    /** 在结果文本中高亮选中的部分 */
    private fun updateTvResultHighlight() {
        if (selectedText.isEmpty()) {
            tvResult.text = allResultLines.joinToString("\n")
            return
        }
        // 高亮显示选中文本
        val fullText = allResultLines.joinToString("\n")
        val spannable = android.text.SpannableString(fullText)

        // 查找并高亮所有匹配的选中文本
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

        // 全选：所有模式都支持
        val selectAllListener = View.OnClickListener {
            when (currentMode) {
                0 -> selectAllBoxes()
                1 -> selectAllLines()
                2 -> {
                    if (wordAdapter.itemCount > 0) wordAdapter.selectAll()
                    // selectAll() 会触发 onSelectionChanged → 更新 selectedText
                }
            }
        }
        findViewById<View>(R.id.btn_action_select_all).setOnClickListener(selectAllListener)
        btnSelectAll.setOnClickListener(selectAllListener)
    }

    /** 框选模式：全选所有 box */
    private fun selectAllBoxes() {
        val all = (allResultWords.indices).toSet()
        ocrOverlay.setSelectedIndices(all)
        selectedText = allResultWords.joinToString("")
        updateTvResultHighlight()
    }

    /** 逐行模式：全选所有行 */
    private fun selectAllLines() {
        val all = (allResultLines.indices).toSet()
        ocrOverlay.setSelectedIndices(all)
        selectedText = allResultLines.joinToString("\n")
        updateTvResultHighlight()
    }

    // ======================== 模型 & 识别 ========================

    private fun loadModel() {
        val selectedLang = spinnerLang.selectedItem as String
        val modelDir = languageMap[selectedLang]!!
        val btnLoad = findViewById<View>(R.id.btn_load_model)
        btnLoad.isEnabled = false
        tvStatus.text = "正在加载 $selectedLang 模型..."
        tvModelLabel.text = "加载中..."

        val config = OcrConfig().apply {
            modelPath = "models/$modelDir"
            clsModelFilename = "cls.nb"
            detModelFilename = "det.nb"
            recModelFilename = "rec.nb"
            isRunDet = true; isRunCls = true; isRunRec = true
            cpuPowerMode = CpuPowerMode.LITE_POWER_FULL
            isDrwwTextPositionBox = true
        }

        if (isModelLoaded) { ocr.releaseModel(); isModelLoaded = false }

        ocr.initModel(config, object : OcrInitCallback {
            override fun onSuccess() = runOnUiThread {
                isModelLoaded = true
                tvStatus.text = "✅ $selectedLang 就绪"
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

                // 记录每个 OCR 词的起始字符位置（用于后续分词对齐）
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

                // 拼接完整文本用于结巴分词
                val fullText = words.joinToString("")

                // 执行结巴分词（在后台线程）
                val segResult = try {
                    segmentBs.segment(fullText, SegmentResultHandlers.word()) as List<String>
                } catch (e: Exception) {
                    Log.e(TAG, "分词失败，回退到原始分词", e)
                    words
                }

                // 将分词结果映射到 OCR box
                val segWords = mapSegmentToBoxes(segResult, words, charPositions)

                // 计算每行包含哪些 box 索引（按 Y 坐标中位数分组）
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

    /**
     * 根据 box 的 Y 坐标中位数，将 box 分组到不同的行
     */
    private fun computeLineBoxGroups(boxes: List<FloatArray>): List<List<Int>> {
        if (boxes.isEmpty()) return emptyList()

        // 计算每个 box 的 Y 中位数
        data class BoxYInfo(val index: Int, val yMedian: Float)
        val boxYInfos = boxes.mapIndexed { idx, box ->
            val ys = floatArrayOf(box[1], box[3], box[5], box[7])
            BoxYInfo(idx, ys.sorted().let { (it[1] + it[2]) / 2f })
        }.sortedBy { it.yMedian }

        // 按 Y 中位数聚类（相邻差值 < 阈值视为同一行）
        val groups = mutableListOf<MutableList<Int>>()
        var currentGroup = mutableListOf(boxYInfos[0].index)
        var currentY = boxYInfos[0].yMedian

        val threshold = 15f // 同行判定阈值（像素）

        for (i in 1 until boxYInfos.size) {
            val info = boxYInfos[i]
            if (abs(info.yMedian - currentY) < threshold) {
                currentGroup.add(info.index)
            } else {
                groups.add(currentGroup)
                currentGroup = mutableListOf(info.index)
                currentY = info.yMedian
            }
        }
        groups.add(currentGroup)

        // 每行内按 X 坐标排序
        return groups.map { group ->
            group.sortedBy { idx ->
                val box = boxes[idx]
                minOf(box[0], box[2], box[4], box[6])
            }
        }
    }

    /**
     * 将结巴分词结果映射到 OCR box 索引
     */
    private fun mapSegmentToBoxes(
        segWords: List<String>,
        ocrWords: List<String>,
        charPositions: List<Int>
    ): List<WordInfo> {
        if (ocrWords.isEmpty()) return emptyList()

        val result = mutableListOf<WordInfo>()
        var segCharOffset = 0

        for (segWord in segWords) {
            if (segWord.isBlank()) {
                segCharOffset += segWord.length
                continue
            }

            val boxIndices = mutableListOf<Int>()

            for (i in ocrWords.indices) {
                val ocrStart = charPositions[i]
                val ocrEnd = ocrStart + ocrWords[i].length
                val segStart = segCharOffset
                val segEnd = segCharOffset + segWord.length

                if (ocrStart < segEnd && ocrEnd > segStart) {
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

        if (isModelLoaded) {
            recognizeImage()
        } else {
            tvStatus.text = "请先加载模型"
        }
    }

    private fun showResultUI(
        result: OcrResult,
        lines: List<String>,
        elapsed: Long,
        lineBoxGroups: List<List<Int>>
    ) {
        panelHome.visibility = View.GONE
        panelResult.visibility = View.VISIBLE
        modeSwitchBar.visibility = View.VISIBLE
        btnBack.visibility = View.VISIBLE
        emptyState.visibility = View.GONE

        ivPreview.setImageBitmap(result.imgWithBox)
        currentBitmap = result.imgWithBox

        // 设置 overlay 数据
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

        // 更新模式按钮样式
        when (mode) {
            0 -> {
                btnModeBox.setBackgroundResource(R.drawable.bg_mode_active)
                btnModeBox.setTextColor(Color.WHITE)
                btnModeLine.setBackgroundResource(0)
                btnModeLine.setTextColor(Color.parseColor("#80FFFFFF"))
                btnModeWord.setBackgroundResource(0)
                btnModeWord.setTextColor(Color.parseColor("#80FFFFFF"))
            }
            1 -> {
                btnModeLine.setBackgroundResource(R.drawable.bg_mode_active)
                btnModeLine.setTextColor(Color.WHITE)
                btnModeBox.setBackgroundResource(0)
                btnModeBox.setTextColor(Color.parseColor("#80FFFFFF"))
                btnModeWord.setBackgroundResource(0)
                btnModeWord.setTextColor(Color.parseColor("#80FFFFFF"))
            }
            2 -> {
                btnModeWord.setBackgroundResource(R.drawable.bg_mode_active)
                btnModeWord.setTextColor(Color.WHITE)
                btnModeBox.setBackgroundResource(0)
                btnModeBox.setTextColor(Color.parseColor("#80FFFFFF"))
                btnModeLine.setBackgroundResource(0)
                btnModeLine.setTextColor(Color.parseColor("#80FFFFFF"))
            }
        }

        // 更新内容面板
        when (mode) {
            0 -> {
                // 框选模式：显示文字结果，overlay 为 LINE 模式（显示所有框）
                wordContainer.visibility = View.GONE
                resultScroll.visibility = View.VISIBLE
                tvResult.text = allResultLines.joinToString("\n")
                ocrOverlay.setMode(OcrOverlayView.OcrMode.FRAME)
                ocrOverlay.clearSelection()
                tvWordsBtnLabel.text = "分词"
            }
            1 -> {
                // 逐行模式：显示文字结果，overlay 为 LINE 模式（显示行框，点击选行）
                wordContainer.visibility = View.GONE
                resultScroll.visibility = View.VISIBLE
                tvResult.text = allResultLines.joinToString("\n")
                ocrOverlay.setMode(OcrOverlayView.OcrMode.LINE)
                ocrOverlay.clearSelection()
                tvWordsBtnLabel.text = "分词"
            }
            2 -> {
                // 分词模式：底部 chip 网格，overlay 为 WORD 模式
                wordContainer.visibility = View.VISIBLE
                resultScroll.visibility = View.GONE
                wordAdapter.setWordInfos(segmentedWords)
                ocrOverlay.setMode(OcrOverlayView.OcrMode.WORD)
                ocrOverlay.clearSelection()
                tvWordsBtnLabel.text = "逐行"
            }
        }
    }

    // ======================== 选择回调 ========================

    /** 分词模式下底部 chip 选择变化 */
    private fun onWordAdapterSelectionChanged() {
        val selectedInfos = wordAdapter.getSelectedWordInfos()
        if (selectedInfos.isNotEmpty()) {
            val boxIndices = mutableSetOf<Int>()
            selectedInfos.forEach { info ->
                info.boxIndices.forEach { boxIndices.add(it) }
            }
            ocrOverlay.setSelectedIndices(boxIndices)
            selectedText = wordAdapter.getSelectedText()
        } else {
            ocrOverlay.setSelectedIndices(emptySet())
            selectedText = ""
        }
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
    }
}
