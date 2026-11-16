package com.lxi.kanade

import android.content.Intent
import android.net.Uri
import com.ryanheise.audioservice.AudioServiceActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

// 继承 AudioServiceActivity：后台播放服务和界面共用同一个 Flutter 引擎
class MainActivity : AudioServiceActivity() {
    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        installLyriconBridge(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, "kanade/app_login").setMethodCallHandler { call, result ->
            when (call.method) {
                // 必须指定包名：QQ 音乐也注册了 wtloginmqq://，不指定可能被它接走
                "open" -> result.success(
                    try {
                        startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(call.argument<String>("url")))
                                .addCategory(Intent.CATEGORY_BROWSABLE)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                .setPackage(call.argument<String>("package")),
                        )
                        true
                    } catch (_: Exception) {
                        false
                    },
                )
                else -> result.notImplemented()
            }
        }
    }

    private fun installLyriconBridge(flutterEngine: FlutterEngine) {
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, "kanade.lyricon_bridge").setMethodCallHandler { call, result ->
            when (call.method) {
                "update" -> {
                    LyriconSnapshotProvider.snapshot = call.argument<String>("json")
                    result.success(null)
                }
                else -> result.notImplemented()
            }
        }
    }
}
