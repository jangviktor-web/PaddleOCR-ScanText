package com.equationl.paddleocr4android.app

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class MoreActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_more)

        findViewById<ImageView>(R.id.btn_back_more).setOnClickListener { finish() }
        findViewById<android.view.View>(R.id.btn_history).setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
        findViewById<android.view.View>(R.id.btn_about).setOnClickListener {
            AboutDialog(this).show()
        }
    }
}
