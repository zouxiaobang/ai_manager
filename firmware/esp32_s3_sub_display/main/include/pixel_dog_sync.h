#pragma once

#include "esp_err.h"

esp_err_t dog_sync_start();
void dog_sync_mark_dirty(void);
bool dog_sync_interact(const char *action);