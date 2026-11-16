package com.hollowden.kanade.lyricon

import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import de.robv.android.xposed.XposedBridge
import io.github.proify.lyricon.lyric.model.LyricWord
import io.github.proify.lyricon.lyric.model.RichLyricLine
import io.github.proify.lyricon.lyric.model.Song
import io.github.proify.lyricon.provider.LyriconFactory
import io.github.proify.lyricon.provider.LyriconProvider
import io.github.proify.lyricon.provider.service.addConnectionListener
import org.json.JSONObject

object KanadeBridge {
    private const val TAG = "KanadeLyricon"
    private const val MODULE_PACKAGE = "com.hollowden.kanade.lyricon"
    private const val PLAYER_PACKAGE = "com.lxi.kanade"
    private const val POLL_INTERVAL_MS = 250L

    fun start(context: Application) {
        val thread = HandlerThread("KanadeLyriconBridge", Process.THREAD_PRIORITY_BACKGROUND).apply { start() }
        val handler = Handler(thread.looper)
        handler.post {
            val poller = SnapshotPoller(context)
            val tick = object : Runnable {
                override fun run() {
                    poller.poll()
                    handler.postDelayed(this, POLL_INTERVAL_MS)
                }
            }
            handler.post(tick)
        }
    }
}

private class SnapshotPoller(private val context: Context) {
    companion object {
        private const val TAG = "KanadeLyricon"
        private val SNAPSHOT_URI: Uri = Uri.parse("content://com.lxi.kanade.lyricon")
    }

    private val provider: LyriconProvider = LyriconFactory.createProvider(
        context = context,
        providerPackageName = "com.hollowden.kanade.lyricon",
        playerPackageName = "com.lxi.kanade",
    ).apply {
        autoSync = true
        player.setDisplayTranslation(true)
        player.setPositionUpdateInterval(250)
    }

    @Volatile
    private var forceSync = true
    private var lastRaw: String? = null
    private var lastSongKey: String? = null
    private var lastPosition: Long? = null
    private var lastPlaying: Boolean? = null
    private var lastError: String? = null

    init {
        provider.service.addConnectionListener {
            onConnected {
                XposedBridge.log("$TAG: Lyricon connected")
                forceSync = true
            }
            onReconnected {
                XposedBridge.log("$TAG: Lyricon reconnected")
                forceSync = true
            }
            onDisconnected {
                XposedBridge.log("$TAG: Lyricon disconnected")
            }
            onConnectTimeout {
                XposedBridge.log("$TAG: Lyricon connection timed out")
            }
        }
        if (!provider.register()) XposedBridge.log("$TAG: Lyricon register request was rejected")
    }

    fun poll() {
        try {
            val raw = context.contentResolver
                .call(SNAPSHOT_URI, "snapshot", null, null)
                ?.getString("json")
            if (raw.isNullOrBlank()) {
                if (lastRaw != null) clear()
                return
            }
            if (raw == lastRaw && !forceSync) return
            lastRaw = raw
            apply(raw)
            lastError = null
        } catch (t: Throwable) {
            val error = t.toString()
            if (error != lastError) {
                lastError = error
                XposedBridge.log("$TAG: read snapshot failed: $t")
            }
        }
    }

    private fun apply(raw: String) {
        val root = JSONObject(raw)
        val name = root.optString("name")
        if (name.isBlank()) return

        val id = root.optString("id").ifBlank { "$name/${root.optString("artist")}" }
        val artist = root.optString("artist")
        val duration = root.optLong("duration", 0L).coerceAtLeast(0L)
        val lyricsJson = root.optJSONArray("lyrics")
        val songKey = listOf(id, name, artist, duration.toString(), lyricsJson?.toString().orEmpty()).joinToString("\u0001")

        if (forceSync || songKey != lastSongKey) {
            val lyrics = buildList {
                val array = lyricsJson ?: return@buildList
                for (i in 0 until array.length()) {
                    val line = array.optJSONObject(i) ?: continue
                    val begin = line.optLong("begin", 0L).coerceAtLeast(0L)
                    val end = line.optLong("end", 0L)
                    val text = line.optString("text")
                    if (begin >= end || text.isBlank()) continue
                    val translation = if (line.isNull("translation")) null else line.optString("translation").ifBlank { null }
                    val wordsJson = line.optJSONArray("words")
                    val words = if (wordsJson != null && wordsJson.length() > 0) {
                        buildList {
                            for (wIdx in 0 until wordsJson.length()) {
                                val wObj = wordsJson.optJSONObject(wIdx) ?: continue
                                val wBegin = wObj.optLong("begin", 0L).coerceAtLeast(0L)
                                val wEnd = wObj.optLong("end", wBegin).coerceAtLeast(wBegin)
                                val wDur = wObj.optLong("duration", (wEnd - wBegin).coerceAtLeast(1L)).coerceAtLeast(1L)
                                val wText = wObj.optString("text")
                                if (wText.isNotEmpty()) {
                                    add(
                                        LyricWord(
                                            begin = wBegin,
                                            end = wEnd,
                                            duration = wDur,
                                            text = wText,
                                        ),
                                    )
                                }
                            }
                        }.takeIf { it.isNotEmpty() }
                    } else {
                        null
                    }
                    add(
                        RichLyricLine(
                            begin = begin,
                            end = end,
                            duration = (end - begin).coerceAtLeast(1L),
                            text = text,
                            words = words,
                            translation = translation,
                        ),
                    )
                }
            }
            provider.player.setSong(
                Song(
                    id = id,
                    name = name,
                    artist = artist,
                    duration = duration,
                    lyrics = lyrics,
                ),
            )
            lastSongKey = songKey
        }

        val position = root.optLong("position", 0L).coerceAtLeast(0L)
        val playing = root.optBoolean("playing", false)
        if (forceSync || position != lastPosition) {
            provider.player.setPosition(position)
            lastPosition = position
        }
        if (forceSync || playing != lastPlaying) {
            provider.player.setPlaybackState(playing)
            lastPlaying = playing
        }
        forceSync = false
    }

    private fun clear() {
        provider.player.setSong(null)
        provider.player.setPlaybackState(false)
        lastRaw = null
        lastSongKey = null
        lastPosition = null
        lastPlaying = null
    }
}
