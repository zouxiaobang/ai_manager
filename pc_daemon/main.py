import argparse
import asyncio
import logging
import sys

from ai_manager_daemon.cloudmusic import NeteaseCloudMusicController
from ai_manager_daemon.config import load_config
from ai_manager_daemon.desktop_app_control import DesktopAppController
from ai_manager_daemon.server import _startup_prepare_desktop_app
from ai_manager_daemon.logging_setup import setup_logging
from ai_manager_daemon.runtime_state import register_pid_cleanup
from ai_manager_daemon.server import run_server

logger = logging.getLogger("uvicorn.error")


async def monitor_netease(config) -> None:
    desktop_app = DesktopAppController(config.desktop_app.executable, config.desktop_app.process_name)
    await _startup_prepare_desktop_app(config, desktop_app)

    music = NeteaseCloudMusicController(
        config.netease.playback_poll_interval_seconds,
        config.netease.lyric_poll_interval_seconds,
        config.netease.lyric_offset_ms,
        config.desktop_app.process_name,
        config.netease.progress_source,
        config.netease.cdp_port,
    )
    logger.info("Monitoring NetEase Cloud Music playback (Ctrl+C to stop)")
    last_line = ""

    while True:
        playback, lyric = await music.fetch_playback_snapshot()
        if playback is None:
            line = "No active NetEase media session. Start playback in desktop app first."
        else:
            current = lyric.line if lyric is not None else ""
            line = (
                f"[{playback.state}] {playback.title} - {playback.artist} "
                f"| {playback.position_ms}/{playback.duration_ms} ms | {current}"
            )

        if line != last_line:
            print(line, flush=True)
            last_line = line

        await asyncio.sleep(config.netease.playback_poll_interval_seconds)


def main() -> None:
    if sys.platform == "win32":
        asyncio.set_event_loop_policy(asyncio.WindowsSelectorEventLoopPolicy())

    parser = argparse.ArgumentParser(description="AI Manager PC Daemon")
    parser.add_argument(
        "--monitor",
        action="store_true",
        help="Monitor NetEase playback/lyrics in console (no WebSocket server)",
    )
    args = parser.parse_args()

    setup_logging()
    register_pid_cleanup()
    config = load_config()

    if args.monitor:
        try:
            asyncio.run(monitor_netease(config))
        except KeyboardInterrupt:
            print("Stopped.", flush=True)
        return

    run_server(config)


if __name__ == "__main__":
    main()
