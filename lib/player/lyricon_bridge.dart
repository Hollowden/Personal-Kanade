import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:flutter/services.dart';

import 'player_controller.dart';

/// 把当前歌曲、歌词和播放进度压成一份快照，交给 Android 侧的 ContentProvider。
///
/// LSPosed 模块不能直接访问 Flutter isolate，所以这里只负责写快照；模块会在
/// Kanade 进程里轮询 ContentProvider，再转发给 Lyricon。
class LyriconBridge {
  LyriconBridge._(this._player) {
    _player.addListener(_onPlayerChanged);
    _player.position.addListener(_onPosition);
    _scheduleSend();
  }

  static const _channel = MethodChannel('kanade.lyricon_bridge');

  /// 非 Android 平台不启用，避免 Windows 调试时多出无用的平台通道调用。
  static LyriconBridge? attach(PlayerController player) =>
      Platform.isAndroid ? LyriconBridge._(player) : null;

  final PlayerController _player;
  Timer? _positionTimer;
  bool _disposed = false;
  bool _dirty = false;
  bool _sending = false;

  void _onPlayerChanged() => _scheduleSend();

  void _onPosition() {
    if (_positionTimer != null) return;
    // 250ms 刷新一次位置，既能保证逐字歌词的时间轴同步精度，也不会明显增加平台通道开销。
    _positionTimer = Timer(const Duration(milliseconds: 250), () {
      _positionTimer = null;
      _scheduleSend();
    });
  }

  void _scheduleSend() {
    if (_disposed) return;
    _dirty = true;
    if (!_sending) unawaited(_flush());
  }

  Future<void> _flush() async {
    _sending = true;
    while (_dirty && !_disposed) {
      _dirty = false;
      final snapshot = _buildSnapshot();
      try {
        await _channel.invokeMethod<void>('update', {
          'json': snapshot == null ? null : jsonEncode(snapshot),
        });
      } catch (_) {
        // 桥接失败只影响 Lyricon，不能妨碍本地播放。
        break;
      }
    }
    _sending = false;
    if (_dirty && !_disposed) unawaited(_flush());
  }

  Map<String, Object?>? _buildSnapshot() {
    final song = _player.current;
    if (song == null) return null;

    final actualDuration = _player.duration.inMilliseconds;
    final duration = actualDuration > 0
        ? actualDuration
        : song.durationSecs * 1000;
    final position = _player.position.value.inMilliseconds.clamp(
      0,
      duration > 0 ? duration : 0,
    );

    return {
      'id': song.mid,
      'name': song.name,
      'artist': song.singers
          .map((singer) => singer.name)
          .where((name) => name.isNotEmpty)
          .join(' / '),
      'duration': duration,
      'playing': _player.playing,
      'position': position,
      'lyrics': _buildLyrics(duration),
    };
  }

  List<Map<String, Object?>> _buildLyrics(int duration) {
    final lyric = _player.lyric;
    final source = lyric?.lines ?? const [];
    List<dynamic>? qrcLines;
    final plain = lyric?.plain ?? '';
    if (source.isNotEmpty && plain.startsWith('[')) {
      try {
        final decoded = jsonDecode(plain);
        if (decoded is List) {
          qrcLines = decoded;
        }
      } catch (_) {
        qrcLines = null;
      }
    }

    final lines = <Map<String, Object?>>[];
    for (var i = 0; i < source.length; i++) {
      final line = source[i];
      final text = line.text.trim();
      if (text.isEmpty) continue;

      var begin = line.timeMs < 0 ? 0 : line.timeMs;
      var end = 0;
      List<Map<String, Object?>>? words;

      if (qrcLines != null && i < qrcLines.length) {
        final qrcItem = qrcLines[i];
        if (qrcItem is Map) {
          final qBegin = (qrcItem['begin'] as num?)?.toInt();
          final qEnd = (qrcItem['end'] as num?)?.toInt();
          if (qBegin != null && qBegin >= 0) {
            begin = qBegin;
          }
          if (qEnd != null && qEnd > begin) {
            end = qEnd;
          }
          final rawWords = qrcItem['words'];
          if (rawWords is List && rawWords.isNotEmpty) {
            final parsedWords = <Map<String, Object?>>[];
            for (final w in rawWords) {
              if (w is Map) {
                final wBegin = (w['begin'] as num?)?.toInt() ?? 0;
                final wEnd = (w['end'] as num?)?.toInt() ?? 0;
                final wDur = (w['duration'] as num?)?.toInt() ?? (wEnd - wBegin);
                final wText = (w['text'] as String?) ?? '';
                if (wText.isNotEmpty && wEnd >= wBegin) {
                  parsedWords.add({
                    'begin': wBegin < 0 ? 0 : wBegin,
                    'end': wEnd < wBegin ? wBegin : wEnd,
                    'duration': wDur < 1 ? 1 : wDur,
                    'text': wText,
                  });
                }
              }
            }
            if (parsedWords.isNotEmpty) {
              words = parsedWords;
            }
          }
        }
      }

      var nextBegin = duration;
      for (var j = i + 1; j < source.length; j++) {
        final next = source[j].timeMs;
        if (next > begin) {
          nextBegin = next;
          break;
        }
      }
      if (end <= begin || (nextBegin > begin && end > nextBegin)) {
        end = nextBegin;
      }
      if (end <= begin) continue;

      final translation = line.trans.trim();
      lines.add({
        'begin': begin,
        'end': end,
        'duration': end - begin,
        'text': text,
        if (translation.isNotEmpty) 'translation': translation,
        if (words != null) 'words': words,
      });
    }
    return lines;
  }

  void dispose() {
    _disposed = true;
    _positionTimer?.cancel();
    _positionTimer = null;
    _player.removeListener(_onPlayerChanged);
    _player.position.removeListener(_onPosition);
  }
}
