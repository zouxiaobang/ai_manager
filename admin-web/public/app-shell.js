/**
 * 同步壳层检测 + 视口变化监听（在 Vue 加载前执行）
 * 与 src/utils/deviceShell.ts 逻辑保持一致
 */
(function () {
  var SESSION_KEY = 'ai-manager-app-shell-session';
  var LEGACY_KEYS = ['ai-manager-app-shell-pref', 'ai-manager-app-shell'];
  var MAX = 768;

  function purgeLegacy() {
    try {
      for (var i = 0; i < LEGACY_KEYS.length; i++) {
        localStorage.removeItem(LEGACY_KEYS[i]);
      }
    } catch (e) {
      /* ignore */
    }
  }

  function readQuery() {
    var q = new URLSearchParams(location.search).get('shell');
    return q === 'pc' || q === 'mobile' ? q : null;
  }

  function readSession() {
    try {
      var s = sessionStorage.getItem(SESSION_KEY);
      return s === 'pc' || s === 'mobile' ? s : null;
    } catch (e) {
      return null;
    }
  }

  function isUaMobile() {
    var ua = navigator.userAgent || '';
    return (
      /Android|webOS|iPhone|iPod|iPad|BlackBerry|IOMobile|Opera Mini|Mobile/i.test(ua) ||
      (navigator.maxTouchPoints > 1 && /MacIntel|Macintosh/i.test(navigator.platform))
    );
  }

  function isNarrow() {
    return Math.min(window.innerWidth, document.documentElement.clientWidth) <= MAX;
  }

  function autoShell() {
    return isUaMobile() || isNarrow() ? 'mobile' : 'pc';
  }

  function resolveShell() {
    purgeLegacy();
    return readQuery() || readSession() || autoShell();
  }

  function hasManualOverride() {
    return readQuery() !== null || readSession() !== null;
  }

  function prepareMobileUrl() {
    if (location.hash.length > 2) return;
    var path = location.pathname.replace(/^\//, '') || 'home';
    history.replaceState(null, '', '/#/' + path + location.search);
  }

  function preparePcUrl() {
    if (!location.hash || location.hash.indexOf('#/') !== 0) return;
    var path = location.hash.slice(2) || 'home';
    history.replaceState(null, '', '/' + path + location.search);
  }

  var shell = resolveShell();
  document.documentElement.classList.toggle('is-mobile-shell', shell === 'mobile');
  document.documentElement.dataset.appShell = shell;

  if (shell === 'mobile') {
    prepareMobileUrl();
  } else {
    preparePcUrl();
  }

  if (hasManualOverride()) {
    return;
  }

  var reloading = false;
  var activeShell = shell;

  function maybeSwitch() {
    if (reloading || hasManualOverride()) return;
    var next = autoShell();
    if (next === activeShell) return;
    reloading = true;
    if (next === 'mobile') {
      prepareMobileUrl();
    } else {
      preparePcUrl();
    }
    location.reload();
  }

  try {
    var mql = window.matchMedia('(max-width: ' + MAX + 'px)');
    if (mql.addEventListener) {
      mql.addEventListener('change', maybeSwitch);
    } else if (mql.addListener) {
      mql.addListener(maybeSwitch);
    }
    window.addEventListener('orientationchange', function () {
      setTimeout(maybeSwitch, 150);
    });
  } catch (e) {
    /* ignore */
  }
})();
