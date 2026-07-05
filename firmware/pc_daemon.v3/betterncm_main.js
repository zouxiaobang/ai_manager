/* progress-bridge v0.3: 上报进度 + 支持 seek 命令轮询 */
(function () {
  "use strict";

  // 配置文件（与 v3 主程序约定）
  const CONFIG = {
    reportUrl: "http://127.0.0.1:9877/internal/progress",
    seekPollUrl: "http://127.0.0.1:9877/internal/seek-poll",
    seekDoneUrl: "http://127.0.0.1:9877/internal/seek-done",
    pollIntervalMs: 300,
    reportIntervalMs: 500,
  };

  let audioEl = null;
  let probeCount = 0;

  // ---- HTTP 工具 ----
  function postJson(url, obj) {
    try {
      fetch(url, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(obj),
      }).catch(function () {});
    } catch (e) {}
  }

  function getJson(url) {
    return fetch(url, { method: "GET", cache: "no-store" })
      .then(function (r) { return r.json(); })
      .catch(function () { return null; });
  }

  // ---- Audio 元素查找 ----
  function findAudioEl() {
    var el = document.querySelector("audio, video");
    if (el) return el;
    try {
      var frames = document.querySelectorAll("iframe");
      for (var f = 0; f < frames.length; f++) {
        try {
          el = frames[f].contentDocument && frames[f].contentDocument.querySelector("audio, video");
          if (el) return el;
        } catch (e) {}
      }
    } catch (e) {}
    try {
      var all = document.querySelectorAll("*");
      for (var n = 0; n < all.length; n++) {
        var node = all[n];
        if (node.tagName === "AUDIO" || node.tagName === "VIDEO") return node;
        if (node.shadowRoot) {
          el = node.shadowRoot.querySelector("audio, video");
          if (el) return el;
        }
      }
    } catch (e) {}
    return null;
  }

  // ---- 进度上报 ----
  function report() {
    if (!audioEl) audioEl = findAudioEl();
    if (!audioEl) {
      postJson(CONFIG.reportUrl, {
        position_ms: 0, duration_ms: 0, playing: false,
        ts: Date.now(), no_audio: true
      });
      return;
    }
    postJson(CONFIG.reportUrl, {
      position_ms: Math.round((audioEl.currentTime || 0) * 1000),
      duration_ms: Math.round((audioEl.duration || 0) * 1000),
      playing: !audioEl.paused && !audioEl.ended,
      ts: Date.now()
    });
  }

  // ---- Seek 命令轮询 ----
  function pollSeek() {
    if (!audioEl) {
      audioEl = findAudioEl();
      if (!audioEl) return;
    }

    getJson(CONFIG.seekPollUrl).then(function (cmd) {
      if (!cmd) return;
      if (cmd.position_ms && audioEl) {
        var sec = cmd.position_ms / 1000;
        audioEl.currentTime = sec;
        console.log("[progress-bridge] Seek to " + cmd.position_ms + "ms (" + sec.toFixed(1) + "s)");
        // 通知已执行
        postJson(CONFIG.seekDoneUrl, {
          position_ms: cmd.position_ms,
          executed_at: Date.now()
        });
      }
    });
  }

  // ---- 初始化 ----
  console.log("[progress-bridge] v0.3 loaded");

  // 进度上报
  setInterval(report, CONFIG.reportIntervalMs);
  setTimeout(report, 500);
  setTimeout(report, 2000);

  // seek 轮询
  setInterval(pollSeek, CONFIG.pollIntervalMs);
})();
