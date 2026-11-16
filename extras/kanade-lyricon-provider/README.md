# Kanade 歌词支持

作者：Hollowden

这是供 [Lyricon（词幕服务）](https://github.com/tomakino/lyricon) 使用的 Kanade 歌词提供插件。它以 LSPosed 模块的形式注入 Kanade，将歌曲信息、逐行歌词、翻译和播放进度发送给词幕服务。它只适配本 Fork 中带有歌词快照桥接的 Kanade，不代表 Kanade 原作者发布的官方插件。

## 安装

1. 安装本目录 dist 下的 APK，并安装、启用 Lyricon 中心。
2. 在 LSPosed 中启用“Kanade 歌词支持”，作用域勾选 Kanade。
3. 强制停止并重开 Kanade，播放一首有歌词的歌曲。

本 Fork 的 Kanade 会在应用进程内提供歌词快照；插件读取该快照并交给 Lyricon。背屏歌词还需要相应的 Lyricon 订阅模块，例如 REAREye。播放器首次启动时若歌词尚未下载，词幕端可能先收到只有歌曲信息的快照。

## 构建

使用 JDK 17 和 Android SDK，在本目录运行 ./gradlew :app:assembleRelease（Windows 用 gradlew.bat）。本地发布构建使用 Android 调试签名；自行构建时签名可能不同，覆盖安装前请检查签名。

Kanade 原项目的代码和署名归原作者所有；本插件及本 Fork 的歌词适配由 Hollowden 维护。
