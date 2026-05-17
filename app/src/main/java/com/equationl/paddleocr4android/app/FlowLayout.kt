package com.equationl.paddleocr4android.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.max
import kotlin.math.min

/**
 * 流式词块布局：按行分组排列词块，自管理滚动，支持：
 * - 点按 toggle 单个词
 * - 拖拽连续选词（松手确认）
 * - 滚动浏览长文本
 *
 * 不依赖 ScrollView，自行处理滚动和选词，彻底避免 touch 事件冲突。
 */
class FlowLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : View(context, attrs, defStyle) {

    data class WordBlock(
        val text: String,
        val wordIndex: Int,
        val lineIndex: Int,
        val positionInLine: Int,
        var left: Int = 0, var top: Int = 0,
        var right: Int = 0, var bottom: Int = 0
    )

    // 词块数据（按行序 → 行内位置排序）
    private var wordBlocks: List<WordBlock> = emptyList()
    private var originalWordInfos: List<WordInfo> = emptyList()

    // 选中状态
    private val selectedPositions = mutableSetOf<Int>()

    // 滚动状态
    private var scrollY = 0
    private var maxScrollY = 0
    private var lastScrollY = 0
    private var velocityY = 0f

    // 拖拽选词状态
    private var isDraggingSelection = false
    private var anchorWordIndex = -1
    private var dragStartX = 0f
    private var dragStartY = 0f
    private var lastSelectedRange: IntRange? = null

    // 惯性滚动
    private var flingActive = false
    private var flingTimestamp = 0L

    // 回调
    var onSelectionChanged: ((selectedInfos: List<WordInfo>) -> Unit)? = null

    // 画笔
    private val normalBgPaint = Paint().apply {
        color = Color.parseColor("#F6F7F9"); style = Paint.Style.FILL
    }
    private val normalBorderPaint = Paint().apply {
        color = Color.parseColor("#E5E6EB"); style = Paint.Style.STROKE; strokeWidth = 1.5f
    }
    private val selectedBgPaint = Paint().apply {
        color = Color.parseColor("#165DFF"); style = Paint.Style.FILL
    }
    private val selectedBorderPaint = Paint().apply {
        color = Color.parseColor("#0E42D2"); style = Paint.Style.STROKE; strokeWidth = 2f
    }
    private val dragBgPaint = Paint().apply {
        color = Color.parseColor("#33165DFF"); style = Paint.Style.FILL
    }
    private val dragBorderPaint = Paint().apply {
        color = Color.parseColor("#66165DFF"); style = Paint.Style.STROKE; strokeWidth = 1.5f
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1D2129"); textSize = 40f
    }
    private val selectedTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textSize = 40f
    }

    // 布局参数
    private val horizontalSpacing = 12
    private val lineSpacing = 28
    private val blockPaddingH = 24
    private val blockPaddingV = 14
    private val blockCornerRadius = 10f
    private val touchSlop = 20f

    // ======================== 公开 API ========================

    fun setWordInfos(infos: List<WordInfo>) {
        originalWordInfos = infos
        wordBlocks = infos.mapIndexed { i, info ->
            WordBlock(info.text, i, info.lineIndex, info.positionInLine)
        }.sortedWith(compareBy({ it.lineIndex }, { it.positionInLine }))
        selectedPositions.clear()
        lastSelectedRange = null
        scrollY = 0
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
        return selectedPositions
            .map { wordBlocks[it] }
            .sortedWith(compareBy({ it.lineIndex }, { it.positionInLine }))
            .joinToString("") { it.text }
    }

    fun getSelectedWordInfos(): List<WordInfo> {
        return selectedPositions
            .map { wordBlocks[it] }
            .sortedWith(compareBy({ it.lineIndex }, { it.positionInLine }))
            .map { b ->
                if (b.wordIndex < originalWordInfos.size) originalWordInfos[b.wordIndex]
                else WordInfo(b.text, listOf(b.wordIndex), b.lineIndex, b.positionInLine)
            }
    }

    // ======================== 布局 ========================

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val parentHeight = MeasureSpec.getSize(heightMeasureSpec)

        var x = 0; var y = 0; var rowHeight = 0; var lastLineIndex = -1
        val lineHeight = (textPaint.fontMetrics.descent - textPaint.fontMetrics.ascent).toInt() + blockPaddingV * 2

        for (block in wordBlocks) {
            if (lastLineIndex >= 0 && block.lineIndex != lastLineIndex) {
                x = 0; y += rowHeight + lineSpacing; rowHeight = 0
            }
            lastLineIndex = block.lineIndex

            val bw = textPaint.measureText(block.text).toInt() + blockPaddingH * 2
            if (x + bw > widthSize && x > 0) {
                x = 0; y += rowHeight + lineSpacing; rowHeight = 0
            }
            block.left = x; block.top = y; block.right = x + bw; block.bottom = y + lineHeight
            x += bw + horizontalSpacing
            rowHeight = max(rowHeight, lineHeight)
        }

        val contentHeight = y + rowHeight
        maxScrollY = max(0, contentHeight - parentHeight)
        scrollY = scrollY.coerceIn(0, maxScrollY)

        setMeasuredDimension(widthSize, parentHeight)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {}

    // ======================== 触摸 ========================

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (wordBlocks.isEmpty()) return false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                flingActive = false
                dragStartX = event.x
                dragStartY = event.y
                lastScrollY = scrollY.toInt()
                velocityY = 0f
                isDraggingSelection = false
                anchorWordIndex = findWordAt(event.x, event.y)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - dragStartX
                val dy = event.y - dragStartY

                if (!isDraggingSelection) {
                    // 判断是滚动还是选词拖拽
                    if (dy * dy + dx * dx > touchSlop * touchSlop) {
                        if (Math.abs(dy) > Math.abs(dx) * 1.5f) {
                            // 垂直拖拽 → 滚动
                            isDraggingSelection = false
                        } else {
                            // 水平拖拽 或 在词块上拖拽 → 选词
                            isDraggingSelection = true
                            if (anchorWordIndex >= 0) {
                                lastSelectedRange = anchorWordIndex..anchorWordIndex
                            }
                        }
                    }
                }

                if (isDraggingSelection) {
                    // 拖拽选词
                    val curIdx = findWordAt(event.x, event.y)
                    if (curIdx >= 0 && anchorWordIndex >= 0) {
                        val range = min(anchorWordIndex, curIdx)..max(anchorWordIndex, curIdx)
                        if (range != lastSelectedRange) {
                            lastSelectedRange = range
                            invalidate()
                        }
                    }
                } else {
                    // 滚动
                    val delta = (lastScrollY - dy.toInt()).coerceIn(0, maxScrollY)
                    if (delta != scrollY) {
                        scrollY = delta
                        velocityY = dy
                        invalidate()
                    }
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDraggingSelection && lastSelectedRange != null) {
                    // 拖拽选词完成
                    selectedPositions.clear()
                    for (i in lastSelectedRange!!) selectedPositions.add(i)
                    lastSelectedRange = null
                    notifySelectionChanged()
                } else if (!isDraggingSelection && anchorWordIndex >= 0) {
                    // 点击 toggle
                    val dy = Math.abs(event.y - dragStartY)
                    val dx = Math.abs(event.x - dragStartX)
                    if (dy < touchSlop && dx < touchSlop) {
                        if (anchorWordIndex in selectedPositions) selectedPositions.remove(anchorWordIndex)
                        else selectedPositions.add(anchorWordIndex)
                        notifySelectionChanged()
                    } else {
                        // 惯性滚动
                        flingActive = true
                        flingTimestamp = System.currentTimeMillis()
                    }
                }

                isDraggingSelection = false
                anchorWordIndex = -1
                invalidate()
                return true
            }
        }
        return false
    }

    private fun findWordAt(x: Float, y: Float): Int {
        val adjustedY = y + scrollY
        for (i in wordBlocks.indices.reversed()) {
            val b = wordBlocks[i]
            if (x >= b.left && x <= b.right && adjustedY >= b.top && adjustedY <= b.bottom) {
                return i
            }
        }
        return -1
    }

    // ======================== 绘制 ========================

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (wordBlocks.isEmpty()) return

        // 处理惯性滚动
        if (flingActive) {
            val elapsed = System.currentTimeMillis() - flingTimestamp
            if (elapsed < 300) {
                val decay = 1f - elapsed / 300f
                val newScroll = (scrollY - velocityY * decay * 0.3f).toInt().coerceIn(0, maxScrollY)
                if (newScroll != scrollY) {
                    scrollY = newScroll
                    postInvalidateOnAnimation()
                }
            } else {
                flingActive = false
            }
        }

        canvas.save()
        canvas.translate(0f, -scrollY.toFloat())

        val metrics = textPaint.fontMetrics
        val textYOffset = (blockPaddingV - metrics.ascent)
        val viewHeight = height

        for ((i, block) in wordBlocks.withIndex()) {
            // 跳过不可见的词块
            if (block.bottom - scrollY < 0 || block.top - scrollY > viewHeight) continue

            val rect = RectF(
                block.left.toFloat(), block.top.toFloat(),
                block.right.toFloat(), block.bottom.toFloat()
            )

            val inDragRange = lastSelectedRange != null && i in lastSelectedRange!!
            val isSelected = i in selectedPositions

            when {
                isSelected -> {
                    canvas.drawRoundRect(rect, blockCornerRadius, blockCornerRadius, selectedBgPaint)
                    canvas.drawRoundRect(rect, blockCornerRadius, blockCornerRadius, selectedBorderPaint)
                }
                inDragRange -> {
                    canvas.drawRoundRect(rect, blockCornerRadius, blockCornerRadius, dragBgPaint)
                    canvas.drawRoundRect(rect, blockCornerRadius, blockCornerRadius, dragBorderPaint)
                }
                else -> {
                    canvas.drawRoundRect(rect, blockCornerRadius, blockCornerRadius, normalBgPaint)
                    canvas.drawRoundRect(rect, blockCornerRadius, blockCornerRadius, normalBorderPaint)
                }
            }

            val paint = if (isSelected) selectedTextPaint else textPaint
            canvas.drawText(block.text, block.left + blockPaddingH.toFloat(), block.top + textYOffset, paint)
        }

        canvas.restore()
    }

    private fun notifySelectionChanged() {
        val infos = selectedPositions
            .map { wordBlocks[it] }
            .sortedWith(compareBy({ it.lineIndex }, { it.positionInLine }))
            .map { b ->
                if (b.wordIndex < originalWordInfos.size) originalWordInfos[b.wordIndex]
                else WordInfo(b.text, listOf(b.wordIndex), b.lineIndex, b.positionInLine)
            }
        onSelectionChanged?.invoke(infos)
    }
}
