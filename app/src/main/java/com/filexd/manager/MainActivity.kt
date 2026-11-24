package com.filexd.manager

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.format.DateFormat
import android.view.Gravity
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ScrollView
import android.widget.SimpleAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.util.Date

class MainActivity : AppCompatActivity() {

    private var activeDrive = "C"
    private lateinit var currentDir: File
    private var displayedFiles: List<File> = emptyList()

    private lateinit var rootFrame: FrameLayout
    private lateinit var tabDriveC: TextView
    private lateinit var tabDriveE: TextView
    private lateinit var pathTextView: TextView
    private lateinit var fileListView: ListView
    private lateinit var pinDrawerOverlay: FrameLayout
    private lateinit var pinListContainer: LinearLayout

    private var clipboardFile: File? = null
    private var isCutOperation: Boolean = false
    private var undoAction: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAndRequestPermissions()
        currentDir = StorageHelper.getDriveCRoot()

        buildMainLayout()
        loadDirectory(currentDir)
    }

    override fun onResume() {
        super.onResume()
        rootFrame.setBackgroundColor(StorageHelper.getBgColor(this))
        if (::currentDir.isInitialized) {
            loadDirectory(currentDir)
        }
    }

    private fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!StorageHelper.hasAllFilesPermission()) {
                AlertDialog.Builder(this)
                    .setTitle("Storage Permission Needed")
                    .setMessage("File_XD needs All Files Access to manage folders in PE:/ and E:/.")
                    .setPositiveButton("Grant") { _, _ ->
                        StorageHelper.requestAllFilesPermission(this)
                    }
                    .setNegativeButton("Later", null)
                    .show()
            }
        } else {
            val perms = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            if (perms.any { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }) {
                ActivityCompat.requestPermissions(this, perms, 101)
            }
        }
    }

    private fun buildMainLayout() {
        rootFrame = FrameLayout(this).apply {
            setBackgroundColor(StorageHelper.getBgColor(this@MainActivity))
        }

        val mainColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 12, 16, 12)
        }

        val driveRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = GradientDrawable().apply {
                setColor(0xFFD9CEE8.toInt())
                cornerRadii = floatArrayOf(24f, 24f, 24f, 24f, 0f, 0f, 0f, 0f)
            }
            setPadding(12, 10, 12, 8)
        }

        tabDriveC = TextView(this).apply {
            text = "PE:/ file folder"
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
            setPadding(24, 18, 24, 18)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = 10
            }
            setOnClickListener {
                activeDrive = "C"
                updateDriveTabStyles()
                loadDirectory(StorageHelper.getDriveCRoot())
            }
        }

        tabDriveE = TextView(this).apply {
            text = "E:/app folder"
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
            setPadding(24, 18, 24, 18)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener {
                activeDrive = "E"
                updateDriveTabStyles()
                loadDirectory(StorageHelper.getDriveERoot(this@MainActivity))
            }
        }

        driveRow.addView(tabDriveC)
        driveRow.addView(tabDriveE)
        mainColumn.addView(driveRow)
        updateDriveTabStyles()

        val toolbarRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = GradientDrawable().apply {
                setColor(0xFFC9BBE0.toInt())
            }
            setPadding(12, 6, 12, 10)
        }

        val searchBtn = createTopPill("" search "") { showSearchDialog() }
        val settingsBtn = createTopPill("" settings "") {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        val pinBtn = createTopPill("" pin file "") {
            refreshPinDrawer()
            pinDrawerOverlay.visibility = View.VISIBLE
        }

        toolbarRow.addView(searchBtn)
        toolbarRow.addView(settingsBtn)
        toolbarRow.addView(pinBtn)
        mainColumn.addView(toolbarRow)

        pathTextView = TextView(this).apply {
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.BLACK)
            background = GradientDrawable().apply {
                setColor(0xFFD5CBE3.toInt())
                cornerRadius = 22f
            }
            setPadding(24, 16, 24, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 8
                bottomMargin = 8
            }
            setOnClickListener { navigateUp() }
        }
        mainColumn.addView(pathTextView)

        fileListView = ListView(this).apply {
            dividerHeight = 2
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            setOnItemClickListener { _, _, position, _ ->
                val clicked = displayedFiles[position]
                if (clicked.isDirectory) {
                    loadDirectory(clicked)
                } else {
                    openFileDefault(clicked)
                }
            }
            setOnItemLongClickListener { _, _, position, _ ->
                showLongPressMenu(displayedFiles[position])
                true
            }
        }
        mainColumn.addView(fileListView)

        val bottomBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = 28f
                setStroke(6, 0xFF7D9AC8.toInt())
            }
            setPadding(24, 14, 24, 14)
        }

        val pasteBtn = TextView(this).apply {
            text = "Paste"
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(0xFFB384FF.toInt())
                cornerRadius = 22f
            }
            setPadding(36, 16, 36, 16)
            setOnClickListener { executePaste() }
        }

        val addSpacerLeft = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
        }

        val plusBtn = TextView(this).apply {
            text = "+"
            textSize = 26f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(0xFF88A4CF.toInt())
                cornerRadius = 18f
            }
            setPadding(34, 6, 34, 6)
            setOnClickListener { showCreateMenu() }
        }

        val addSpacerRight = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
        }

        val undoBtn = TextView(this).apply {
            text = "undo"
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(0xFFB384FF.toInt())
                cornerRadius = 22f
            }
            setPadding(38, 16, 38, 16)
            setOnClickListener {
                val action = undoAction
                if (action != null) {
                    action.invoke()
                    undoAction = null
                    loadDirectory(currentDir)
                    Toast.makeText(this@MainActivity, "Undone", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "Nothing to undo", Toast.LENGTH_SHORT).show()
                }
            }
        }

        bottomBar.addView(pasteBtn)
        bottomBar.addView(addSpacerLeft)
        bottomBar.addView(plusBtn)
        bottomBar.addView(addSpacerRight)
        bottomBar.addView(undoBtn)
        mainColumn.addView(bottomBar)

        rootFrame.addView(mainColumn)

        pinDrawerOverlay = buildPinFileDrawer()
        pinDrawerOverlay.visibility = View.GONE
        rootFrame.addView(pinDrawerOverlay)

        setContentView(rootFrame)
    }

    private fun updateDriveTabStyles() {
        val activeColor = 0xFFB384FF.toInt()
        val inactiveColor = 0xFFE4DAF2.toInt()
        tabDriveC.background = GradientDrawable().apply {
            setColor(if (activeDrive == "C") activeColor else inactiveColor)
            cornerRadius = 22f
        }
        tabDriveE.background = GradientDrawable().apply {
            setColor(if (activeDrive == "E") activeColor else inactiveColor)
            cornerRadius = 22f
        }
    }

    private fun createTopPill(label: String, onClick: () -> Unit): TextView {
        return TextView(this).apply {
            text = label
            textSize = 15f
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = 18f
            }
            setPadding(16, 18, 16, 18)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(6, 0, 6, 0)
            }
            setOnClickListener { onClick() }
        }
    }

    private fun loadDirectory(dir: File) {
        if (!dir.exists()) dir.mkdirs()
        currentDir = dir
        pathTextView.text = if (activeDrive == "E") {
            val eRoot = StorageHelper.getDriveERoot(this).absolutePath
            "E:/" + dir.absolutePath.removePrefix(eRoot).trimStart('/')
        } else {
            dir.absolutePath
        }

        val rawList = dir.listFiles()?.toList() ?: emptyList()
        val sortMode = StorageHelper.getSortBy(this)

        val comparator = when (sortMode) {
            StorageHelper.SORT_DATE -> compareByDescending<File> { it.isDirectory }.thenByDescending { it.lastModified() }
            StorageHelper.SORT_SIZE -> compareByDescending<File> { it.isDirectory }.thenByDescending { it.length() }
            StorageHelper.SORT_TYPE -> compareByDescending<File> { it.isDirectory }.thenBy { it.extension.lowercase() }
            else -> compareByDescending<File> { it.isDirectory }.thenBy { it.name.lowercase() }
        }
        displayedFiles = rawList.sortedWith(comparator)

        val rows = displayedFiles.map { file ->
            val icon = when {
                file.isDirectory -> "📁"
                file.extension.equals("zip", true) -> "🗜️"
                file.extension.equals("html", true) || file.extension.equals("htm", true) -> "🌐"
                file.extension.equals("xml", true) -> "🧩"
                else -> "📄"
            }
            val dateStr = DateFormat.format("MM/dd/yy h:mm a", Date(file.lastModified())).toString()
            val meta = if (file.isDirectory) {
                val count = file.listFiles()?.size ?: 0
                "$count items · $dateStr"
            } else {
                "${StorageHelper.formatSize(file.length())} · $dateStr"
            }
            mapOf(
                "title" to "$icon  ${file.name}",
                "subtitle" to "      $meta"
            )
        }

        val textSizeSp = StorageHelper.getTextSize(this)
        val adapter = object : SimpleAdapter(
            this,
            rows,
            android.R.layout.simple_list_item_2,
            arrayOf("title", "subtitle"),
            intArrayOf(android.R.id.text1, android.R.id.text2)
        ) {
            override fun setViewText(v: TextView, text: String) {
                super.setViewText(v, text)
                if (v.id == android.R.id.text1) {
                    v.textSize = textSizeSp
                    v.setTextColor(Color.BLACK)
                } else {
                    v.textSize = (textSizeSp - 3f).coerceAtLeast(11f)
                    v.setTextColor(0xFF555555.toInt())
                }
            }
        }
        fileListView.adapter = adapter
    }

    private fun navigateUp(): Boolean {
        val rootLimit = if (activeDrive == "E") {
            StorageHelper.getDriveERoot(this)
        } else {
            StorageHelper.getDriveCRoot()
        }
        if (currentDir.absolutePath != rootLimit.absolutePath) {
            val parent = currentDir.parentFile
            if (parent != null && parent.canRead()) {
                loadDirectory(parent)
                return true
            }
        }
        return false
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (pinDrawerOverlay.visibility == View.VISIBLE) {
            pinDrawerOverlay.visibility = View.GONE
            return
        }
        if (!navigateUp()) {
            super.onBackPressed()
        }
    }

    private fun showCreateMenu() {
        val options = arrayOf(
            "New Folder",
            "New File (.txt, .xml, .html)",
            "Create .zip from folder/files"
        )
        AlertDialog.Builder(this)
            .setTitle("Create in ${currentDir.name}")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> promptNewItem(isFolder = true)
                    1 -> promptNewItem(isFolder = false)
                    2 -> promptMakeZip()
                }
            }
            .show()
    }

    private fun promptNewItem(isFolder: Boolean) {
        val input = EditText(this).apply {
            hint = if (isFolder) "Folder name" else "example.txt (or .xml, .html)"
        }
        AlertDialog.Builder(this)
            .setTitle(if (isFolder) "Create New Folder" else "Create New File")
            .setView(input)
            .setPositiveButton("Create") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isEmpty()) return@setPositiveButton
                val target = File(currentDir, name)
                if (isFolder) {
                    target.mkdirs()
                } else {
                    target.createNewFile()
                }
                undoAction = { target.deleteRecursively() }
                loadDirectory(currentDir)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun promptMakeZip() {
        if (displayedFiles.isEmpty()) {
            Toast.makeText(this, "No files in current folder to zip", Toast.LENGTH_SHORT).show()
            return
        }
        val names = displayedFiles.map { it.name }.toTypedArray()
        val checked = BooleanArray(names.size)
        val zipNameInput = EditText(this).apply {
            setText("${currentDir.name}.zip")
        }

        AlertDialog.Builder(this)
            .setTitle("Select items to compress (.zip)")
            .setMultiChoiceItems(names, checked) { _, which, isChecked ->
                checked[which] = isChecked
            }
            .setPositiveButton("Next") { _, _ ->
                val selected = displayedFiles.filterIndexed { index, _ -> checked[index] }
                if (selected.isEmpty()) return@setPositiveButton
                AlertDialog.Builder(this)
                    .setTitle("Archive Name")
                    .setView(zipNameInput)
                    .setPositiveButton("Create ZIP") { _, _ ->
                        var outName = zipNameInput.text.toString().trim()
                        if (!outName.endsWith(".zip", ignoreCase = true)) outName += ".zip"
                        val destZip = File(currentDir, outName)
                        val ok = ZipHelper.zipFiles(selected, destZip)
                        if (ok) {
                            undoAction = { destZip.delete() }
                            loadDirectory(currentDir)
                            Toast.makeText(this, "Created $outName", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "ZIP creation failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showLongPressMenu(file: File) {
        val actions = mutableListOf(
            "sandbox",
            "Properties >",
            "Share >",
            "Open in chrome >",
            "Edit >",
            "rename >",
            "cut/move >",
            "copy >",
            "Compress to .zip >",
            "Delete >"
        )
        if (file.extension.equals("zip", ignoreCase = true)) {
            actions.add(1, "Extract here (Unzip) >")
        }

        AlertDialog.Builder(this)
            .setTitle(file.name)
            .setItems(actions.toTypedArray()) { _, which ->
                when (actions[which]) {
                    "sandbox" -> {
                        startActivity(Intent(this, SandboxActivity::class.java).putExtra("file_path", file.absolutePath))
                    }
                    "Extract here (Unzip) >" -> {
                        val outDir = File(currentDir, file.nameWithoutExtension)
                        ZipHelper.unzipWithReport(file, outDir)
                        loadDirectory(currentDir)
                        Toast.makeText(this, "Extracted to ${outDir.name}", Toast.LENGTH_SHORT).show()
                    }
                    "Properties >" -> showPropertiesDialog(file)
                    "Share >" -> shareFile(file)
                    "Open in chrome >" -> openInChrome(file)
                    "Edit >" -> {
                        if (file.isDirectory) {
                            Toast.makeText(this, "Cannot edit a folder", Toast.LENGTH_SHORT).show()
                        } else {
                            startActivity(Intent(this, EditorActivity::class.java).putExtra("file_path", file.absolutePath))
                        }
                    }
                    "rename >" -> promptRename(file)
                    "cut/move >" -> {
                        clipboardFile = file
                        isCutOperation = true
                        Toast.makeText(this, "Cut: ${file.name}. Tap Paste in destination.", Toast.LENGTH_SHORT).show()
                    }
                    "copy >" -> {
                        clipboardFile = file
                        isCutOperation = false
                        Toast.makeText(this, "Copied: ${file.name}. Tap Paste in destination.", Toast.LENGTH_SHORT).show()
                    }
                    "Compress to .zip >" -> {
                        val zipTarget = File(currentDir, "${file.nameWithoutExtension}.zip")
                        if (ZipHelper.zipFiles(listOf(file), zipTarget)) {
                            loadDirectory(currentDir)
                            Toast.makeText(this, "Created ${zipTarget.name}", Toast.LENGTH_SHORT).show()
                        }
                    }
                    "Delete >" -> {
                        AlertDialog.Builder(this)
                            .setTitle("Delete ${file.name}?")
                            .setPositiveButton("Delete") { _, _ ->
                                file.deleteRecursively()
                                loadDirectory(currentDir)
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    }
                }
            }
            .show()
    }

    private fun promptRename(file: File) {
        val input = EditText(this).apply {
            setText(file.name)
            setSelection(0, file.nameWithoutExtension.length.coerceAtMost(file.name.length))
        }
        AlertDialog.Builder(this)
            .setTitle("Rename (change extension e.g. .txt to .html)")
            .setView(input)
            .setPositiveButton("Rename") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isEmpty()) return@setPositiveButton
                val oldFile = file
                val newFile = File(file.parentFile, newName)
                if (oldFile.renameTo(newFile)) {
                    undoAction = { newFile.renameTo(oldFile) }
                    loadDirectory(currentDir)
                } else {
                    Toast.makeText(this, "Rename failed", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun executePaste() {
        val src = clipboardFile
        if (src == null || !src.exists()) {
            Toast.makeText(this, "Clipboard is empty", Toast.LENGTH_SHORT).show()
            return
        }
        var dest = File(currentDir, src.name)
        if (dest.exists()) {
            dest = File(currentDir, "copy_${src.name}")
        }
        try {
            if (isCutOperation) {
                val originalParent = src.parentFile
                src.copyRecursively(dest, overwrite = true)
                src.deleteRecursively()
                clipboardFile = null
                undoAction = {
                    if (originalParent != null) {
                        dest.copyRecursively(File(originalParent, dest.name), overwrite = true)
                        dest.deleteRecursively()
                    }
                }
            } else {
                src.copyRecursively(dest, overwrite = true)
                undoAction = { dest.deleteRecursively() }
            }
            loadDirectory(currentDir)
            Toast.makeText(this, "Pasted ${dest.name}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Paste failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showPropertiesDialog(file: File) {
        val dateStr = DateFormat.format("yyyy-MM-dd HH:mm:ss", Date(file.lastModified()))
        val sizeStr = if (file.isDirectory) {
            "${file.listFiles()?.size ?: 0} items"
        } else {
            "${StorageHelper.formatSize(file.length())} (${file.length()} bytes)"
        }
        AlertDialog.Builder(this)
            .setTitle("Properties")
            .setMessage(
                "Name: ${file.name}\n" +
                "Path: ${file.absolutePath}\n" +
                "Size: $sizeStr\n" +
                "Modified: $dateStr\n" +
                "Permissions: R=${file.canRead()} W=${file.canWrite()}"
            )
            .setPositiveButton("OK", null)
            .show()
    }

    private fun shareFile(file: File) {
        if (file.isDirectory) {
            Toast.makeText(this, "Compress folder to .zip before sharing", Toast.LENGTH_SHORT).show()
            return
        }
        val uri: Uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension.lowercase()) ?: "*/*"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Share ${file.name}"))
    }

    private fun openInChrome(file: File) {
        val uri: Uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "text/html")
            setPackage("com.android.chrome")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            val fallback = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "text/html")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(fallback)
        }
    }

    private fun openFileDefault(file: File) {
        val ext = file.extension.lowercase()
        when (ext) {
            "html", "htm", "zip" -> {
                startActivity(Intent(this, SandboxActivity::class.java).putExtra("file_path", file.absolutePath))
            }
            "txt", "xml", "json", "js", "py", "kt", "css", "md", "ini", "cfg" -> {
                startActivity(Intent(this, EditorActivity::class.java).putExtra("file_path", file.absolutePath))
            }
            else -> {
                val uri: Uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
                val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, mime)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    startActivity(Intent(this, EditorActivity::class.java).putExtra("file_path", file.absolutePath))
                }
            }
        }
    }

    private fun showSearchDialog() {
        val input = EditText(this).apply {
            hint = "Enter file or folder name..."
        }
        AlertDialog.Builder(this)
            .setTitle("Search in ${currentDir.name}")
            .setView(input)
            .setPositiveButton("Search") { _, _ ->
                val query = input.text.toString().trim().lowercase()
                if (query.isEmpty()) return@setPositiveButton
                val matches = mutableListOf<File>()
                currentDir.walkTopDown().maxDepth(5).forEach {
                    if (it != currentDir && it.name.lowercase().contains(query)) {
                        matches.add(it)
                    }
                }
                if (matches.isEmpty()) {
                    Toast.makeText(this, "No matches for \"$query\"", Toast.LENGTH_SHORT).show()
                } else {
                    val labels = matches.map { "${if (it.isDirectory) "📁" else "📄"} ${it.name}\n${it.parent}" }.toTypedArray()
                    AlertDialog.Builder(this)
                        .setTitle("Results (${matches.size})")
                        .setItems(labels) { _, idx ->
                            val picked = matches[idx]
                            if (picked.isDirectory) {
                                loadDirectory(picked)
                            } else {
                                picked.parentFile?.let { loadDirectory(it) }
                                openFileDefault(picked)
                            }
                        }
                        .show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun buildPinFileDrawer(): FrameLayout {
        val overlay = FrameLayout(this).apply {
            setBackgroundColor(0x55000000)
            setOnClickListener { visibility = View.GONE }
        }

        val drawer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(0xFFCBBEE0.toInt())
                cornerRadii = floatArrayOf(28f, 28f, 0f, 0f, 0f, 0f, 28f, 28f)
            }
            setPadding(20, 20, 20, 20)
            isClickable = true
            layoutParams = FrameLayout.LayoutParams(540, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.END).apply {
                topMargin = 140
                bottomMargin = 140
            }
        }

        val closePill = View(this).apply {
            background = GradientDrawable().apply {
                setColor(Color.BLACK)
                cornerRadius = 16f
            }
            layoutParams = LinearLayout.LayoutParams(220, 20).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = 14
            }
            setOnClickListener { overlay.visibility = View.GONE }
        }
        drawer.addView(closePill)

        val pinCurrentBtn = TextView(this).apply {
            text = "+ Pin Current Folder"
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(0xFFB384FF.toInt())
                cornerRadius = 16f
            }
            setPadding(16, 14, 16, 14)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 12
            }
            setOnClickListener {
                StorageHelper.addPinnedPath(this@MainActivity, currentDir.absolutePath)
                refreshPinDrawer()
            }
        }
        drawer.addView(pinCurrentBtn)

        val scroll = ScrollView(this)
        pinListContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        scroll.addView(pinListContainer)
        drawer.addView(scroll)

        overlay.addView(drawer)
        return overlay
    }

    private fun refreshPinDrawer() {
        pinListContainer.removeAllViews()
        val pinned = StorageHelper.getPinnedPaths(this)
        for (path in pinned) {
            val folder = File(path)
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                background = GradientDrawable().apply {
                    setColor(Color.WHITE)
                    cornerRadius = 20f
                }
                setPadding(16, 16, 16, 16)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 12
                }
                setOnLongClickListener {
                    StorageHelper.removePinnedPath(this@MainActivity, path)
                    refreshPinDrawer()
                    true
                }
            }

            card.addView(TextView(this).apply {
                text = folder.name.ifEmpty { "Device storage" }
                textSize = 18f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.BLACK)
                gravity = Gravity.CENTER
            })

            card.addView(TextView(this).apply {
                text = path
                textSize = 11f
                setTextColor(0xFF666666.toInt())
                gravity = Gravity.CENTER
                setPadding(0, 2, 0, 10)
            })

            card.addView(TextView(this).apply {
                text = "go"
                textSize = 18f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.BLACK)
                gravity = Gravity.CENTER
                background = GradientDrawable().apply {
                    setColor(0xFFBDBDBD.toInt())
                    cornerRadius = 14f
                }
                setPadding(44, 10, 44, 10)
                setOnClickListener {
                    val eRoot = StorageHelper.getDriveERoot(this@MainActivity).absolutePath
                    if (path.startsWith(eRoot)) {
                        activeDrive = "E"
                    } else {
                        activeDrive = "C"
                    }
                    updateDriveTabStyles()
                    loadDirectory(folder)
                    pinDrawerOverlay.visibility = View.GONE
                }
            })

            pinListContainer.addView(card)
        }
    }
}
