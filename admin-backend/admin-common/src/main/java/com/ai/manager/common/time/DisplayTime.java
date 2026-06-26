package com.ai.manager.common.time;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public final class DisplayTime {

    public static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private static final DateTimeFormatter DISPLAY =
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm").withZone(ZONE);

    private static final DateTimeFormatter DISPLAY_SECONDS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZONE);

    private DisplayTime() {}

    public static String formatMinute(Instant instant) {
        return DISPLAY.format(instant);
    }

    public static String formatSeconds(Instant instant) {
        return DISPLAY_SECONDS.format(instant);
    }

    public static ZonedDateTime toZoned(Instant instant) {
        return instant.atZone(ZONE);
    }
}
