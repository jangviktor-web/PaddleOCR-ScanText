package com.equationl.paddleocr4android.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * 分词选择适配器：每个词是一个可点击的 chip
 * 支持 WordInfo（结巴分词结果 + box 映射）
 */
class WordAdapter(
    private val onSelectionChanged: (Set<Int>) -> Unit
) : RecyclerView.Adapter<WordAdapter.WordViewHolder>() {

    private var wordInfos: List<WordInfo> = emptyList()
    private val selectedPositions = mutableSetOf<Int>()

    fun setWordInfos(newWords: List<WordInfo>) {
        wordInfos = newWords
        selectedPositions.clear()
        notifyDataSetChanged()
    }

    /** @deprecated 使用 setWordInfos 代替 */
    fun setWords(newWords: List<String>) {
        wordInfos = newWords.map { WordInfo(it, emptyList()) }
        selectedPositions.clear()
        notifyDataSetChanged()
    }

    fun selectAll() {
        selectedPositions.clear()
        wordInfos.indices.forEach { selectedPositions.add(it) }
        notifyDataSetChanged()
        onSelectionChanged(selectedPositions.toSet())
    }

    fun clearSelection() {
        selectedPositions.clear()
        notifyDataSetChanged()
        onSelectionChanged(emptySet())
    }

    fun getSelectedText(): String {
        return selectedPositions.sorted().joinToString("") { wordInfos[it].text }
    }

    fun getSelectedWordInfos(): List<WordInfo> {
        return selectedPositions.sorted().map { wordInfos[it] }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WordViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_word, parent, false)
        return WordViewHolder(view)
    }

    override fun onBindViewHolder(holder: WordViewHolder, position: Int) {
        val info = wordInfos[position]
        holder.tvWord.text = info.text

        val isSelected = position in selectedPositions
        holder.tvWord.setBackgroundResource(
            if (isSelected) R.drawable.bg_chip_selected else R.drawable.bg_chip_normal
        )
        holder.tvWord.setTextColor(
            if (isSelected) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#1A1C1E")
        )
        holder.tvWord.textSize = 16f

        holder.tvWord.setOnClickListener {
            if (isSelected) selectedPositions.remove(position)
            else selectedPositions.add(position)

            notifyItemChanged(position)
            onSelectionChanged(selectedPositions.toSet())
        }
    }

    override fun getItemCount() = wordInfos.size

    class WordViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvWord: TextView = view.findViewById(R.id.tv_word)
    }
}
