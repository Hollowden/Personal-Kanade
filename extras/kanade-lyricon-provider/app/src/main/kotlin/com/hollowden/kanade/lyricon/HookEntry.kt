package com.hollowden.kanade.lyricon

import android.app.Application
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.util.concurrent.atomic.AtomicBoolean

class HookEntry : IXposedHookLoadPackage {
    companion object {
        private const val TAG = "KanadeLyricon"
        private const val TARGET_PACKAGE = "com.lxi.kanade"
        private val started = AtomicBoolean(false)
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != TARGET_PACKAGE) return

        try {
            XposedHelpers.findAndHookMethod(
                Application::class.java,
                "onCreate",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val app = param.thisObject as? Application ?: return
                        if (!started.compareAndSet(false, true)) return
                        XposedBridge.log("$TAG: Kanade process hooked")
                        KanadeBridge.start(app)
                    }
                },
            )
        } catch (t: Throwable) {
            XposedBridge.log("$TAG: hook Application.onCreate failed: $t")
        }
    }
}
