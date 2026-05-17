package com.equationl.paddleocr4android.app

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * OCR 结果覆盖层：在图片上绘制文字检测框，支持触摸选择
 *
 * 模式：
 * - FRAME：手指拖动画矩形，框内所有文字一次性选中
 * - LINE：点击一行选中整行，可连续点击累加选择
 * - WORD：逐词选择（由分词面板驱动）
 */
class OcrOverlayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val boxPaint = Paint().apply {
        color = Color.parseColor("#BB2196F3")
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }

    private val selectedBoxPaint = Paint().apply {
        color = Color.parseColor("#DD2196F3")
        style = Paint.Style.FILL
        strokeWidth = 0f
    }

    private val lineStrokePaint = Paint().apply {
        color = Color.parseColor("#BB2196F3")
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val selectedLineFillPaint = Paint().apply {
        color = Color.parseColor("#552196F3")
        style = Paint.Style.FILL
    }

    private val selectedLineStrokePaint = Paint().apply {
        color = Color.parseColor("#FF2196F3")
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }

    private val touchFeedbackPaint = Paint().apply {
        color = Color.parseColor("#33FFFFFF")
        style = Paint.Style.FILL
    }

    // 框选拖拽矩形画笔
    private val dragRectFillPaint = Paint().apply {
        color = Color.parseColor("#22165DFF")
        style = Paint.Style.FILL
    }

    private val dragRectStrokePaint = Paint().apply {
        color = Color.parseColor("#FF165DFF")
        style = Paint.Style.STROKE
        strokeWidth = 3f
        pathEffect = DashPathEffect(floatArrayOf(12f, 8f), 0f)
    }

    // OCR 返回的点位列表：每个元素是一组点 (8个 float：4个角点 x,y)
    private var wordBoxes: List<FloatArray> = emptyList()
    private var imageMatrix: Matrix? = null
    private var imageRect: RectF? = null
    private var selectedIndices: Set<Int> = emptySet()
    private var mode: OcrMode = OcrMode.FRAME

    // 文本数据（用于回调返回文字内容）
    private var allWords: List<String> = emptyList()
    private var allLines: List<String> = emptyList()

    // 行数据
    private var lineIndices: List<List<Int>> = emptyList()
    private var lineRects: List<RectF> = emptyList()
    private val selectedLineIndices = mutableSetOf<Int>()

    // 触摸反馈
    private var touchDownBoxIndex: Int = -1

    // 框选拖拽状态
    private var isDragging = false
    private var dragStartX = 0f
    private var dragStartY = 0f
    private var dragEndX = 0f
    private var dragEndY = 0f
    private var isDragShort = true // 拖拽距离过短视为点击

    // 回调
    var onLineSelected: ((lineIndex: Int, lineText: String) -> Unit)? = null
    var onBoxSelected: ((boxIndex: Int, boxText: String) -> Unit)? = null
    var onMultiBoxSelected: ((indices: Set<Int>, text: String) -> Unit)? = null

    enum class OcrMode { FRAME, LINE, WORD }

    fun setBoxes(boxes: List<FloatArray>, matrix: Matrix, imageRect: RectF) {
        this.wordBoxes = boxes
        this.imageMatrix = matrix
        this.imageRect = imageRect
        computeLineRects()
        invalidate()
    }

    fun setSelectedIndices(indices: Set<Int>) {
        this.selectedIndices = indices
        invalidate()
    }

    fun selectAllLines() {
        selectedLineIndices.clear()
        selectedLineIndices.addAll(lineIndices.indices)
        selectedIndices = wordBoxes.indices.toSet()
        invalidate()
    }

    fun getSelectedIndices(): Set<Int> = selectedIndices

    /**
     * 将 box 索引集合按原文阅读顺序排序（行号从上到下 → 行内从左到右）
     */
    fun sortByReadingOrder(indices: Set<Int>): List<Int> {
        if (lineIndices.isEmpty()) return indices.sorted()
        val posMap = mutableMapOf<Int, Pair<Int, Int>>()
        for ((lineIdx, line) in lineIndices.withIndex()) {
            for ((posInLine, boxIdx) in line.withIndex()) {
                posMap[boxIdx] = Pair(lineIdx, posInLine)
            }
        }
        return indices.sortedWith(compareBy(
            { posMap[it]?.first ?: Int.MAX_VALUE },
            { posMap[it]?.second ?: Int.MAX_VALUE }
        ))
    }

    fun setMode(mode: OcrMode) {
        this.mode = mode
        this.selectedLineIndices.clear()
        this.touchDownBoxIndex = -1
        this.isDragging = false
        computeLineRects()
        invalidate()
    }

    fun setLineIndices(indices: List<List<Int>>) {
        this.lineIndices = indices
        computeLineRects()
    }

    fun setTexts(words: List<String>, lines: List<String>) {
        this.allWords = words
        this.allLines = lines
    }

    fun clearSelection() {
        selectedIndices = emptySet()
        selectedLineIndices.clear()
        touchDownBoxIndex = -1
        isDragging = false
        invalidate()
    }

    fun clear() {
        wordBoxes = emptyList()
        selectedIndices = emptySet()
        allWords = emptyList()
        allLines = emptyList()
        lineIndices = emptyList()
        lineRects = emptyList()
        selectedLineIndices.clear()
        touchDownBoxIndex = -1
        isDragging = false
        invalidate()
    }

    /**
     * 根据 box 坐标和行分组，计算每行在屏幕上的矩形区域
     */
    private fun computeLineRects() {
        if (wordBoxes.isEmpty() || imageMatrix == null || lineIndices.isEmpty()) {
            lineRects = emptyList()
            return
        }
        val mat = imageMatrix!!
        val results = mutableListOf<RectF>()

        for (lineBoxes in lineIndices) {
            var minX = Float.MAX_VALUE
            var minY = Float.MAX_VALUE
            var maxX = Float.MIN_VALUE
            var maxY = Float.MIN_VALUE

            for (boxIdx in lineBoxes) {
                if (boxIdx >= wordBoxes.size) continue
                val box = wordBoxes[boxIdx]
                val pts = FloatArray(8)
                for (i in 0 until 4) {
                    pts[i * 2] = box[i * 2]
                    pts[i * 2 + 1] = box[i * 2 + 1]
                }
                mat.mapPoints(pts)
                for (i in 0 until 4) {
                    minX = minOf(minX, pts[i * 2])
                    minY = minOf(minY, pts[i * 2 + 1])
                    maxX = maxOf(maxX, pts[i * 2])
                    maxY = maxOf(maxY, pts[i * 2 + 1])
                }
            }

            if (minX < Float.MAX_VALUE) {
                results.add(RectF(minX - 4, minY - 4, maxX + 4, maxY + 4))
            }
        }
        lineRects = results
    }

    // ======================== 触摸交互 ========================

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (wordBoxes.isEmpty()) return false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                when (mode) {
                    OcrMode.FRAME -> {
                        dragStartX = event.x
                        dragStartY = event.y
                        dragEndX = event.x
                        dragEndY = event.y
                        isDragging = false
                        isDragShort = true
                        touchDownBoxIndex = findBoxAtPoint(event.x, event.y)
                        invalidate()
                        return true
                    }
                    OcrMode.LINE -> {
                        touchDownBoxIndex = findBoxAtPoint(event.x, event.y)
                        invalidate()
                        return true
                    }
                    OcrMode.WORD -> {
                        touchDownBoxIndex = findBoxAtPoint(event.x, event.y)
                        if (touchDownBoxIndex >= 0) {
                            invalidate()
                            return true
                        }
                        return false
                    }
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (mode == OcrMode.FRAME) {
                    dragEndX = event.x
                    dragEndY = event.y
                    val dx = dragEndX - dragStartX
                    val dy = dragEndY - dragStartY
                    if (dx * dx + dy * dy > 400f) { // > 20px
                        isDragging = true
                        isDragShort = false
                        touchDownBoxIndex = -1
                    }
                    invalidate()
                    return true
                }
            }
            MotionEvent.ACTION_UP -> {
                val downBox = touchDownBoxIndex
                touchDownBoxIndex = -1

                when (mode) {
                    OcrMode.FRAME -> {
                        if (isDragging) {
                            // 框选完成：选中矩形内所有 box
                            val rect = getDragSelectionRect() ?: run {
                                isDragging = false
                                invalidate()
                                return true
                            }
                            val hitIndices = mutableSetOf<Int>()
                            for (i in wordBoxes.indices) {
                                if (isBoxCenterInRect(i, rect)) {
                                    hitIndices.add(i)
                                }
                            }
                            selectedIndices = hitIndices
                            isDragging = false
                            // 按阅读顺序拼接选中框的文字
                            val ordered = sortByReadingOrder(hitIndices)
                            val text = ordered.joinToString("") { i ->
                                if (i < allWords.size) allWords[i] else ""
                            }
                            onMultiBoxSelected?.invoke(hitIndices, text)
                            invalidate()
                            return true
                        } else {
                            // 短按：点击单个 box 切换选中
                            val boxIdx = findBoxAtPoint(event.x, event.y)
                            if (boxIdx >= 0) {
                                val newSelected = selectedIndices.toMutableSet()
                                if (boxIdx in newSelected) newSelected.remove(boxIdx)
                                else newSelected.add(boxIdx)
                                selectedIndices = newSelected
                                if (boxIdx < allWords.size) {
                                    onBoxSelected?.invoke(boxIdx, allWords[boxIdx])
                                }
                                invalidate()
                                return true
                            }
                        }
                    }
                    OcrMode.LINE -> {
                        val lineIdx = findLineAtPoint(event.x, event.y)
                        if (lineIdx >= 0) {
                            if (lineIdx in selectedLineIndices) {
                                // 取消选中该行
                                selectedLineIndices.remove(lineIdx)
                                // 重建 selectedIndices
                                val newSet = mutableSetOf<Int>()
                                for (li in selectedLineIndices) {
                                    if (li < lineIndices.size) {
                                        lineIndices[li].forEach { newSet.add(it) }
                                    }
                                }
                                selectedIndices = newSet
                                if (selectedLineIndices.isEmpty()) {
                                    onLineSelected?.invoke(-1, "")
                                } else {
                                    val text = selectedLineIndices.sorted().joinToString("\n") { li ->
                                        if (li < allLines.size) allLines[li] else ""
                                    }
                                    onLineSelected?.invoke(lineIdx, text)
                                }
                            } else {
                                // 选中该行（累加）
                                selectedLineIndices.add(lineIdx)
                                val lineBoxSet = mutableSetOf<Int>()
                                if (lineIdx < lineIndices.size) {
                                    lineIndices[lineIdx].forEach { lineBoxSet.add(it) }
                                }
                                selectedIndices = selectedIndices + lineBoxSet
                                val text = selectedLineIndices.sorted().joinToString("\n") { li ->
                                    if (li < allLines.size) allLines[li] else ""
                                }
                                onLineSelected?.invoke(lineIdx, text)
                            }
                            invalidate()
                            return true
                        }
                    }
                    OcrMode.WORD -> {
                        val boxIdx = findBoxAtPoint(event.x, event.y)
                        if (boxIdx >= 0 && downBox == boxIdx) {
                            val newSelected = selectedIndices.toMutableSet()
                            if (boxIdx in newSelected) newSelected.remove(boxIdx)
                            else newSelected.add(boxIdx)
                            selectedIndices = newSelected
                            if (boxIdx < allWords.size) {
                                onBoxSelected?.invoke(boxIdx, allWords[boxIdx])
                            }
                            invalidate()
                            return true
                        }
                    }
                }
                invalidate()
            }
            MotionEvent.ACTION_CANCEL -> {
                touchDownBoxIndex = -1
                isDragging = false
                invalidate()
            }
        }
        return false
    }

    /**
     * 获取拖拽选区矩形（屏幕坐标）
     */
    private fun getDragSelectionRect(): RectF? {
        if (!isDragging) return null
        val left = min(dragStartX, dragEndX)
        val top = min(dragStartY, dragEndY)
        val right = max(dragStartX, dragEndX)
        val bottom = max(dragStartY, dragEndY)
        if (right - left < 10f || bottom - top < 10f) return null
        return RectF(left, top, right, bottom)
    }

    /**
     * 判断某个 box 的中心点是否在矩形内
     */
    private fun isBoxCenterInRect(boxIdx: Int, rect: RectF): Boolean {
        if (boxIdx >= wordBoxes.size || imageMatrix == null) return false
        val box = wordBoxes[boxIdx]
        val mat = imageMatrix!!
        val pts = floatArrayOf(
            (box[0] + box[4]) / 2f,
            (box[1] + box[5]) / 2f
        )
        mat.mapPoints(pts)
        return rect.contains(pts[0], pts[1])
    }

    /**
     * 测试触摸点是否在某个 box 的四边形内
     */
    private fun findBoxAtPoint(tx: Float, ty: Float): Int {
        val mat = imageMatrix ?: return -1

        for (index in wordBoxes.indices.reversed()) {
            val box = wordBoxes[index]
            val pts = FloatArray(8)
            for (i in 0 until 4) {
                pts[i * 2] = box[i * 2]
                pts[i * 2 + 1] = box[i * 2 + 1]
            }
            mat.mapPoints(pts)

            if (isPointInPolygon(tx, ty, pts)) {
                return index
            }
        }
        return -1
    }

    /**
     * 测试触摸点是否在某个行矩形内
     */
    private fun findLineAtPoint(tx: Float, ty: Float): Int {
        for (i in lineRects.indices.reversed()) {
            if (lineRects[i].contains(tx, ty)) {
                return i
            }
        }
        return -1
    }

    /**
     * 判断点 (px,py) 是否在凸多边形内
     */
    private fun isPointInPolygon(px: Float, py: Float, pts: FloatArray): Boolean {
        var sign = 0
        for (i in 0 until 4) {
            val x1 = pts[i * 2]
            val y1 = pts[i * 2 + 1]
            val x2 = pts[((i + 1) % 4) * 2]
            val y2 = pts[((i + 1) % 4) * 2 + 1]
            val cross = (px - x1) * (y2 - y1) - (py - y1) * (x2 - x1)
            if (cross != 0f) {
                val s = if (cross > 0) 1 else -1
                if (sign == 0) sign = s
                else if (sign != s) return false
            }
        }
        return true
    }

    // ======================== 绘制 ========================

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (wordBoxes.isEmpty() || imageMatrix == null || imageRect == null) return

        val mat = imageMatrix!!

        when (mode) {
            OcrMode.LINE -> {
                drawLineMode(canvas, mat)
            }
            OcrMode.FRAME -> {
                drawFrameMode(canvas, mat)
            }
            OcrMode.WORD -> {
                drawWordMode(canvas, mat)
            }
        }
    }

    /**
     * 逐行模式：绘制行区域 + 单个 box，支持多行累加选中
     */
    private fun drawLineMode(canvas: Canvas, mat: Matrix) {
        // 绘制行区域矩形
        for ((lineIdx, rect) in lineRects.withIndex()) {
            val isSelected = lineIdx in selectedLineIndices
            val paint = if (isSelected) selectedLineStrokePaint else lineStrokePaint
            canvas.drawRect(rect, paint)
            if (isSelected) {
                canvas.drawRect(rect, selectedLineFillPaint)
            }
        }

        // 绘制所有 box
        for ((index, box) in wordBoxes.withIndex()) {
            val pts = transformBox(box, mat)
            val path = boxToPath(pts)

            if (index in selectedIndices) {
                canvas.drawPath(path, selectedBoxPaint)
            }
            canvas.drawPath(path, boxPaint)

            if (index == touchDownBoxIndex) {
                canvas.drawPath(path, touchFeedbackPaint)
            }
        }
    }

    /**
     * 框选模式：绘制 box + 拖拽选区矩形
     */
    private fun drawFrameMode(canvas: Canvas, mat: Matrix) {
        // 绘制所有 box
        for ((index, box) in wordBoxes.withIndex()) {
            val pts = transformBox(box, mat)
            val path = boxToPath(pts)

            if (index in selectedIndices) {
                canvas.drawPath(path, selectedBoxPaint)
            }
            canvas.drawPath(path, boxPaint)

            if (index == touchDownBoxIndex) {
                canvas.drawPath(path, touchFeedbackPaint)
            }
        }

        // 绘制拖拽选区矩形
        if (isDragging) {
            val rect = getDragSelectionRect()
            if (rect != null) {
                canvas.drawRect(rect, dragRectFillPaint)
                canvas.drawRect(rect, dragRectStrokePaint)
            }
        }
    }

    /**
     * WORD 模式：高亮选中的 box
     */
    private fun drawWordMode(canvas: Canvas, mat: Matrix) {
        for ((index, box) in wordBoxes.withIndex()) {
            val pts = transformBox(box, mat)
            val path = boxToPath(pts)

            if (index in selectedIndices) {
                canvas.drawPath(path, selectedBoxPaint)
            }
            canvas.drawPath(path, boxPaint)

            if (index == touchDownBoxIndex) {
                canvas.drawPath(path, touchFeedbackPaint)
            }
        }
    }

    private fun transformBox(box: FloatArray, mat: Matrix): FloatArray {
        val pts = FloatArray(8)
        for (i in 0 until 4) {
            pts[i * 2] = box[i * 2]
            pts[i * 2 + 1] = box[i * 2 + 1]
        }
        mat.mapPoints(pts)
        return pts
    }

    private fun boxToPath(pts: FloatArray): Path {
        return Path().apply {
            moveTo(pts[0], pts[1])
            lineTo(pts[2], pts[3])
            lineTo(pts[4], pts[5])
            lineTo(pts[6], pts[7])
            close()
        }
    }
}
