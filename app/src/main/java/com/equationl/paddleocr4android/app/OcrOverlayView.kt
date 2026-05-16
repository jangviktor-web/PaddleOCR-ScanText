package com.equationl.paddleocr4android.app

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

/**
 * OCR 结果覆盖层：在图片上绘制文字检测框，支持触摸选择
 *
 * 模式：
 * - FRAME / LINE：显示所有框，点击可选择一行
 * - WORD：根据 selectedIndices 高亮选中的框（由分词模式驱动）
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
    private var lineIndices: List<List<Int>> = emptyList() // 每行包含的 box 索引
    private var lineRects: List<RectF> = emptyList()        // 每行的矩形区域（屏幕坐标）
    private var selectedLineIndex: Int = -1                 // 当前选中的行号

    // 触摸反馈
    private var touchDownBoxIndex: Int = -1                 // 按下时的 box 索引（用于按压反馈）

    // 回调
    var onLineSelected: ((lineIndex: Int, lineText: String) -> Unit)? = null
    var onBoxSelected: ((boxIndex: Int, boxText: String) -> Unit)? = null

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

    fun getSelectedIndices(): Set<Int> = selectedIndices

    fun setMode(mode: OcrMode) {
        this.mode = mode
        this.selectedLineIndex = -1
        this.touchDownBoxIndex = -1
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
        selectedLineIndex = -1
        touchDownBoxIndex = -1
        invalidate()
    }

    fun clear() {
        wordBoxes = emptyList()
        selectedIndices = emptySet()
        allWords = emptyList()
        allLines = emptyList()
        lineIndices = emptyList()
        lineRects = emptyList()
        selectedLineIndex = -1
        touchDownBoxIndex = -1
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
                touchDownBoxIndex = findBoxAtPoint(event.x, event.y)
                if (touchDownBoxIndex >= 0) {
                    invalidate()
                    return true
                }
                // 也检查是否点在行区域上
                if (mode == OcrMode.LINE || mode == OcrMode.FRAME) {
                    val lineIdx = findLineAtPoint(event.x, event.y)
                    if (lineIdx >= 0) {
                        invalidate()
                        return true
                    }
                }
                return false
            }
            MotionEvent.ACTION_UP -> {
                val downBox = touchDownBoxIndex
                touchDownBoxIndex = -1

                when (mode) {
                    OcrMode.FRAME -> {
                        // 框选模式：点击单个 box 切换选中
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
                    OcrMode.LINE -> {
                        // 逐行模式：点击行区域切换选中
                        val lineIdx = findLineAtPoint(event.x, event.y)
                        if (lineIdx >= 0) {
                            if (selectedLineIndex == lineIdx) {
                                // 取消选中
                                selectedLineIndex = -1
                                selectedIndices = emptySet()
                                onLineSelected?.invoke(-1, "")
                            } else {
                                selectedLineIndex = lineIdx
                                val lineBoxSet = mutableSetOf<Int>()
                                if (lineIdx < lineIndices.size) {
                                    lineIndices[lineIdx].forEach { lineBoxSet.add(it) }
                                }
                                selectedIndices = lineBoxSet
                                val lineText = if (lineIdx < allLines.size) allLines[lineIdx] else ""
                                onLineSelected?.invoke(lineIdx, lineText)
                            }
                            invalidate()
                            return true
                        }
                    }
                    OcrMode.WORD -> {
                        // 分词模式：图片上也支持点击选 box
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
                invalidate()
            }
        }
        return false
    }

    /**
     * 测试触摸点是否在某个 box 的四边形内（凸多边形 hit test）
     */
    private fun findBoxAtPoint(tx: Float, ty: Float): Int {
        val mat = imageMatrix ?: return -1

        // 从后往前检测，后绘制的在上面
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
     * 判断点 (px,py) 是否在凸多边形内（4 个顶点，顺时针或逆时针）
     * 使用 cross product 符号一致性检测
     */
    private fun isPointInPolygon(px: Float, py: Float, pts: FloatArray): Boolean {
        // pts: [x0,y0, x1,y1, x2,y2, x3,y3]
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
            OcrMode.LINE, OcrMode.FRAME -> {
                drawLineMode(canvas, mat)
            }
            OcrMode.WORD -> {
                drawWordMode(canvas, mat)
            }
        }
    }

    /**
     * LINE / FRAME 模式：绘制行区域 + 单个 box
     */
    private fun drawLineMode(canvas: Canvas, mat: Matrix) {
        // 1. 先绘制行区域矩形
        for ((lineIdx, rect) in lineRects.withIndex()) {
            val paint = if (lineIdx == selectedLineIndex) selectedLineStrokePaint else lineStrokePaint
            canvas.drawRect(rect, paint)
            if (lineIdx == selectedLineIndex) {
                canvas.drawRect(rect, selectedLineFillPaint)
            }
        }

        // 2. 绘制所有 box
        for ((index, box) in wordBoxes.withIndex()) {
            val pts = transformBox(box, mat)
            val path = boxToPath(pts)

            // 选中的 box 加填充
            if (index in selectedIndices) {
                canvas.drawPath(path, selectedBoxPaint)
            }
            canvas.drawPath(path, boxPaint)

            // 按压反馈
            if (index == touchDownBoxIndex) {
                canvas.drawPath(path, touchFeedbackPaint)
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
