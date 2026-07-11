#pragma once

#include "lvgl.h"
#include "pixel_dog_model.h"

#ifdef __cplusplus
extern "C" {
#endif

void dog_sprite_init(void);
lv_obj_t *dog_sprite_create(lv_obj_t *parent, int pixel_scale);
void dog_sprite_update(void);
void dog_sprite_set_scale(int pixel_scale);
void dog_sprite_set_status(DogStatus status);
void dog_sprite_set_level(int level);

#ifdef __cplusplus
}
#endif