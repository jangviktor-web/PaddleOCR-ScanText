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
import kotlin.math.abs

data class WordInfo(val text: String, val boxIndices: List<Int>)

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "OCRDemo"
    }

    private data class LangInfo(val modelDir: String, val labelPath: String)

    private val languageConfig = linkedMapOf(
        "中文 + English" to LangInfo("ch_PP-OCRv4", "labels/ppocr_keys_v1.txt"),
        "日本語" to LangInfo("japan_PP-OCRv3", "labels/japan_dict.txt"),
        "한국어" to LangInfo("korean_PP-OCRv3", "labels/korean_dict.txt"),
        "繁體中文" to LangInfo("chinese_cht_PP-OCRv3", "labels/chinese_cht_dict.txt"),
    )

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
    private lateinit var tvResult: TextView
    private lateinit var tvTime: TextView
    private lateinit var wordContainer: FrameLayout
    private lateinit var resultScroll: ScrollView
    private lateinit var rvWords: RecyclerView
    private lateinit var btnSelectAll: TextView
    private lateinit var btnBack: ImageView
    private lateinit var spinnerLang: Spinner
    private lateinit var tvWordsBtnLabel: TextView

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

    private val segmentBs: SegmentBs by lazy { SegmentBs.newInstance() }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && currentPhotoUri != null) loadBitmapFromUri(currentPhotoUri!!)?.let { showImage(it) }
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
        spinnerLang.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, languageConfig.keys.toList())
        spinnerLang.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                loadModel()
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        ocrOverlay.onLineSelected = { li, lt ->
            selectedText = if (li >= 0) lt else ""
            updateTvResultHighlight()
        }
        ocrOverlay.onBoxSelected = { _, _ ->
            val idx = ocrOverlay.getSelectedIndices()
            selectedText = if (idx.isNotEmpty()) idx.sorted().joinToString("") { i ->
                if (i < allResultWords.size) allResultWords[i] else ""
            } else ""
            updateTvResultHighlight()
        }
    }

    private fun updateTvResultHighlight() {
        if (selectedText.isEmpty()) {
            tvResult.text = allResultLines.joinToString("\n")
            return
        }
        val ft = allResultLines.joinToString("\n")
        val sp = android.text.SpannableString(ft)
        var sf = 0
        while (sf < ft.length) {
            val s = ft.indexOf(selectedText, sf)
            if (s < 0) break
            sp.setSpan(
                android.text.style.BackgroundColorSpan(Color.parseColor("#662196F3")),
                s, s + selectedText.length,
                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            sf = s + selectedText.length
        }
        tvResult.text = sp
    }

    private fun setupListeners() {
        findViewById<View>(R.id.btn_camera_home).setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
                launchCamera()
            else permissionLauncher.launch(Manifest.permission.CAMERA)
        }
        findViewById<View>(R.id.btn_gallery_home).setOnClickListener { galleryLauncher.launch("image/*") }
        btnBack.setOnClickListener { resetToCapture() }
        btnModeBox.setOnClickListener { switchMode(0) }
        btnModeLine.setOnClickListener { switchMode(1) }
        btnModeWord.setOnClickListener { switchMode(2) }
        findViewById<View>(R.id.btn_action_copy).setOnClickListener { copyResult() }
        findViewById<View>(R.id.btn_action_share).setOnClickListener { shareResult() }
        findViewById<View>(R.id.btn_action_words).setOnClickListener { switchMode(2) }
        val sla = View.OnClickListener {
            when (currentMode) {
                0 -> selectAllBoxes()
                1 -> selectAllLines()
                2 -> { if (wordAdapter.itemCount > 0) wordAdapter.selectAll() }
            }
        }
        findViewById<View>(R.id.btn_action_select_all).setOnClickListener(sla)
        btnSelectAll.setOnClickListener(sla)
    }

    private fun loadModel() {
        val sel = spinnerLang.selectedItem as? String ?: return
        val info = languageConfig[sel] ?: return
        tvStatus.text = "正在加载 $sel 模型..."
        val c = OcrConfig().apply {
            modelPath = "models/${info.modelDir}"
            labelPath = info.labelPath
            clsModelFilename = "cls.nb"
            detModelFilename = "det.nb"
            recModelFilename = "rec.nb"
            isRunDet = true
            isRunCls = true
            isRunRec = true
            cpuPowerMode = CpuPowerMode.LITE_POWER_FULL
            isDrwwTextPositionBox = true
        }
        if (isModelLoaded) {
            ocr.releaseModel()
            isModelLoaded = false
        }
        ocr.initModel(c, object : OcrInitCallback {
            override fun onSuccess() = runOnUiThread {
                isModelLoaded = true
                tvStatus.text = "✅ $sel 就绪"
            }
            override fun onFail(e: Throwable) = runOnUiThread {
                tvStatus.text = "❌ 加载失败: ${e.message}"
            }
        })
    }

    private fun recognizeImage() {
        val bmp = currentBitmap ?: return
        if (!isModelLoaded) { tvStatus.text = "请先加载模型"; return }
        loadingOverlay.visibility = View.VISIBLE
        tvLoading.text = "正在识别..."
        val t0 = System.currentTimeMillis()
        ocr.run(bmp, object : OcrRunCallback {
            override fun onSuccess(result: OcrResult) {
                val el = System.currentTimeMillis() - t0
                val wl = ocr.getWordLabels()
                val lines = result.simpleText.split("\n").filter { it.isNotBlank() }
                val words = mutableListOf<String>()
                val boxes = mutableListOf<FloatArray>()
                val cp = mutableListOf<Int>()
                var co = 0
                result.outputRawResult.forEachIndexed { _, item ->
                    val sb = StringBuilder()
                    item.wordIndex.forEach { idx -> if (idx in wl.indices) sb.append(wl[idx]) }
                    val w = sb.toString()
                    if (w.isNotBlank()) {
                        words.add(w); cp.add(co); co += w.length
                        val pts = item.points
                        val fp = FloatArray(pts.size * 2)
                        for (i in pts.indices) { fp[i * 2] = pts[i].x.toFloat(); fp[i * 2 + 1] = pts[i].y.toFloat() }
                        boxes.add(fp)
                    }
                }
                val ft = words.joinToString("")
                val sr = try {
                    segmentBs.segment(ft, SegmentResultHandlers.word()) as List<String>
                } catch (e: Exception) { Log.e(TAG, "分词失败", e); words }
                val sw = mapSegmentToBoxes(sr, words, cp)
                val lbg = computeLineBoxGroups(boxes)
                runOnUiThread {
                    loadingOverlay.visibility = View.GONE
                    allResultBoxes = boxes; allResultWords = words; allResultLines = lines
                    segmentedWords = sw; resultText = lines.joinToString("\n")
                    showResultUI(result, lines, el, lbg)
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
        data class BYI(val i: Int, val y: Float)
        val byi = boxes.mapIndexed { idx, b ->
            val ys = floatArrayOf(b[1], b[3], b[5], b[7])
            BYI(idx, ys.sorted().let { (it[1] + it[2]) / 2f })
        }.sortedBy { it.y }
        val g = mutableListOf<MutableList<Int>>()
        var cg = mutableListOf(byi[0].i)
        var cy = byi[0].y
        for (i in 1 until byi.size) {
            val info = byi[i]
            if (abs(info.y - cy) < 15f) cg.add(info.i)
            else { g.add(cg); cg = mutableListOf(info.i); cy = info.y }
        }
        g.add(cg)
        return g.map { gr -> gr.sortedBy { idx -> val b = boxes[idx]; minOf(b[0], b[2], b[4], b[6]) } }
    }

    private fun mapSegmentToBoxes(sw: List<String>, ow: List<String>, cp: List<Int>): List<WordInfo> {
        if (ow.isEmpty()) return emptyList()
        val r = mutableListOf<WordInfo>()
        var so = 0
        for (s in sw) {
            if (s.isBlank()) { so += s.length; continue }
            val bi = mutableListOf<Int>()
            for (i in ow.indices) {
                val os = cp[i]; val oe = os + ow[i].length
                if (os < so + s.length && oe > so) bi.add(i)
            }
            r.add(WordInfo(s, bi)); so += s.length
        }
        return r
    }

    private fun showImage(bmp: Bitmap) {
        currentBitmap = bmp; ivPreview.setImageBitmap(bmp); emptyState.visibility = View.GONE
        if (isModelLoaded) recognizeImage() else tvStatus.text = "请先加载模型"
    }

    private fun showResultUI(result: OcrResult, lines: List<String>, elapsed: Long, lbg: List<List<Int>>) {
        panelHome.visibility = View.GONE; panelResult.visibility = View.VISIBLE
        modeSwitchBar.visibility = View.VISIBLE; btnBack.visibility = View.VISIBLE
        emptyState.visibility = View.GONE
        ivPreview.setImageBitmap(result.imgWithBox); currentBitmap = result.imgWithBox
        setupOverlayBoxes()
        ocrOverlay.setTexts(allResultWords, lines); ocrOverlay.setLineIndices(lbg)
        tvTime.text = "${elapsed}ms · ${segmentedWords.size} 个词 · ${lines.size} 行"
        tvResult.text = lines.joinToString("\n")
        switchMode(0)
    }

    private fun setupOverlayBoxes() {
        val iv = ivPreview; val d = iv.drawable ?: return
        val vw = iv.width.toFloat(); val vh = iv.height.toFloat()
        val iw = d.intrinsicWidth.toFloat(); val ih = d.intrinsicHeight.toFloat()
        val s = minOf(vw / iw, vh / ih)
        val dx = (vw - iw * s) / 2f; val dy = (vh - ih * s) / 2f
        val m = Matrix().apply { setScale(s, s); postTranslate(dx, dy) }
        ocrOverlay.setBoxes(allResultBoxes, m, RectF(dx, dy, dx + iw * s, dy + ih * s))
    }

    private fun resetToCapture() {
        panelHome.visibility = View.VISIBLE; panelResult.visibility = View.GONE
        wordContainer.visibility = View.GONE; modeSwitchBar.visibility = View.GONE
        btnBack.visibility = View.GONE; emptyState.visibility = View.VISIBLE
        ocrOverlay.clear()
        ivPreview.setImageBitmap(null)
        currentBitmap = null
        allResultBoxes = emptyList(); allResultWords = emptyList(); allResultLines = emptyList()
        segmentedWords = emptyList(); resultText = ""
        tvStatus.text = if (isModelLoaded) "拍照或从相册选图" else "请先加载模型"
        currentMode = 0; selectedText = ""
    }

    private fun switchMode(mode: Int) {
        currentMode = mode; selectedText = ""
        val act = Color.WHITE; val inact = Color.parseColor("#80FFFFFF")
        val btns = listOf(btnModeBox, btnModeLine, btnModeWord)
        btns.forEachIndexed { i, b ->
            if (i == mode) { b.setBackgroundResource(R.drawable.bg_mode_active); b.setTextColor(act) }
            else { b.setBackgroundResource(0); b.setTextColor(inact) }
        }
        when (mode) {
            0 -> { wordContainer.visibility = View.GONE; resultScroll.visibility = View.VISIBLE; tvResult.text = allResultLines.joinToString("\n"); ocrOverlay.setMode(OcrOverlayView.OcrMode.FRAME); ocrOverlay.clearSelection(); tvWordsBtnLabel.text = "分词" }
            1 -> { wordContainer.visibility = View.GONE; resultScroll.visibility = View.VISIBLE; tvResult.text = allResultLines.joinToString("\n"); ocrOverlay.setMode(OcrOverlayView.OcrMode.LINE); ocrOverlay.clearSelection(); tvWordsBtnLabel.text = "分词" }
            2 -> { wordContainer.visibility = View.VISIBLE; resultScroll.visibility = View.GONE; wordAdapter.setWordInfos(segmentedWords); ocrOverlay.setMode(OcrOverlayView.OcrMode.WORD); ocrOverlay.clearSelection(); tvWordsBtnLabel.text = "逐行" }
        }
    }

    private fun onWordAdapterSelectionChanged() {
        val si = wordAdapter.getSelectedWordInfos()
        if (si.isNotEmpty()) {
            val bi = mutableSetOf<Int>()
            si.forEach { it.boxIndices.forEach { i -> bi.add(i) } }
            ocrOverlay.setSelectedIndices(bi)
            selectedText = wordAdapter.getSelectedText()
        } else {
            ocrOverlay.setSelectedIndices(emptySet()); selectedText = ""
        }
    }

    private fun selectAllBoxes() {
        val a = (allResultWords.indices).toSet()
        ocrOverlay.setSelectedIndices(a); selectedText = allResultWords.joinToString("")
        updateTvResultHighlight()
    }

    private fun selectAllLines() {
        val a = (allResultLines.indices).toSet()
        ocrOverlay.setSelectedIndices(a); selectedText = allResultLines.joinToString("\n")
        updateTvResultHighlight()
    }

    private fun copyResult() {
        val t = when {
            selectedText.isNotEmpty() -> selectedText
            currentMode == 2 -> segmentedWords.joinToString("") { it.text }
            else -> resultText
        }
        (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("OCR结果", t))
        Toast.makeText(this, "✅ 已复制", Toast.LENGTH_SHORT).show()
    }

    private fun shareResult() {
        val t = when {
            selectedText.isNotEmpty() -> selectedText
            currentMode == 2 -> segmentedWords.joinToString("") { it.text }
            else -> resultText
        }
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, t)
        }, "分享识别结果"))
    }

    private fun launchCamera() {
        val f = File(cacheDir, "ocr_${System.currentTimeMillis()}.jpg")
        currentPhotoUri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", f)
        cameraLauncher.launch(currentPhotoUri!!)
    }

    private fun loadBitmapFromUri(uri: Uri): Bitmap? = try {
        val ins = contentResolver.openInputStream(uri)
        val bmp = BitmapFactory.decodeStream(ins)
        ins?.close()
        val mx = 2048
        if (bmp.width > mx || bmp.height > mx) {
            val s = mx.toFloat() / maxOf(bmp.width, bmp.height)
            Bitmap.createScaledBitmap(bmp, (bmp.width * s).toInt(), (bmp.height * s).toInt(), true)
        } else bmp
    } catch (e: Exception) { tvStatus.text = "加载图片失败: ${e.message}"; null }

    override fun onDestroy() {
        super.onDestroy()
        if (isModelLoaded) ocr.releaseModel()
        try { segmentBs.destroy() } catch (_: Exception) {}
    }
}
