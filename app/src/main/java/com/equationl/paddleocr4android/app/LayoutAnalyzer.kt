package com.equationl.paddleocr4android.app

import android.graphics.RectF
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object LayoutAnalyzer {

    data class TextBlock(
        val boxIndex: Int,
        val word: String,
        val x1: Float, val y1: Float, val x2: Float, val y2: Float,
        val charHeight: Float
    ) {
        val cx get() = (x1 + x2) / 2f
        val cy get() = (y1 + y2) / 2f
        val width get() = x2 - x1
        val height get() = y2 - y1
    }

    fun analyze(
        boxes: List<FloatArray>,
        words: List<String>,
        lines: List<String>,
        lineGroups: List<List<Int>>
    ): List<LayoutRegion> {
        if (boxes.isEmpty()) return emptyList()

        val blocks = buildBlocks(boxes, words)
        if (blocks.isEmpty()) return emptyList()

        val avgCharHeight = blocks.map { it.charHeight }.average().toFloat()
        val paragraphs = groupParagraphs(blocks, lineGroups)

        val regions = mutableListOf<LayoutRegion>()

        for (para in paragraphs) {
            val type = classifyParagraph(para, avgCharHeight, blocks.size)
            val lineTexts = para.map { it.word }
            val bbox = computeBbox(para)
            regions.add(LayoutRegion(
                type = type,
                boxIndices = para.map { it.boxIndex },
                bbox = bbox,
                lines = lineTexts,
                confidence = 0.8f
            ))
        }

        return mergeAdjacentTypes(regions)
    }

    private fun buildBlocks(boxes: List<FloatArray>, words: List<String>): List<TextBlock> {
        val blocks = mutableListOf<TextBlock>()
        for (i in boxes.indices) {
            if (i >= words.size) break
            val b = boxes[i]
            val w = words[i]
            if (w.isBlank()) continue
            val xs = floatArrayOf(b[0], b[2], b[4], b[6])
            val ys = floatArrayOf(b[1], b[3], b[5], b[7])
            val x1 = xs.min(); val x2 = xs.max()
            val y1 = ys.min(); val y2 = ys.max()
            val charH = (y2 - y1)
            blocks.add(TextBlock(i, w, x1, y1, x2, y2, charH))
        }
        return blocks
    }

    private fun groupParagraphs(
        blocks: List<TextBlock>,
        lineGroups: List<List<Int>>
    ): List<List<TextBlock>> {
        if (lineGroups.isEmpty()) return listOf(blocks)

        val blockByIndex = blocks.associateBy { it.boxIndex }
        val lineBlocks = lineGroups.mapNotNull { grp ->
            grp.mapNotNull { blockByIndex[it] }.takeIf { it.isNotEmpty() }
        }
        if (lineBlocks.isEmpty()) return listOf(blocks)

        val paragraphs = mutableListOf<MutableList<TextBlock>>()
        var current = lineBlocks[0].toMutableList()

        for (i in 1 until lineBlocks.size) {
            val prevLine = lineBlocks[i - 1]
            val thisLine = lineBlocks[i]
            val prevY = prevLine.map { it.cy }.average().toFloat()
            val thisY = thisLine.map { it.cy }.average().toFloat()
            val gap = thisY - prevY
            val avgH = (prevLine + thisLine).map { it.height }.average().toFloat()
            val leftAligned = isLeftAligned(prevLine, thisLine)

            if (gap < avgH * 1.8f && leftAligned) {
                current.addAll(thisLine)
            } else {
                paragraphs.add(current)
                current = thisLine.toMutableList()
            }
        }
        paragraphs.add(current)
        return paragraphs
    }

    private fun isLeftAligned(lineA: List<TextBlock>, lineB: List<TextBlock>): Boolean {
        val leftA = lineA.minOfOrNull { it.x1 } ?: return false
        val leftB = lineB.minOfOrNull { it.x1 } ?: return false
        val avgWidth = (lineA + lineB).map { it.width }.average().toFloat()
        return abs(leftA - leftB) < avgWidth * 0.15f
    }

    private fun classifyParagraph(
        para: List<TextBlock>,
        avgCharHeight: Float,
        totalBlocks: Int
    ): LayoutType {
        if (para.isEmpty()) return LayoutType.TEXT

        val maxH = para.maxOfOrNull { it.charHeight } ?: 0f
        val avgH = para.map { it.charHeight }.average().toFloat()
        val totalWidth = para.sumOf { it.width.toDouble() }.toFloat()

        if (para.size == 1 && maxH > avgCharHeight * 1.4f) {
            return LayoutType.TITLE
        }

        if (para.size <= 3 && avgH > avgCharHeight * 1.3f && totalWidth < 500f) {
            return LayoutType.TITLE
        }

        if (para.size >= 2) {
            val rows = groupByY(para)
            if (rows.size >= 2) {
                val colsPerRow = rows.map { it.size }
                val allSimilarCols = colsPerRow.all { abs(it - colsPerRow[0]) <= 1 }
                val allSimilarWidth = rows.all { row ->
                    row.all { abs(it.width - row[0].width) < row[0].width * 0.3f }
                }
                if (allSimilarCols && allSimilarWidth && colsPerRow[0] >= 2) {
                    return LayoutType.TABLE
                }
            }
        }

        if (para.size == 1 && para[0].word.trimStart().let {
            it.startsWith("•") || it.startsWith("·") || it.startsWith("·") ||
            it.startsWith("-") || it.startsWith("●") || it.startsWith("▪") ||
            it.matches(Regex("^\\d+[.)].*")) || it.matches(Regex("^[①②③④⑤⑥⑦⑧⑨⑩].*"))
        }) {
            return LayoutType.LIST
        }

        return LayoutType.TEXT
    }

    private fun groupByY(blocks: List<TextBlock>): List<List<TextBlock>> {
        val sorted = blocks.sortedBy { it.cy }
        val rows = mutableListOf<MutableList<TextBlock>>()
        var currentRow = mutableListOf(sorted[0])
        var currentY = sorted[0].cy

        for (i in 1 until sorted.size) {
            if (abs(sorted[i].cy - currentY) < sorted[i].height * 0.5f) {
                currentRow.add(sorted[i])
            } else {
                rows.add(currentRow.sortedBy { it.x1 }.toMutableList())
                currentRow = mutableListOf(sorted[i])
                currentY = sorted[i].cy
            }
        }
        rows.add(currentRow.sortedBy { it.x1 }.toMutableList())
        return rows
    }

    private fun computeBbox(para: List<TextBlock>): RectF {
        val x1 = para.minOfOrNull { it.x1 } ?: 0f
        val y1 = para.minOfOrNull { it.y1 } ?: 0f
        val x2 = para.maxOfOrNull { it.x2 } ?: 0f
        val y2 = para.maxOfOrNull { it.y2 } ?: 0f
        return RectF(x1, y1, x2, y2)
    }

    private fun mergeAdjacentTypes(regions: List<LayoutRegion>): List<LayoutRegion> {
        if (regions.size <= 1) return regions
        val merged = mutableListOf<LayoutRegion>()
        var current = regions[0]

        for (i in 1 until regions.size) {
            val next = regions[i]
            if (current.type == next.type && current.type != LayoutType.TABLE && current.type != LayoutType.IMAGE) {
                val gap = next.bbox.top - current.bbox.bottom
                val avgH = (current.bbox.height() + next.bbox.height()) / 2f
                if (gap < avgH * 0.5f) {
                    current = LayoutRegion(
                        type = current.type,
                        boxIndices = current.boxIndices + next.boxIndices,
                        bbox = RectF(
                            min(current.bbox.left, next.bbox.left),
                            current.bbox.top,
                            max(current.bbox.right, next.bbox.right),
                            next.bbox.bottom
                        ),
                        lines = current.lines + next.lines,
                        confidence = max(current.confidence, next.confidence)
                    )
                    continue
                }
            }
            merged.add(current)
            current = next
        }
        merged.add(current)
        return merged
    }
}
