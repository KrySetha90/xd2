package com.filexd.manager

import android.database.Cursor
import android.database.MatrixCursor
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract.Document
import android.provider.DocumentsContract.Root
import android.provider.DocumentsProvider
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileNotFoundException

class FileXdDocumentProvider : DocumentsProvider() {

    private val defaultRootProjection = arrayOf(
        Root.COLUMN_ROOT_ID,
        Root.COLUMN_MIME_TYPES,
        Root.COLUMN_FLAGS,
        Root.COLUMN_ICON,
        Root.COLUMN_TITLE,
        Root.COLUMN_SUMMARY,
        Root.COLUMN_DOCUMENT_ID
    )

    private val defaultDocumentProjection = arrayOf(
        Document.COLUMN_DOCUMENT_ID,
        Document.COLUMN_MIME_TYPE,
        Document.COLUMN_DISPLAY_NAME,
        Document.COLUMN_LAST_MODIFIED,
        Document.COLUMN_FLAGS,
        Document.COLUMN_SIZE
    )

    override fun onCreate(): Boolean = true

    override fun queryRoots(projection: Array<out String>?): Cursor {
        val result = MatrixCursor(projection ?: defaultRootProjection)
        val ctx = context ?: return result

        val driveC = StorageHelper.getDriveCRoot()
        result.newRow().apply {
            add(Root.COLUMN_ROOT_ID, "drive_c")
            add(Root.COLUMN_DOCUMENT_ID, driveC.absolutePath)
            add(Root.COLUMN_TITLE, "File_XD (PE:/)")
            add(Root.COLUMN_SUMMARY, "Shared Storage")
            add(Root.COLUMN_FLAGS, Root.FLAG_SUPPORTS_CREATE or Root.FLAG_LOCAL_ONLY)
            add(Root.COLUMN_MIME_TYPES, "*/*")
            add(Root.COLUMN_ICON, R.mipmap.ic_launcher)
        }

        val driveE = StorageHelper.getDriveERoot(ctx)
        result.newRow().apply {
            add(Root.COLUMN_ROOT_ID, "drive_e")
            add(Root.COLUMN_DOCUMENT_ID, driveE.absolutePath)
            add(Root.COLUMN_TITLE, "File_XD (E:/ App Folder)")
            add(Root.COLUMN_SUMMARY, "App Folder Storage")
            add(Root.COLUMN_FLAGS, Root.FLAG_SUPPORTS_CREATE or Root.FLAG_LOCAL_ONLY)
            add(Root.COLUMN_MIME_TYPES, "*/*")
            add(Root.COLUMN_ICON, R.mipmap.ic_launcher)
        }
        return result
    }

    override fun queryDocument(documentId: String, projection: Array<out String>?): Cursor {
        val result = MatrixCursor(projection ?: defaultDocumentProjection)
        includeFile(result, File(documentId))
        return result
    }

    override fun queryChildDocuments(
        parentDocumentId: String,
        projection: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        val result = MatrixCursor(projection ?: defaultDocumentProjection)
        val parent = File(parentDocumentId)
        parent.listFiles()?.forEach { includeFile(result, it) }
        return result
    }

    override fun openDocument(
        documentId: String,
        mode: String,
        signal: CancellationSignal?
    ): ParcelFileDescriptor {
        val file = File(documentId)
        val accessMode = ParcelFileDescriptor.parseMode(mode)
        return ParcelFileDescriptor.open(file, accessMode)
    }

    override fun createDocument(
        parentDocumentId: String,
        mimeType: String,
        displayName: String
    ): String {
        val parent = File(parentDocumentId)
        var target = File(parent, displayName)
        var index = 1
        while (target.exists()) {
            target = File(parent, "${displayName}_$index")
            index++
        }
        if (mimeType == Document.MIME_TYPE_DIR) {
            if (!target.mkdirs()) throw FileNotFoundException("Failed to create folder")
        } else {
            if (!target.createNewFile()) throw FileNotFoundException("Failed to create file")
        }
        return target.absolutePath
    }

    override fun deleteDocument(documentId: String) {
        val file = File(documentId)
        if (!file.deleteRecursively()) {
            throw FileNotFoundException("Failed to delete $documentId")
        }
    }

    override fun renameDocument(documentId: String, displayName: String): String {
        val source = File(documentId)
        val dest = File(source.parentFile, displayName)
        if (!source.renameTo(dest)) {
            throw FileNotFoundException("Failed to rename $documentId")
        }
        return dest.absolutePath
    }

    private fun includeFile(result: MatrixCursor, file: File) {
        var flags = 0
        if (file.isDirectory && file.canWrite()) {
            flags = flags or Document.FLAG_DIR_SUPPORTS_CREATE
        } else if (file.canWrite()) {
            flags = flags or Document.FLAG_SUPPORTS_WRITE or
                Document.FLAG_SUPPORTS_DELETE or
                Document.FLAG_SUPPORTS_RENAME
        }
        val mimeType = if (file.isDirectory) {
            Document.MIME_TYPE_DIR
        } else {
            val ext = file.extension.lowercase()
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
        }
        result.newRow().apply {
            add(Document.COLUMN_DOCUMENT_ID, file.absolutePath)
            add(Document.COLUMN_DISPLAY_NAME, file.name)
            add(Document.COLUMN_SIZE, file.length())
            add(Document.COLUMN_MIME_TYPE, mimeType)
            add(Document.COLUMN_LAST_MODIFIED, file.lastModified())
            add(Document.COLUMN_FLAGS, flags)
        }
    }
}
