package com.notes.app

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val textView = TextView(this)
        textView.text = "Notes App v0.1.1"
        textView.textSize = 24f
        textView.setPadding(32, 32, 32, 32)
        setContentView(textView)
    }
}
