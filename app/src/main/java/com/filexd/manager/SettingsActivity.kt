package com.filexd.manager

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    private lateinit var customDrawerOverlay: FrameLayout
    private var selectedColor: Int = Color.WHITE

    private val paletteColors = intArrayOf(
        0xFFFF4D4D.toInt(), 0xFFF5B041.toInt(), 0xFFFFEB57.toInt(), 0xFF19D837.toInt(), 0xFF1CE8E8.toInt(), 0xFF3E64FF.toInt(), 0xFFD946EF.toInt(),
        0xFFFFFFFF.toInt(), 0xFFC0C0C0.toInt(), 0xFF888888.toInt(), 0xFF4A4A4A.toInt(), 0xFF0A0A0A.toInt(), 0xFF60C5E8.toInt(), 0xFFA2D9A2.toInt(),
        0xFF00FF19.toInt(), 0xFF5D6675.toInt(), 0xFF9AC5E8.toInt(), 0xFFB888FF.toInt(), 0xFF15C935.toInt(), 0xFF42526E.toInt(), 0xFFC5C5D2.toInt()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedColor = StorageHelper.getBgColor(this)

        val rootFrame = FrameLayout(this).apply {
            setBackgroundColor(Color.WHITE)
        }

        val mainContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 16, 24, 24)
        }

        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                setColor(0xFFC9BBE0.toInt())
                cornerRadius = 24f
            }
            setPadding(16, 12, 16, 12)
        }

        val backBtn = TextView(this).apply {
            text = "◀"
            textSize = 22f
            setTextColor(Color.BLACK)
            setPadding(12, 8, 24, 8)
            setOnClickListener { finish() }
        }

        val titlePill = TextView(this).apply {
            text = "" settings ""
            textSize = 18f
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = 20f
            }
            setPadding(36, 16, 36, 16)
        }

        topBar.addView(backBtn)
        topBar.addView(titlePill)
        mainContainer.addView(topBar)

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(0xFFC8AEE8.toInt())
                cornerRadius = 28f
            }
            setPadding(24, 24, 24, 24)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            ).apply {
                topMargin = 16
            }
        }

        panel.addView(createMenuCard("FILE TYPE", "Configure .txt, .xml, .zip, .html associations") {
            AlertDialog.Builder(this)
                .setTitle("Supported File Types")
                .setMessage("• Web & Archives: .zip, .html, .htm (Sandbox Runner)\n• Text & Code: .txt, .xml, .json, .js, .py, .kt, .ini (Built-in Editor)\n• Media & APK: Open via Android Intent")
                .setPositiveButton("OK", null)
                .show()
        })

        panel.addView(createMenuCard("CUSTOM", "Background color, text size, sort order") {
            customDrawerOverlay.visibility = View.VISIBLE
        })

        panel.addView(createMenuCard("ROOT ACCESS", "Shizuku / SUI") {
            checkShizukuStatus()
        })

        panel.addView(createMenuCard("REPORT THE PROBLEM", "Send feedback or bug details") {
            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_SUBJECT, "File_XD Bug Report")
            }
            try {
                startActivity(emailIntent)
            } catch (e: Exception) {
                Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show()
            }
        })

        panel.addView(createMenuCard("permission", "Manage All Files Access (PE:/ & E:/)") {
            StorageHelper.requestAllFilesPermission(this)
        })

        mainContainer.addView(panel)
        rootFrame.addView(mainContainer)

        customDrawerOverlay = buildCustomOverlay()
        customDrawerOverlay.visibility = View.GONE
        rootFrame.addView(customDrawerOverlay)

        setContentView(rootFrame)
    }

    private fun createMenuCard(title: String, subtitle: String?, onClick: () -> Unit): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.BLACK)
                cornerRadius = 24f
            }
            setPadding(32, 26, 32, 26)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16
            }

            addView(TextView(this@SettingsActivity).apply {
                text = title
                textSize = 18f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.WHITE)
            })

            if (!subtitle.isNullOrEmpty()) {
                addView(TextView(this@SettingsActivity).apply {
                    text = subtitle
                    textSize = 14f
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(0xFFB0B0B0.toInt())
                    setPadding(0, 6, 0, 0)
                })
            }

            setOnClickListener { onClick() }
        }
    }

    private fun buildCustomOverlay(): FrameLayout {
        val overlay = FrameLayout(this).apply {
            setBackgroundColor(0x66000000)
            setOnClickListener { visibility = View.GONE }
        }

        val sheet = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(0xFFD0B8F0.toInt())
                cornerRadius = 28f
                setStroke(4, 0xFF8E7AA8.toInt())
            }
            setPadding(28, 24, 28, 28)
            isClickable = true
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM or Gravity.END
            ).apply {
                setMargins(120, 80, 24, 48)
            }
        }

        val closeBar = View(this).apply {
            background = GradientDrawable().apply {
                setColor(Color.BLACK)
                cornerRadius = 20f
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 24).apply {
                bottomMargin = 12
            }
            setOnClickListener { overlay.visibility = View.GONE }
        }
        sheet.addView(closeBar)

        sheet.addView(TextView(this).apply {
            text = "Custom"
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        })

        sheet.addView(TextView(this).apply {
            text = "background color"
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
            setPadding(0, 12, 0, 12)
        })

        val hexLabel = TextView(this).apply {
            text = String.format("#%06X (100%%)", 0xFFFFFF and selectedColor)
            textSize = 16f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.BLACK)
            gravity = Gravity.CENTER
            setPadding(12, 12, 12, 12)
        }
        sheet.addView(hexLabel)

        val grid = GridLayout(this).apply {
            columnCount = 7
            rowCount = 3
            setBackgroundColor(Color.BLACK)
            setPadding(8, 8, 8, 8)
        }

        for (color in paletteColors) {
            val swatch = View(this).apply {
                background = GradientDrawable().apply {
                    setColor(color)
                    cornerRadius = 12f
                }
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 64
                    height = 64
                    setMargins(6, 6, 6, 6)
                }
                setOnClickListener {
                    selectedColor = color
                    hexLabel.text = String.format("#%06X (100%%)", 0xFFFFFF and color)
                }
            }
            grid.addView(swatch)
        }
        sheet.addView(grid)

        val applyBtn = TextView(this).apply {
            text = "apply"
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.BLACK)
            setPadding(8, 16, 8, 12)
            setOnClickListener {
                StorageHelper.setBgColor(this@SettingsActivity, selectedColor)
                Toast.makeText(this@SettingsActivity, "Background color applied", Toast.LENGTH_SHORT).show()
                overlay.visibility = View.GONE
            }
        }
        sheet.addView(applyBtn)

        val currentTextSize = StorageHelper.getTextSize(this)
        val textSizeLabel = TextView(this).apply {
            text = "Text size (${currentTextSize.toInt()}sp)"
            textSize = 17f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.BLACK)
            setPadding(8, 8, 8, 4)
        }
        sheet.addView(textSizeLabel)

        val seekBar = SeekBar(this).apply {
            max = 16
            progress = (currentTextSize.toInt() - 12).coerceIn(0, 16)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                    val sp = 12 + progress
                    textSizeLabel.text = "Text size (${sp}sp)"
                    StorageHelper.setTextSize(this@SettingsActivity, sp.toFloat())
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }
        sheet.addView(seekBar)

        val sortOptions = arrayOf("Name", "Date modified", "Size", "Type")
        val sortLabel = TextView(this).apply {
            text = "Sort by\n  ${sortOptions[StorageHelper.getSortBy(this@SettingsActivity)]} >"
            textSize = 17f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.BLACK)
            setPadding(8, 16, 8, 8)
            setOnClickListener {
                AlertDialog.Builder(this@SettingsActivity)
                    .setTitle("Sort files by")
                    .setItems(sortOptions) { _, which ->
                        StorageHelper.setSortBy(this@SettingsActivity, which)
                        text = "Sort by\n  ${sortOptions[which]} >"
                    }
                    .show()
            }
        }
        sheet.addView(sortLabel)

        overlay.addView(sheet)
        return overlay
    }

    private fun checkShizukuStatus() {
        val installed = try {
            packageManager.getPackageInfo("moe.shizuku.privileged.api", 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
        val msg = if (installed) {
            "Shizuku service package detected! Ready for privileged operations."
        } else {
            "Shizuku / SUI is not installed on this device. Standard & All-Files storage modes are active."
        }
        AlertDialog.Builder(this)
            .setTitle("ROOT ACCESS (Shizuku / SUI)")
            .setMessage(msg)
            .setPositiveButton("OK", null)
            .show()
    }
}
