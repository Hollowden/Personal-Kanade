package com.lxi.kanade

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle

/**
 * 保存 Flutter 写来的歌词快照，供同进程内的 LSPosed 模块读取。
 *
 * exported=false 时普通应用无法查询；LSPosed 注入 Kanade 后沿用同一 UID，
 * 因此仍可通过 contentResolver.call("snapshot") 读取。
 */
class LyriconSnapshotProvider : ContentProvider() {
    companion object {
        @Volatile
        var snapshot: String? = null
    }

    override fun onCreate(): Boolean = true

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? = when (method) {
        "snapshot" -> Bundle().apply { putString("json", snapshot) }
        else -> null
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0
}
