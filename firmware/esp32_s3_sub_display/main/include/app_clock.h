#pragma once

#include <cstddef>

void app_clock_init();
void app_clock_tick();
bool app_clock_is_synced();
void app_clock_format_time(char *buf, size_t len);
void app_clock_get_hm(int *hour, int *min);
