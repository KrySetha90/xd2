package com.filexd.manager

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ZipHelper {

    data class ZipEntryReport(
        val path: String,
        val isDirectory: Boolean,
        val ok: Boolean
    )

    fun zipFiles(sources: List<File>, destZip: File): Boolean {
        return try {
            ZipOutputStream(BufferedOutputStream(FileOutputStream(destZip))).use { zos ->
                for (src in sources) {
                    if (src.isDirectory) {
                        addFolderToZip(src, src.name, zos)
                    } else {
                        addFileToZip(src, src.name, zos)
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun addFolderToZip(folder: File, parentPath: String, zos: ZipOutputStream) {
        val children = folder.listFiles() ?: return
        if (children.isEmpty()) {
            zos.putNextEntry(ZipEntry("$parentPath/"))
            zos.closeEntry()
            return
        }
        for (file in children) {
            val entryName = "$parentPath/${file.name}"
            if (file.isDirectory) {
                addFolderToZip(file, entryName, zos)
            } else {
                addFileToZip(file, entryName, zos)
            }
        }
    }

    private fun addFileToZip(file: File, entryName: String, zos: ZipOutputStream) {
        FileInputStream(file).use { fis ->
            BufferedInputStream(fis).use { bis ->
                val entry = ZipEntry(entryName)
                zos.putNextEntry(entry)
                val buffer = ByteArray(8192)
                var count: Int
                while (bis.read(buffer).also { count = it } != -1) {
                    zos.write(buffer, 0, count)
                }
                zos.closeEntry()
            }
        }
    }

    fun unzipWithReport(zipFile: File, targetDir: File): List<ZipEntryReport> {
        val reports = mutableListOf<ZipEntryReport>()
        if (!targetDir.exists()) targetDir.mkdirs()

        ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            val buffer = ByteArray(8192)
            while (entry != null) {
                val outFile = File(targetDir, entry.name)
                if (!outFile.canonicalPath.startsWith(targetDir.canonicalPath)) {
                    reports.add(ZipEntryReport(entry.name, entry.isDirectory, false))
                    entry = zis.nextEntry
                    continue
                }
                try {
                    if (entry.isDirectory) {
                        outFile.mkdirs()
                        reports.add(ZipEntryReport(entry.name.trimEnd('/'), true, true))
                    } else {
                        outFile.parentFile?.mkdirs()
                        FileOutputStream(outFile).use { fos ->
                            var len: Int
                            while (zis.read(buffer).also { len = it } > 0) {
                                fos.write(buffer, 0, len)
                            }
                        }
                        reports.add(ZipEntryReport(entry.name, false, true))
                    }
                } catch (e: Exception) {
                    reports.add(ZipEntryReport(entry.name, entry.isDirectory, false))
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        return reports
    }
}
