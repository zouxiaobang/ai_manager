import asyncio
from winsdk.windows.media.control import GlobalSystemMediaTransportControlsSessionManager as Mgr


async def test():
    manager = await Mgr.request_async()
    for session in manager.get_sessions():
        app = (session.source_app_user_model_id or "").lower()
        if "cloudmusic" in app or "netease" in app:
            print(f"Found: {app}")
            props = await session.try_get_media_properties_async()
            print(f"Now playing: {props.title} - {props.artist}")

            # try_change_playback_position_async 接受 TimeSpan，
            # 在 winsdk 中映射为 int ticks（100 纳秒单位）
            # 10 秒 = 10 * 10^7 ticks = 100000000 ticks
            result = await session.try_change_playback_position_async(10 * 10**7)
            print(f"Seek to 10s: {'SUCCESS' if result else 'FAILED'}")
            return
    print("NetEase session not found")

asyncio.run(test())
