package com.equationl.paddleocr4android.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * 流式词块布局：按语义分词结果流式排列，支持点选和拖拽连续选词。
 *
 * 视觉风格参考 WPS 手机版文字选词：
 * - 词块之间有细微间隙，保持阅读连贯性
 * - 选中词块高亮显示（浅蓝背景 + 深蓝边框）
 * - 拖拽时实时显示选区范围
 */
class FlowLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : View(context, attrs, defStyle) {

    data class WordBlock(
        val text: String,
        val wordIndex: Int,
        var left: Int = 0, var top: Int = 0,
        var right: Int = 0, var bottom: Int = 0
    ) {
        val width get() = right - left
        val height get() = bottom - top
        val cx get() = (left + right) / 2f
        val cy get() = (top + bottom) / 2f
    }

    // 词块数据
    private var wordBlocks: List<WordBlock> = emptyList()
    private var originalWordInfos: List<WordInfo> = emptyList()

    // 选中状态
    private val selectedPositions = mutableSetOf<Int>()

    // 拖拽状态
    private var isDragging = false
    private var anchorWordIndex = -1
    private var dragStartX = 0f
    private var dragStartY = 0f

    // 回调
    var onSelectionChanged: ((selectedInfos: List<WordInfo>) -> Unit)? = null

    // 画笔
    private val normalBgPaint = Paint().apply {
        color = Color.parseColor("#F6F7F9")
        style = Paint.Style.FILL
    }
    private val normalBorderPaint = Paint().apply {
        color = Color.parseColor("#E5E6EB")
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
    }
    private val selectedBgPaint = Paint().apply {
        color = Color.parseColor("#165DFF")
        style = Paint.Style.FILL
    }
    private val selectedBorderPaint = Paint().apply {
        color = Color.parseColor("#0E42D2")
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val dragBgPaint = Paint().apply {
        color = Color.parseColor("#33165DFF")
        style = Paint.Style.FILL
    }
    private val dragBorderPaint = Paint().apply {
        color = Color.parseColor("#66165DFF")
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1D2129")
        textSize = 40f
    }
    private val selectedTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 40f
    }

    // 布局参数
    private val horizontalSpacing = 12  // 词块间水平间距 px
    private val verticalSpacing = 16    // 词块间垂直间距 px
    private val blockPaddingH = 24      // 词块内水平 padding px
    private val blockPaddingV = 14      // 词块内垂直 padding px
    private val blockCornerRadius = 10f // 词块圆角半径 px
    private val minDragDistance = 20f   // 最小拖拽距离 px

    private var lastSelectedRange: IntRange? = null

    // ======================== 公开 API ========================

    fun setWordInfos(infos: List<WordInfo>) {
        originalWordInfos = infos
        wordBlocks = infos.mapIndexed { i, info ->
            WordBlock(text = info.text, wordIndex = i)
        }
        selectedPositions.clear()
        lastSelectedRange = null
        requestLayout()
        invalidate()
    }

    fun selectAll() {
        selectedPositions.clear()
        wordBlocks.indices.forEach { selectedPositions.add(it) }
        invalidate()
        notifySelectionChanged()
    }

    fun clearSelection() {
        selectedPositions.clear()
        lastSelectedRange = null
        invalidate()
        notifySelectionChanged()
    }

    fun getSelectedText(): String {
        return selectedPositions.sorted().joinToString("") { wordBlocks[it].text }
    }

    fun getSelectedWordInfos(): List<WordInfo> {
        return selectedPositions.sorted().map { i ->
            if (i < originalWordInfos.size) originalWordInfos[i]
            else WordInfo(wordBlocks[i].text, listOf(i))
        }
    }

    // ======================== 布局 ========================

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val textWidth = textPaint.measureText("中")

        var x = 0
        var y = 0
        var rowHeight = 0
        val lineHeight = (textPaint.fontMetrics.descent - textPaint.fontMetrics.ascent).toInt() + blockPaddingV * 2

        for (block in wordBlocks) {
            val tw = textPaint.measureText(block.text).toInt()
            val bw = tw + blockPaddingH * 2
            val bh = lineHeight

            if (x + bw > widthSize && x > 0) {
                x = 0
                y += rowHeight + verticalSpacing
                rowHeight = 0
            }

            block.left = x
            block.top = y
            block.right = x + bw
            block.bottom = y + bh

            x += bw + horizontalSpacing
            rowHeight = max(rowHeight, bh)
        }

        setMeasuredDimension(widthSize, y + rowHeight)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        // 布局在 onMeasure 中完成
    }

    // ======================== 触摸 ========================

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (wordBlocks.isEmpty()) return false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val idx = findWordAt(event.x, event.y)
                dragStartX = event.x
                dragStartY = event.y
                anchorWordIndex = idx
                isDragging = false
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (anchorWordIndex < 0) return false
                val dx = event.x - dragStartX
                val dy = event.y - dragStartY
                if (!isDragging && (dx * dx + dy * dy) > minDragDistance * minDragDistance) {
                    isDragging = true
                }
                if (isDragging) {
                    val curIdx = findWordAt(event.x, event.y)
                    if (curIdx >= 0) {
                        val range = min(anchorWordIndex, curIdx)..max(anchorWordIndex, curIdx)
                        if (range != lastSelectedRange) {
                            lastSelectedRange = range
                            invalidate()
                        }
                    }
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (event.action == MotionEvent.ACTION_CANCEL) {
                    isDragging = false
                    anchorWordIndex = -1
                    lastSelectedRange = null
                    invalidate()
                    return true
                }

                if (isDragging) {
                    // 拖拽完成：选中范围内所有词
                    val curIdx = findWordAt(event.x, event.y)
                    if (curIdx >= 0 && anchorWordIndex >= 0) {
                        val start = min(anchorWordIndex, curIdx)
                        val end = max(anchorWordIndex, curIdx)
                        selectedPositions.clear()
                        for (i in start..end) selectedPositions.add(i)
                    }
                } else {
                    // 点击：toggle 单个词
                    val idx = findWordAt(event.x, event.y)
                    if (idx >= 0) {
                        if (idx in selectedPositions) selectedPositions.remove(idx)
                        else selectedPositions.add(idx)
                    }
                }

                isDragging = false
                anchorWordIndex = -1
                lastSelectedRange = null
                invalidate()
                notifySelectionChanged()
                return true
            }
        }
        return false
    }

    private fun findWordAt(x: Float, y: Float): Int {
        for (i in wordBlocks.indices.reversed()) {
            val b = wordBlocks[i]
            if (x >= b.left && x <= b.right && y >= b.top && y <= b.bottom) {
                return i
            }
        }
        return -1
    }

    // ======================== 绘制 ========================

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (wordBlocks.isEmpty()) return

        val metrics = textPaint.fontMetrics
        val textYOffset = (blockPaddingV - metrics.ascent)

        for ((i, block) in wordBlocks.withIndex()) {
            val rect = RectF(
                block.left.toFloat(), block.top.toFloat(),
                block.right.toFloat(), block.bottom.toFloat()
            )

            when {
                i in selectedPositions -> {
                    canvas.drawRoundRect(rect, blockCornerRadius, blockCornerRadius, selectedBgPaint)
                    canvas.drawRoundRect(rect, blockCornerRadius, blockCornerRadius, selectedBorderPaint)
                }
                lastSelectedRange != null && i in lastSelectedRange!! -> {
                    canvas.drawRoundRect(rect, blockCornerRadius, blockCornerRadius, dragBgPaint)
                    canvas.drawRoundRect(rect, blockCornerRadius, blockCornerRadius, dragBorderPaint)
                }
                else -> {
                    canvas.drawRoundRect(rect, blockCornerRadius, blockCornerRadius, normalBgPaint)
                    canvas.drawRoundRect(rect, blockCornerRadius, blockCornerRadius, normalBorderPaint)
                }
            }

            val paint = if (i in selectedPositions) selectedTextPaint else textPaint
            canvas.drawText(
                block.text,
                block.left + blockPaddingH.toFloat(),
                block.top + textYOffset,
                paint
            )
        }
    }

    private fun notifySelectionChanged() {
        val infos = selectedPositions.sorted().map { i ->
            if (i < originalWordInfos.size) originalWordInfos[i]
            else WordInfo(wordBlocks[i].text, listOf(i))
        }
        onSelectionChanged?.invoke(infos)
    }
}
