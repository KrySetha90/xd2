package com.filexd.manager

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.Spannable
import android.text.TextWatcher
import android.text.style.BackgroundColorSpan
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class EditorActivity : AppCompatActivity() {

    private lateinit var targetFile: File
    private lateinit var contentEdit: EditText
    private lateinit var searchEdit: EditText
    private var lastSearchIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val path = intent.getStringExtra("file_path") ?: run {
            finish()
            return
        }
        targetFile = File(path)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
        }

        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(12, 12, 12, 12)
        }

        val backBtn = TextView(this).apply {
            text = "◀"
            textSize = 26f
            setTextColor(Color.BLACK)
            setPadding(16, 8, 20, 8)
            setOnClickListener { finish() }
        }

        val searchBox = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                setColor(0xFFB888FF.toInt())
                cornerRadius = 24f
            }
            setPadding(24, 10, 24, 10)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.3f).apply {
                marginEnd = 12
            }
        }

        searchEdit = EditText(this).apply {
            hint = "search"
            setHintTextColor(0xFF222222.toInt())
            setTextColor(Color.BLACK)
            textSize = 17f
            setTypeface(null, Typeface.BOLD)
            setBackgroundColor(Color.TRANSPARENT)
            isSingleLine = true
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val nextMatchBtn = TextView(this).apply {
            text = "🔍"
            textSize = 18f
            setPadding(12, 4, 4, 4)
            setOnClickListener { findNextInText() }
        }
        searchBox.addView(searchEdit)
        searchBox.addView(nextMatchBtn)

        val saveBtn = TextView(this).apply {
            text = "Save"
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(0xFF67EFEA.toInt())
                cornerRadius = 24f
            }
            setPadding(32, 22, 32, 22)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.7f)
            setOnClickListener { saveFile() }
        }

        topBar.addView(backBtn)
        topBar.addView(searchBox)
        topBar.addView(saveBtn)
        root.addView(topBar)

        val editorCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(0xFFC7C0D8.toInt())
                cornerRadii = floatArrayOf(28f, 28f, 28f, 28f, 0f, 0f, 0f, 0f)
            }
            setPadding(28, 24, 28, 24)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            ).apply {
                setMargins(28, 8, 0, 0)
            }
        }

        val fileTitle = TextView(this).apply {
            text = targetFile.name
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.BLACK)
            setPadding(4, 0, 4, 16)
        }
        editorCard.addView(fileTitle)

        val divider = TextView(this).apply {
            text = "- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -"
            setTextColor(Color.BLACK)
            isSingleLine = true
            setPadding(0, 0, 0, 12)
        }
        editorCard.addView(divider)

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }

        contentEdit = EditText(this).apply {
            hint = "Start typing..."
            setHintTextColor(0xFF6B6678.toInt())
            setTextColor(Color.BLACK)
            textSize = StorageHelper.getTextSize(this@EditorActivity)
            typeface = Typeface.MONOSPACE
            gravity = Gravity.TOP or Gravity.START
            setBackgroundColor(Color.TRANSPARENT)
            if (targetFile.exists()) {
                setText(targetFile.readText())
            }
        }

        searchEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                lastSearchIndex = 0
                highlightMatches(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        scroll.addView(contentEdit)
        editorCard.addView(scroll)
        root.addView(editorCard)

        setContentView(root)
    }

    private fun highlightMatches(query: String) {
        val text = contentEdit.text ?: return
        val spans = text.getSpans(0, text.length, BackgroundColorSpan::class.java)
        for (span in spans) text.removeSpan(span)

        if (query.isEmpty()) return
        val full = text.toString().lowercase()
        val q = query.lowercase()
        var idx = full.indexOf(q, 0)
        while (idx >= 0) {
            text.setSpan(
                BackgroundColorSpan(0xFFFFEB3B.toInt()),
                idx,
                idx + q.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            idx = full.indexOf(q, idx + q.length)
        }
    }

    private fun findNextInText() {
        val query = searchEdit.text.toString()
        if (query.isEmpty()) return
        val full = contentEdit.text.toString().lowercase()
        var idx = full.indexOf(query.lowercase(), lastSearchIndex)
        if (idx < 0) idx = full.indexOf(query.lowercase(), 0)
        if (idx >= 0) {
            contentEdit.requestFocus()
            contentEdit.setSelection(idx, idx + query.length)
            lastSearchIndex = idx + query.length
        } else {
            Toast.makeText(this, "No matches found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveFile() {
        try {
            targetFile.parentFile?.mkdirs()
            targetFile.writeText(contentEdit.text.toString())
            Toast.makeText(this, "Saved ${targetFile.name}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Save failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
