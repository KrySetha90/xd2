package com.filexd.manager

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import java.io.File

object StorageHelper {
    const val PREFS_NAME = "file_xd_prefs"
    const val KEY_BG_COLOR = "bg_color"
    const val KEY_TEXT_SIZE = "text_size"
    const val KEY_SORT_BY = "sort_by"
    const val KEY_PINNED = "pinned_paths"

    const val SORT_NAME = 0
    const val SORT_DATE = 1
    const val SORT_SIZE = 2
    const val SORT_TYPE = 3

    fun getDriveCRoot(): File {
        return Environment.getExternalStorageDirectory()
    }

    fun getDriveERoot(context: Context): File {
        val dir = context.getExternalFilesDir(null) ?: File(context.filesDir, "Drive_E")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun hasAllFilesPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }

    fun requestAllFilesPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                context.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
            }
        }
    }

    fun getBgColor(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_BG_COLOR, 0xFFFFFFFF.toInt())
    }

    fun setBgColor(context: Context, color: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putInt(KEY_BG_COLOR, color).apply()
    }

    fun getTextSize(context: Context): Float {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getFloat(KEY_TEXT_SIZE, 16f)
    }

    fun setTextSize(context: Context, size: Float) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putFloat(KEY_TEXT_SIZE, size).apply()
    }

    fun getSortBy(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_SORT_BY, SORT_DATE)
    }

    fun setSortBy(context: Context, sort: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putInt(KEY_SORT_BY, sort).apply()
    }

    fun getPinnedPaths(context: Context): MutableList<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_PINNED, null)
        if (raw == null) {
            val defaults = mutableListOf(
                File(getDriveCRoot(), "Download").absolutePath,
                getDriveCRoot().absolutePath,
                getDriveERoot(context).absolutePath
            )
            savePinnedPaths(context, defaults)
            return defaults
        }
        return raw.split("|").filter { it.isNotBlank() }.toMutableList()
    }

    fun savePinnedPaths(context: Context, paths: List<String>) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_PINNED, paths.joinToString("|")).apply()
    }

    fun addPinnedPath(context: Context, path: String) {
        val list = getPinnedPaths(context)
        if (!list.contains(path)) {
            list.add(path)
            savePinnedPaths(context, list)
        }
    }

    fun removePinnedPath(context: Context, path: String) {
        val list = getPinnedPaths(context)
        list.remove(path)
        savePinnedPaths(context, list)
    }

    fun formatSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format("%.1f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024) return String.format("%.1f MB", mb)
        return String.format("%.2f GB", mb / 1024.0)
    }
}
