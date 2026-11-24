package com.filexd.manager

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class SandboxActivity : AppCompatActivity() {

    private lateinit var sourceFile: File
    private lateinit var webView: WebView
    private lateinit var runnerDetailsText: TextView
    private var sandboxRootDir: File? = null
    private var entryHtmlFile: File? = null
    private var extractedFiles: List<File> = emptyList()

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val path = intent.getStringExtra("file_path") ?: run {
            finish()
            return
        }
        sourceFile = File(path)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(24, 16, 24, 24)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, 12)
        }

        val backBtn = TextView(this).apply {
            text = "◀"
            textSize = 22f
            setTextColor(Color.BLACK)
            setPadding(8, 4, 20, 4)
            setOnClickListener { finish() }
        }

        val titleText = TextView(this).apply {
            text = "website tester / app viewer for test"
            textSize = 17f
            setTextColor(Color.BLACK)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val reloadBtn = TextView(this).apply {
            text = "↻"
            textSize = 22f
            setTextColor(Color.BLACK)
            setPadding(16, 4, 8, 4)
            setOnClickListener { prepareAndRunSandbox() }
        }

        header.addView(backBtn)
        header.addView(titleText)
        header.addView(reloadBtn)
        root.addView(header)

        val sandboxFrame = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(0xFF4A6394.toInt())
                cornerRadius = 32f
            }
            setPadding(12, 12, 12, 12)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }

        val sandboxBanner = TextView(this).apply {
            text = "sandbox"
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
            setPadding(0, 4, 0, 8)
        }
        sandboxFrame.addView(sandboxBanner)

        webView = WebView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            webViewClient = WebViewClient()
            webChromeClient = WebChromeClient()
        }
        sandboxFrame.addView(webView)
        root.addView(sandboxFrame)

        val bottomRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.BOTTOM
            setPadding(4, 16, 4, 4)
        }

        val detailsScroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, 260, 1f)
        }

        runnerDetailsText = TextView(this).apply {
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(0xFF6E6E6E.toInt())
        }
        detailsScroll.addView(runnerDetailsText)

        val editBtn = TextView(this).apply {
            text = "edit"
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(0xFFB384FF.toInt())
                cornerRadius = 24f
            }
            setPadding(40, 28, 40, 28)
            setOnClickListener { openSandboxFileInEditor() }
        }

        bottomRow.addView(detailsScroll)
        bottomRow.addView(editBtn)
        root.addView(bottomRow)

        setContentView(root)
        prepareAndRunSandbox()
    }

    override fun onResume() {
        super.onResume()
        entryHtmlFile?.let {
            if (it.exists()) {
                webView.loadUrl("file://${it.absolutePath}")
            }
        }
    }

    private fun prepareAndRunSandbox() {
        val sb = StringBuilder()

        if (sourceFile.extension.equals("zip", ignoreCase = true)) {
            val extractDir = File(cacheDir, "sandbox_${sourceFile.nameWithoutExtension}")
            extractDir.deleteRecursively()
            val reports = ZipHelper.unzipWithReport(sourceFile, extractDir)
            sandboxRootDir = extractDir

            val okCount = reports.count { it.ok }
            val pct = if (reports.isEmpty()) 100 else (okCount * 100) / reports.size
            sb.appendLine("file details runner : ${sourceFile.name} :$pct%")

            val folders = reports.filter { it.isDirectory }
            if (folders.isNotEmpty()) {
                sb.appendLine("+ folder: ${folders.first().path}")
            } else {
                sb.appendLine("+ folder: ${sourceFile.nameWithoutExtension}")
            }

            reports.filter { !it.isDirectory }.forEach { rep ->
                val shortName = rep.path.substringAfterLast('/')
                val status = if (rep.ok) "okay 100%" else "error 0%"
                sb.appendLine("  - $shortName   :$status")
            }

            extractedFiles = extractDir.walkTopDown().filter { it.isFile }.toList()
            entryHtmlFile = extractedFiles.firstOrNull { it.name.equals("index.html", true) }
                ?: extractedFiles.firstOrNull { it.extension.equals("html", true) || it.extension.equals("htm", true) }

        } else {
            sandboxRootDir = sourceFile.parentFile
            extractedFiles = listOf(sourceFile)
            entryHtmlFile = sourceFile
            sb.appendLine("file details runner : ${sourceFile.name} :100%")
            sb.appendLine("+ folder: ${sourceFile.parentFile?.name ?: "root"}")
            sb.appendLine("  - ${sourceFile.name}   :okay 100%")
        }

        runnerDetailsText.text = sb.toString().trimEnd()

        val target = entryHtmlFile
        if (target != null && target.exists()) {
            webView.loadUrl("file://${target.absolutePath}")
        } else {
            webView.loadData(
                "<html><body style='background:#4A6394;color:#fff;font-family:sans-serif;padding:20px;'>" +
                    "<h3>No index.html found in ${sourceFile.name}</h3>" +
                    "<p>Tap <b>edit</b> to inspect or edit the extracted files.</p></body></html>",
                "text/html",
                "UTF-8"
            )
        }
    }

    private fun openSandboxFileInEditor() {
        if (extractedFiles.size <= 1) {
            val fileToEdit = extractedFiles.firstOrNull() ?: sourceFile
            startActivity(Intent(this, EditorActivity::class.java).putExtra("file_path", fileToEdit.absolutePath))
            return
        }

        val names = extractedFiles.map { it.relativeTo(sandboxRootDir!!).path }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Select file to edit")
            .setItems(names) { _, which ->
                val selected = extractedFiles[which]
                startActivity(Intent(this, EditorActivity::class.java).putExtra("file_path", selected.absolutePath))
            }
            .show()
    }
}
