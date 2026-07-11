#include "pixel_dog_sprite.h"

#include <cstdlib>
#include <cstring>

#include "esp_heap_caps.h"
#include "esp_log.h"
#include "panel_config.h"
#include "widgets/canvas/lv_canvas.h"

namespace {
constexpr int kDogCols = 16;
constexpr int kDogRows = 16;
constexpr int kDogColors = 4;
constexpr char kSpriteTag[] = "dog_sprite";

const uint8_t kDogIdleFrames[2][kDogRows][kDogCols] = {
    {
        {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
        {0,0,0,0,0,0,3,1,1,3,0,0,0,0,0,0},
        {0,0,0,0,0,3,1,2,2,1,3,0,0,0,0,0},
        {0,0,0,0,3,1,2,2,2,2,1,3,0,0,0,0},
        {0,0,0,1,1,2,2,1,1,2,2,1,1,0,0,0},
        {0,0,0,1,2,2,2,1,1,2,2,2,1,0,0,0},
        {0,0,1,1,2,2,2,2,2,2,2,1,1,0,0,0},
        {0,0,1,2,2,2,2,2,2,2,2,2,1,0,0,0},
        {0,0,1,2,2,2,2,2,2,2,2,2,1,1,0,0},
        {0,1,1,2,2,2,2,2,2,2,2,2,2,1,0,0},
        {0,1,2,2,2,2,2,2,2,2,2,2,2,1,0,0},
        {0,1,2,2,2,2,2,2,2,2,2,2,2,1,0,0},
        {0,0,1,1,2,2,2,2,2,2,2,1,1,0,0,0},
        {0,0,0,1,1,1,1,1,1,1,1,1,0,0,0,0},
        {0,0,0,0,0,1,3,3,3,1,0,0,0,0,0,0},
        {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
    },
    {
        {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
        {0,0,0,0,0,0,3,1,1,3,0,0,0,0,0,0},
        {0,0,0,0,0,3,1,2,2,1,3,0,0,0,0,0},
        {0,0,0,0,3,1,2,2,2,2,1,3,0,0,0,0},
        {0,0,0,1,1,2,2,1,1,2,2,1,1,0,0,0},
        {0,0,0,1,2,2,2,1,1,2,2,2,1,0,0,0},
        {0,0,1,1,2,2,2,2,2,2,2,1,1,0,0,0},
        {0,0,1,2,2,2,2,2,2,2,2,2,1,0,0,0},
        {0,0,1,2,2,2,2,2,2,2,2,2,1,1,0,0},
        {0,1,1,2,2,2,2,2,2,2,2,2,2,1,0,0},
        {0,1,2,2,2,2,2,2,2,2,2,2,2,1,0,0},
        {0,1,2,2,2,2,2,2,2,2,2,2,2,1,0,0},
        {0,0,1,1,2,2,2,2,2,2,2,1,1,0,0,0},
        {0,0,0,1,1,1,1,1,1,1,1,1,0,0,0,0},
        {0,0,0,0,0,1,3,3,3,1,0,0,0,0,0,0},
        {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
    },
};

const uint8_t kDogHappyFrames[3][kDogRows][kDogCols] = {
    {
        {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
        {0,0,0,0,0,0,3,1,1,3,0,0,0,0,0,0},
        {0,0,0,0,0,3,1,2,2,1,3,0,0,0,0,0},
        {0,0,0,0,3,1,2,2,2,2,1,3,0,0,0,0},
        {0,0,0,1,1,2,4,4,4,2,2,1,1,0,0,0},
        {0,0,0,1,2,4,4,4,4,4,2,1,0,0,0,0},
        {0,0,1,1,2,2,2,2,2,2,2,1,1,0,0,0},
        {0,0,1,2,2,2,2,2,2,2,2,2,1,0,0,0},
        {0,0,1,2,2,2,2,2,2,2,2,2,2,1,0,0},
        {0,1,1,2,2,2,2,2,2,2,2,2,2,1,0,0},
        {0,1,2,2,2,2,2,2,2,2,2,2,2,1,0,0},
        {0,1,2,2,2,2,2,2,2,2,2,2,2,1,0,0},
        {0,0,1,1,2,2,2,2,2,2,2,1,1,0,0,0},
        {0,0,0,1,1,1,1,1,1,1,1,1,0,0,0,0},
        {0,0,0,0,0,1,3,3,3,1,0,0,0,0,0,0},
        {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
    },
    {
        {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
        {0,0,0,0,0,0,3,1,1,3,0,0,0,0,0,0},
        {0,0,0,0,0,3,1,2,2,1,3,0,0,0,0,0},
        {0,0,0,0,3,1,2,2,2,2,1,3,0,0,0,0},
        {0,0,0,1,1,2,4,4,4,2,2,1,1,0,0,0},
        {0,0,0,1,2,4,4,4,4,4,2,1,0,0,0,0},
        {0,0,1,1,2,2,2,2,2,2,2,1,1,0,0,0},
        {0,0,1,2,2,2,2,2,2,2,2,2,1,0,0,0},
        {0,0,1,2,2,2,2,2,2,2,2,2,2,1,0,0},
        {0,1,1,2,2,2,2,2,2,2,2,2,2,1,0,0},
        {0,1,2,2,2,2,2,2,2,2,2,2,2,1,0,0},
        {0,1,2,2,2,2,2,2,2,2,2,2,2,1,0,0},
        {0,0,1,1,2,2,2,2,2,2,2,1,1,0,0,0},
        {0,0,0,1,1,1,1,1,1,1,1,1,0,0,0,0},
        {0,0,0,0,0,1,3,3,3,1,0,0,0,0,0,0},
        {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
    },
    {
        {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
        {0,0,0,0,0,0,3,1,1,3,0,0,0,0,0,0},
        {0,0,0,0,0,3,1,2,2,1,3,0,0,0,0,0},
        {0,0,0,0,3,1,2,2,2,2,1,3,0,0,0,0},
        {0,0,0,1,1,2,4,4,4,2,2,1,1,0,0,0},
        {0,0,0,1,2,4,4,4,4,4,2,1,0,0,0,0},
        {0,0,1,1,2,2,2,2,2,2,2,1,1,0,0,0},
        {0,0,1,2,2,2,2,2,2,2,2,2,1,0,0,0},
        {0,0,1,2,2,2,2,2,2,2,2,2,2,1,0,0},
        {0,1,1,2,2,2,2,2,2,2,2,2,2,1,0,0},
        {0,1,2,2,2,2,2,2,2,2,2,2,2,1,0,0},
        {0,1,2,2,2,2,2,2,2,2,2,2,2,1,0,0},
        {0,0,1,1,2,2,2,2,2,2,2,1,1,0,0,0},
        {0,0,0,1,1,1,1,1,1,1,1,1,0,0,0,0},
        {0,0,0,0,0,1,3,3,3,1,0,0,0,0,0,0},
        {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
    },
};

const uint8_t kDogSleepFrames[2][kDogRows][kDogCols] = {
    {
        {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
        {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
        {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
        {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
        {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
        {0,0,0,0,0,0,3,1,1,3,0,0,0,0,0,0},
        {0,0,0,0,0,3,1,2,2,1,3,0,0,0,0,0},
        {0,0,0,0,3,1,2,3,3,2,1,3,0,0,0,0},
        {0,0,0,1,1,2,2,2,2,2,2,1,1,0,0,0},
        {0,0,0,1,2,2,2,2,2,2,2,2,1,0,0,0},
        {0,0,1,1,2,2,2,2,2,2,2,1,1,0,0,0},
        {0,0,1,2,2,2,2,2,2,2,2,2,1,0,0,0},
        {0,0,1,2,2,2,2,2,2,2,2,2,1,0,0,0},
        {0,0,0,1,1,1,1,1,1,1,1,1,0,0,0,0},
        {0,0,0,0,0,1,3,3,3,1,0,0,0,0,0,0},
        {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
    },
    {
        {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
        {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
        {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
        {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
        {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
        {0,0,0,0,0,0,3,1,1,3,0,0,0,0,0,0},
        {0,0,0,0,0,3,1,2,2,1,3,0,0,0,0,0},
        {0,0,0,0,3,1,2,3,3,2,1,3,0,0,0,0},
        {0,0,0,1,1,2,2,2,2,2,2,1,1,0,0,0},
        {0,0,0,1,2,2,2,2,2,2,2,2,1,0,0,0},
        {0,0,1,1,2,2,2,2,2,2,2,1,1,0,0,0},
        {0,0,1,2,2,2,2,2,2,2,2,2,1,0,0,0},
        {0,0,1,2,2,2,2,2,2,2,2,2,1,0,0,0},
        {0,0,0,1,1,1,1,1,1,1,1,1,0,0,0,0},
        {0,0,0,0,0,1,3,3,3,1,0,0,0,0,0,0},
        {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
    },
};

const uint8_t kDogWalkFrames[4][kDogRows][kDogCols] = {
    {
        {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
        {0,0,0,0,0,0,3,1,1,3,0,0,0,0,0,0},
        {0,0,0,0,0,3,1,2,2,1,3,0,0,0,0,0},
        {0,0,0,0,3,1,2,2,2,2,1,3,0,0,0,0},
        {0,0,0,1,1,2,2,1,1,2,2,1,1,0,0,0},
        {0,0,0,1,2,2,2,1,1,2,2,2,1,0,0,0},
        {0,0,1,1,2,2,2,2,2,2,2,1,1,0,0,0},
        {0,1,2,2,2,2,2,2,2,2,2,2,1,1,0,0},
        {0,1,2,2,2,2,2,2,2,2,2,2,2,1,0,0},
        {1,2,2,2,2,2,2,2,2,2,2,2,2,1,1,0},
        {0,2,2,2,2,2,2,2,2,2,2,2,2,2,1,0},
        {0,0,1,2,2,2,2,2,2,2,2,2,2,1,0,0},
        {0,0,0,1,1,2,2,2,2,2,2,1,1,0,0,0},
        {0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,0},
        {0,0,0,0,0,0,1,3,3,1,0,0,0,0,0,0},
        {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
    },
    {
        {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
        {0,0,0,0,0,0,3,1,1,3,0,0,0,0,0,0},
        {0,0,0,0,0,3,1,2,2,1,3,0,0,0,0,0},
        {0,0,0,0,3,1,2,2,2,2,1,3,0,0,0,0},
        {0,0,0,1,1,2,2,1,1,2,2,1,1,0,0,0},
        {0,0,0,1,2,2,2,1,1,2,2,2,1,0,0,0},
        {0,0,1,1,2,2,2,2,2,2,2,1,1,0,0,0},
        {0,0,1,2,2,2,2,2,2,2,2,2,1,0,0,0},
        {0,0,1,2,2,2,2,2,2,2,2,2,1,1,0,0},
        {0,1,2,2,2,2,2,2,2,2,2,2,2,1,0,0},
        {0,1,2,2,2,2,2,2,2,2,2,2,2,1,0,0},
        {0,1,2,2,2,2,2,2,2,2,2,2,2,1,0,0},
        {0,0,1,1,2,2,2,2,2,2,2,1,1,0,0,0},
        {0,0,0,1,1,1,1,1,1,1,1,1,0,0,0,0},
        {0,0,0,0,0,1,3,3,3,1,0,0,0,0,0,0},
        {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
    },
    {
        {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
        {0,0,0,0,0,0,3,1,1,3,0,0,0,0,0,0},
        {0,0,0,0,0,3,1,2,2,1,3,0,0,0,0,0},
        {0,0,0,0,3,1,2,2,2,2,1,3,0,0,0,0},
        {0,0,0,1,1,2,2,1,1,2,2,1,1,0,0,0},
        {0,0,0,1,2,2,2,1,1,2,2,2,1,0,0,0},
        {0,0,1,1,2,2,2,2,2,2,2,1,1,0,0,0},
        {0,1,2,2,2,2,2,2,2,2,2,2,1,1,0,0},
        {0,1,2,2,2,2,2,2,2,2,2,2,2,1,0,0},
        {1,2,2,2,2,2,2,2,2,2,2,2,2,1,1,0},
        {0,2,2,2,2,2,2,2,2,2,2,2,2,2,1,0},
        {0,0,1,2,2,2,2,2,2,2,2,2,2,1,0,0},
        {0,0,0,1,1,2,2,2,2,2,2,1,1,0,0,0},
        {0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,0},
        {0,0,0,0,0,0,1,3,3,1,0,0,0,0,0,0},
        {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
    },
    {
        {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
        {0,0,0,0,0,0,3,1,1,3,0,0,0,0,0,0},
        {0,0,0,0,0,3,1,2,2,1,3,0,0,0,0,0},
        {0,0,0,0,3,1,2,2,2,2,1,3,0,0,0,0},
        {0,0,0,1,1,2,2,1,1,2,2,1,1,0,0,0},
        {0,0,0,1,2,2,2,1,1,2,2,2,1,0,0,0},
        {0,0,1,1,2,2,2,2,2,2,2,1,1,0,0,0},
        {0,0,1,2,2,2,2,2,2,2,2,2,1,0,0,0},
        {0,0,1,2,2,2,2,2,2,2,2,2,1,1,0,0},
        {0,1,2,2,2,2,2,2,2,2,2,2,2,1,0,0},
        {0,1,2,2,2,2,2,2,2,2,2,2,2,1,0,0},
        {0,1,2,2,2,2,2,2,2,2,2,2,2,1,0,0},
        {0,0,1,1,2,2,2,2,2,2,2,1,1,0,0,0},
        {0,0,0,1,1,1,1,1,1,1,1,1,0,0,0,0},
        {0,0,0,0,0,1,3,3,3,1,0,0,0,0,0,0},
        {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
    },
};

const uint8_t kDogPettingFrames[3][kDogRows][kDogCols] = {
    {
        {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
        {0,0,0,0,0,3,1,1,1,3,0,0,0,0,0,0},
        {0,0,0,0,3,1,2,2,2,1,3,0,0,0,0,0},
        {0,0,0,3,1,2,2,4,4,2,1,3,0,0,0,0},
        {0,0,1,1,2,2,4,4,4,4,2,1,1,0,0,0},
        {0,0,1,2,2,4,4,4,4,4,2,2,1,0,0,0},
        {0,0,1,1,2,2,2,2,2,2,2,1,1,0,0,0},
        {0,0,1,2,2,2,2,2,2,2,2,2,1,0,0,0},
        {0,0,1,2,2,2,2,2,2,2,2,2,1,1,0,0},
        {0,1,1,2,2,2,2,2,2,2,2,2,2,1,0,0},
        {0,1,2,2,2,2,2,2,2,2,2,2,2,1,0,0},
        {0,1,2,2,2,2,2,2,2,2,2,2,2,1,0,0},
        {0,0,1,1,2,2,2,2,2,2,2,1,1,0,0,0},
        {0,0,0,1,1,1,1,1,1,1,1,1,0,0,0,0},
        {0,0,0,0,0,1,3,3,3,1,0,0,0,0,0,0},
        {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
    },
    {
        {0,0,0,0,0,0,3,1,1,3,0,0,0,0,0,0},
        {0,0,0,0,0,3,1,2,2,1,3,0,0,0,0,0},
        {0,0,0,0,3,1,2,2,2,2,1,3,0,0,0,0},
        {0,0,0,1,1,2,4,4,4,2,2,1,1,0,0,0},
        {0,0,0,1,2,4,4,4,4,4,2,1,0,0,0,0},
        {0,0,1,1,2,2,2,2,2,2,2,1,1,0,0,0},
        {0,0,1,2,2,2,2,2,2,2,2,2,1,0,0,0},
        {0,0,1,2,2,2,2,2,2,2,2,2,1,1,0,0},
        {0,1,1,2,2,2,2,2,2,2,2,2,2,1,0,0},
        {0,1,2,2,2,2,2,2,2,2,2,2,2,1,0,0},
        {0,1,2,2,2,2,2,2,2,2,2,2,2,1,0,0},
        {0,0,1,1,2,2,2,2,2,2,2,1,1,0,0,0},
        {0,0,0,1,1,1,1,1,1,1,1,1,0,0,0,0},
        {0,0,0,0,0,1,3,3,3,1,0,0,0,0,0,0},
        {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
    },
    {
        {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
        {0,0,0,0,0,3,1,1,1,3,0,0,0,0,0,0},
        {0,0,0,0,3,1,2,2,2,1,3,0,0,0,0,0},
        {0,0,0,3,1,2,2,4,4,2,1,3,0,0,0,0},
        {0,0,1,1,2,2,4,4,4,4,2,1,1,0,0,0},
        {0,0,1,2,2,4,4,4,4,4,2,2,1,0,0,0},
        {0,0,1,1,2,2,2,2,2,2,2,1,1,0,0,0},
        {0,0,1,2,2,2,2,2,2,2,2,2,1,0,0,0},
        {0,0,1,2,2,2,2,2,2,2,2,2,1,1,0,0},
        {0,1,1,2,2,2,2,2,2,2,2,2,2,1,0,0},
        {0,1,2,2,2,2,2,2,2,2,2,2,2,1,0,0},
        {0,1,2,2,2,2,2,2,2,2,2,2,2,1,0,0},
        {0,0,1,1,2,2,2,2,2,2,2,1,1,0,0,0},
        {0,0,0,1,1,1,1,1,1,1,1,1,0,0,0,0},
        {0,0,0,0,0,1,3,3,3,1,0,0,0,0,0,0},
        {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
    },
};

const uint8_t kDogFocusFrames[2][kDogRows][kDogCols] = {
    {
        {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
        {0,0,0,0,0,0,3,1,1,3,0,0,0,0,0,0},
        {0,0,0,0,0,3,1,2,2,1,3,0,0,0,0,0},
        {0,0,0,0,3,1,2,2,2,2,1,3,0,0,0,0},
        {0,0,0,1,1,2,1,1,1,1,2,1,1,0,0,0},   // brown eyes open
        {0,0,0,1,2,2,1,1,1,1,2,2,1,0,0,0},   // brown eyes open
        {0,0,1,1,2,2,2,2,2,2,2,1,1,0,0,0},
        {0,0,1,2,2,2,2,2,2,2,2,2,1,0,0,0},
        {0,0,1,2,2,2,2,2,2,2,2,2,1,1,0,0},
        {0,1,1,2,2,2,2,2,2,2,2,2,2,1,0,0},
        {0,1,2,2,2,2,2,2,2,2,2,2,2,1,0,0},
        {0,1,2,2,2,2,2,2,2,2,2,2,2,1,0,0},
        {0,0,1,1,2,2,2,2,2,2,2,1,1,0,0,0},
        {0,0,0,1,1,1,1,1,1,1,1,1,0,0,0,0},
        {0,0,0,0,0,1,3,3,3,1,0,0,0,0,0,0},
        {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
    },
    {
        {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
        {0,0,0,0,0,0,3,1,1,3,0,0,0,0,0,0},
        {0,0,0,0,0,3,1,2,2,1,3,0,0,0,0,0},
        {0,0,0,0,3,1,2,2,2,2,1,3,0,0,0,0},
        {0,0,0,1,1,2,2,2,2,2,2,1,1,0,0,0},   // eyes closed (white)
        {0,0,0,1,2,2,2,2,2,2,2,2,1,0,0,0},   // eyes closed (white)
        {0,0,1,1,2,2,2,2,2,2,2,1,1,0,0,0},
        {0,0,1,2,2,2,2,2,2,2,2,2,1,0,0,0},
        {0,0,1,2,2,2,2,2,2,2,2,2,1,1,0,0},
        {0,1,1,2,2,2,2,2,2,2,2,2,2,1,0,0},
        {0,1,2,2,2,2,2,2,2,2,2,2,2,1,0,0},
        {0,1,2,2,2,2,2,2,2,2,2,2,2,1,0,0},
        {0,0,1,1,2,2,2,2,2,2,2,1,1,0,0,0},
        {0,0,0,1,1,1,1,1,1,1,1,1,0,0,0,0},
        {0,0,0,0,0,1,3,3,3,1,0,0,0,0,0,0},
        {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
    },
};

const uint8_t kDogGreetingFrames[4][kDogRows][kDogCols] = {
    {
        {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
        {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
        {0,0,0,0,0,0,3,1,1,3,0,0,0,0,0,0},
        {0,0,0,0,0,3,1,2,2,1,3,0,0,0,0,0},
        {0,0,0,0,3,1,2,4,4,2,1,3,0,0,0,0},
        {0,0,0,1,1,2,4,4,4,4,2,1,1,0,0,0},
        {0,0,1,1,2,2,2,2,2,2,2,1,1,0,0,0},
        {0,0,1,2,2,2,2,2,2,2,2,2,1,0,0,0},
        {0,0,1,2,2,2,2,2,2,2,2,2,1,1,0,0},
        {0,1,1,2,2,2,2,2,2,2,2,2,2,1,0,0},
        {0,1,2,2,2,2,2,2,2,2,2,2,2,1,0,0},
        {0,1,2,2,2,2,2,2,2,2,2,2,2,1,0,0},
        {0,0,1,1,2,2,2,2,2,2,2,1,1,0,0,0},
        {0,0,0,1,1,1,1,1,1,1,1,1,0,0,0,0},
        {0,0,0,0,0,1,3,3,3,1,0,0,0,0,0,0},
        {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
    },
    {
        {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
        {0,0,0,0,0,3,1,1,1,3,0,0,0,0,0,0},
        {0,0,0,0,3,1,2,2,2,1,3,0,0,0,0,0},
        {0,0,0,3,1,2,4,4,4,2,1,3,0,0,0,0},
        {0,0,1,1,2,4,4,4,4,4,2,1,1,0,0,0},
        {0,0,1,2,2,2,2,2,2,2,2,2,1,0,0,0},
        {0,0,1,1,2,2,2,2,2,2,2,1,1,0,0,0},
        {0,0,1,2,2,2,2,2,2,2,2,2,1,0,0,0},
        {0,0,1,2,2,2,2,2,2,2,2,2,1,1,0,0},
        {0,1,1,2,2,2,2,2,2,2,2,2,2,1,0,0},
        {0,1,2,2,2,2,2,2,2,2,2,2,2,1,0,0},
        {0,1,2,2,2,2,2,2,2,2,2,2,2,1,0,0},
        {0,0,1,1,2,2,2,2,2,2,2,1,1,0,0,0},
        {0,0,0,1,1,1,1,1,1,1,1,1,0,0,0,0},
        {0,0,0,0,0,1,3,3,3,1,0,0,0,0,0,0},
        {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
    },
    {
        {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
        {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
        {0,0,0,0,0,0,3,1,1,3,0,0,0,0,0,0},
        {0,0,0,0,0,3,1,2,2,1,3,0,0,0,0,0},
        {0,0,0,0,3,1,2,4,4,2,1,3,0,0,0,0},
        {0,0,0,1,1,2,4,4,4,4,2,1,1,0,0,0},
        {0,0,1,1,2,2,2,2,2,2,2,1,1,0,0,0},
        {0,0,1,2,2,2,2,2,2,2,2,2,1,0,0,0},
        {0,0,1,2,2,2,2,2,2,2,2,2,1,1,0,0},
        {0,1,1,2,2,2,2,2,2,2,2,2,2,1,0,0},
        {0,1,2,2,2,2,2,2,2,2,2,2,2,1,0,0},
        {0,1,2,2,2,2,2,2,2,2,2,2,2,1,0,0},
        {0,0,1,1,2,2,2,2,2,2,2,1,1,0,0,0},
        {0,0,0,1,1,1,1,1,1,1,1,1,0,0,0,0},
        {0,0,0,0,0,1,3,3,3,1,0,0,0,0,0,0},
        {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
    },
    {
        {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
        {0,0,0,0,0,3,1,1,1,3,0,0,0,0,0,0},
        {0,0,0,0,3,1,2,2,2,1,3,0,0,0,0,0},
        {0,0,0,3,1,2,4,4,4,2,1,3,0,0,0,0},
        {0,0,1,1,2,4,4,4,4,4,2,1,1,0,0,0},
        {0,0,1,2,2,2,2,2,2,2,2,2,1,0,0,0},
        {0,0,1,1,2,2,2,2,2,2,2,1,1,0,0,0},
        {0,0,1,2,2,2,2,2,2,2,2,2,1,0,0,0},
        {0,0,1,2,2,2,2,2,2,2,2,2,1,1,0,0},
        {0,1,1,2,2,2,2,2,2,2,2,2,2,1,0,0},
        {0,1,2,2,2,2,2,2,2,2,2,2,2,1,0,0},
        {0,1,2,2,2,2,2,2,2,2,2,2,2,1,0,0},
        {0,0,1,1,2,2,2,2,2,2,2,1,1,0,0,0},
        {0,0,0,1,1,1,1,1,1,1,1,1,0,0,0,0},
        {0,0,0,0,0,1,3,3,3,1,0,0,0,0,0,0},
        {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
    },
};

lv_color_t dog_color(uint8_t id) {
    switch (id) {
        case 1:
            return lv_color_hex(0x8d6e63);
        case 2:
            return lv_color_hex(0xffffff);
        case 3:
            return lv_color_hex(0x4e342e);
        case 4:
            return lv_color_hex(0xff5252);
        case 5: {
            lv_color_t c = lv_color_hex(0x00ff00);   // bright green for focus eyes (unmistakable)
            ESP_LOGI(kSpriteTag, "dog_color(5) = R=%d G=%d B=%d", c.red, c.green, c.blue);
            return c;
        }
        default:
            return lv_color_hex(0x000000);
    }
}

// --- Item pixel grids (matching PC side itemGrids) ---
struct DogItemGrid {
    uint8_t require_level;  // 解锁所需等级
    uint8_t rows;
    uint8_t cols;
    uint8_t offset_col;
    uint8_t offset_row;
    uint32_t color_hex;
    const uint8_t *data;
};

// 形状模板（与 PC 端 pixel-dog-items.ts 完全一致）
static const uint8_t kShapeBow[] = {
    0,1,0,0,1,0,
    1,1,1,1,1,1,
    0,1,0,0,1,0,
};
static const uint8_t kShapeHat[] = {
    0,0,1,1,0,0,
    0,1,1,1,1,0,
    1,1,1,1,1,1,
};
static const uint8_t kShapeGlasses[] = {
    1,1,1,0,0,1,1,1,
    1,0,1,1,1,1,0,1,
    0,0,0,1,1,0,0,0,
};
static const uint8_t kShapeCollar[] = {
    0,0,1,0,1,0,1,0,0,
    0,1,0,1,1,1,0,1,0,
};
static const uint8_t kShapeCrown[] = {
    1,0,1,0,1,0,1,
    1,1,1,1,1,1,1,
    1,1,1,1,1,1,1,
};
static const uint8_t kShapeStar[] = {
    0,0,1,0,0,
    0,1,1,1,0,
    1,0,1,0,1,
};
static const uint8_t kShapeHeart[] = {
    0,1,0,1,0,
    1,1,1,1,1,
    0,1,1,1,0,
};
static const uint8_t kShapeCape[] = {
    1,1,1,1,
    1,0,0,1,
    1,0,0,1,
};
static const uint8_t kShapeRing[] = {
    1,1,1,1,1,1,
};
static const uint8_t kShapeDot[] = {
    1,0,1,
    0,1,0,
};

static const DogItemGrid kDogItems[] = {
    {2,  3, 6, 5, 0, 0xff69b4, kShapeBow},       // 蝴蝶结
    {3,  3, 8, 4, 4, 0xea580c, kShapeGlasses},    // 墨镜
    {4,  3, 6, 5, 0, 0x7c3aed, kShapeHat},        // 小礼帽
    {5,  2, 9, 4, 12, 0xffd700, kShapeCollar},    // 星星项圈
    {6,  3, 7, 5, 0, 0xffd700, kShapeCrown},      // 金皇冠
    {7,  3, 5, 11, 0, 0xfbbf24, kShapeStar},      // 小星星
    {8,  3, 5, 12, 2, 0xec4899, kShapeHeart},     // 粉爱心
    {9,  3, 8, 4, 4, 0x3b82f6, kShapeGlasses},    // 蓝眼镜
    {10, 2, 9, 4, 12, 0xfbbf24, kShapeCollar},    // 金铃铛
    {11, 3, 4, 10, 5, 0xdc2626, kShapeCape},      // 红披风
    {12, 1, 6, 5, 0, 0xfbbf24, kShapeRing},       // 金光环
    {13, 2, 3, 12, 1, 0xf97316, kShapeDot},       // 火苗
    {14, 3, 6, 5, 0, 0x6366f1, kShapeHat},        // 魔法帽
    {15, 3, 6, 5, 0, 0xf472b6, kShapeBow},        // 粉蝶结
    {16, 3, 7, 5, 0, 0xe5e7eb, kShapeCrown},      // 银皇冠
    {17, 3, 5, 11, 0, 0x3b82f6, kShapeStar},      // 蓝星
    {18, 3, 8, 4, 4, 0xf59e0b, kShapeGlasses},    // 金丝镜
    {19, 3, 5, 12, 2, 0x3b82f6, kShapeHeart},     // 蓝爱心
    {20, 2, 9, 4, 12, 0x06b6d4, kShapeCollar},    // 钻石链
    {21, 3, 4, 10, 5, 0x2563eb, kShapeCape},      // 蓝披风
    {22, 3, 6, 5, 0, 0xec4899, kShapeBow},        // 粉丝带
    {23, 2, 3, 12, 1, 0x60a5fa, kShapeDot},       // 冰晶
    {24, 3, 6, 5, 0, 0x1f2937, kShapeHat},        // 绅士帽
    {25, 3, 7, 5, 0, 0xdc2626, kShapeCrown},      // 红宝石冠
    {26, 1, 6, 5, 0, 0xf3f4f6, kShapeRing},       // 云朵
    {27, 3, 5, 11, 0, 0xec4899, kShapeStar},      // 粉星
    {28, 2, 9, 4, 12, 0xdc2626, kShapeCollar},    // 红围巾
    {29, 3, 5, 12, 2, 0xfbbf24, kShapeHeart},     // 金爱心
    {30, 3, 4, 10, 5, 0x16a34a, kShapeCape},      // 绿披风
    {31, 1, 6, 5, 0, 0xe5e7eb, kShapeRing},       // 银光环
    {32, 2, 3, 12, 1, 0xfbbf24, kShapeDot},       // 闪电
    {33, 3, 6, 5, 0, 0x92400e, kShapeHat},        // 复古帽
    {34, 3, 7, 5, 0, 0x10b981, kShapeCrown},      // 翠玉冠
    {35, 2, 3, 12, 1, 0xe5e7eb, kShapeDot},       // 月亮
    {36, 3, 5, 11, 0, 0x22c55e, kShapeStar},      // 绿星
    {37, 3, 6, 5, 0, 0x2563eb, kShapeBow},        // 蓝领结
    {38, 3, 8, 4, 4, 0xef4444, kShapeGlasses},    // 红色镜
    {39, 3, 5, 12, 2, 0x10b981, kShapeHeart},     // 绿爱心
    {40, 3, 4, 10, 5, 0x7c3aed, kShapeCape},      // 紫披风
    {41, 2, 9, 4, 12, 0xa855f7, kShapeCollar},    // 紫水晶
    {42, 3, 6, 5, 0, 0x4f46e5, kShapeHat},        // 星辰帽
    {43, 2, 3, 12, 1, 0x22d3ee, kShapeDot},       // 泡泡
    {44, 3, 7, 5, 0, 0x8b5cf6, kShapeCrown},      // 紫晶冠
    {45, 3, 5, 11, 0, 0xf59e0b, kShapeStar},      // 太阳
    {46, 3, 6, 5, 0, 0xef4444, kShapeBow},        // 红丝带
    {47, 3, 5, 11, 0, 0xef4444, kShapeStar},      // 红星
    {48, 3, 8, 4, 4, 0x8b5cf6, kShapeGlasses},    // 紫色镜
    {49, 3, 5, 12, 2, 0xa855f7, kShapeHeart},     // 紫爱心
    {50, 2, 3, 12, 1, 0xf472b6, kShapeDot},       // 闪光
    {55, 3, 7, 5, 0, 0x06b6d4, kShapeCrown},      // 钻石冠
};
constexpr int kDogItemCount = sizeof(kDogItems) / sizeof(kDogItems[0]);

void draw_dog_items(lv_obj_t *canvas, uint64_t equipped_items, int pixel) {
    if (canvas == nullptr) return;
    lv_draw_buf_t *db = lv_canvas_get_draw_buf(canvas);
    if (db == nullptr || db->data == nullptr) return;

    const int stride = db->header.stride;
    auto *base = static_cast<uint8_t *>(db->data);

    for (int i = 0; i < kDogItemCount; i++) {
        const DogItemGrid &item = kDogItems[i];
        if ((equipped_items & (1ULL << i)) == 0) continue;

        const lv_color32_t px = lv_color_to_32(lv_color_hex(item.color_hex), LV_OPA_COVER);

        for (int ri = 0; ri < item.rows; ri++) {
            for (int ci = 0; ci < item.cols; ci++) {
                if (item.data[ri * item.cols + ci] == 0) continue;

                const int start_x = (item.offset_col + ci) * pixel;
                const int start_y = (item.offset_row + ri) * pixel;

                for (int pr = 0; pr < pixel; pr++) {
                    for (int pc = 0; pc < pixel; pc++) {
                        const int x = start_x + pc;
                        const int y = start_y + pr;
                        if (x >= 0 && y >= 0) {
                            auto *line = reinterpret_cast<lv_color32_t *>(base + y * stride);
                            line[x] = px;
                        }
                    }
                }
            }
        }
    }
}

struct DogSpriteData {
    lv_obj_t *canvas = nullptr;
    lv_color32_t *buf = nullptr;
    DogStatus current_status = DOG_STATUS_IDLE;
    int current_frame = 0;
    int pixel_scale = 4;
    int level = 1;
    // Random walk state
    int walk_x = 0;
    int walk_y = 0;
    int walk_target_x = 0;
    int walk_target_y = 0;
    int walk_hold = 0;
};

static DogSpriteData s_sprite = {};
static lv_timer_t *s_timer = nullptr;

float dog_level_scale(int level) {
    float scale = 1.0f + (level - 1) * 0.08f;
    if (scale > 2.0f) scale = 2.0f;
    return scale;
}

float dog_separation(int level) {
    float sep = (level - 1) * 0.5f;
    if (sep > 4.0f) sep = 4.0f;
    return sep;
}

int get_render_x(int col, int row, int level) {
    float sep = dog_separation(level);
    float offset = 0.0f;
    
    if (row == 1 && (col == 6 || col == 9)) {
        offset = -sep * (col == 6 ? 0.8f : -0.8f);
    } else if (row == 2 && (col == 5 || col == 10)) {
        offset = -sep * (col == 5 ? 0.6f : -0.6f);
    } else if ((row == 3 || row == 4) && (col == 6 || col == 9)) {
        offset = -sep * (col == 6 ? 0.4f : -0.4f);
    } else if (row == 4 && (col == 7 || col == 8)) {
        offset = sep * (col == 7 ? -0.3f : 0.3f);
    } else if (row == 5 && (col == 7 || col == 8)) {
        offset = sep * (col == 7 ? -0.2f : 0.2f);
    }
    
    return (int)((col + offset) * s_sprite.pixel_scale);
}

int get_render_y(int row, int col, int level) {
    float sep = dog_separation(level);
    float offset = 0.0f;
    
    if (row == 1 && (col == 6 || col == 9)) {
        offset = -sep * 1.2f;
    } else if (row == 2 && (col == 5 || col == 10)) {
        offset = -sep * 0.8f;
    } else if (row == 4 && col == 7) {
        offset = sep * 0.3f;
    } else if (row == 13 && col <= 3) {
        offset = sep * 0.5f;
    } else if (row == 14 && (col == 5 || col == 6 || col == 7 || col == 8 || col == 9)) {
        if (col == 5) offset = sep * 1.2f;
        else if (col == 6) offset = sep * 0.8f;
        else if (col == 7 || col == 8) offset = sep * 0.3f;
        else if (col == 9) offset = sep * 0.8f;
    }
    
    return (int)((row + offset) * s_sprite.pixel_scale);
}

void draw_dog_frame(lv_obj_t *canvas, const uint8_t (*frame)[kDogRows][kDogCols], int pixel, int level) {
    if (canvas == nullptr || frame == nullptr) return;

    lv_draw_buf_t *db = lv_canvas_get_draw_buf(canvas);
    if (db == nullptr || db->data == nullptr) return;

    const int w = kDogCols * pixel;
    const int h = kDogRows * pixel;
    const int stride = db->header.stride;
    auto *base = static_cast<uint8_t *>(db->data);

    for (int r = 0; r < kDogRows; r++) {
        for (int c = 0; c < kDogCols; c++) {
            const uint8_t v = (*frame)[r][c];
            if (v == 0) continue;

            const lv_color_t color = dog_color(v);
            const lv_color32_t px = lv_color_to_32(color, LV_OPA_COVER);

            const int start_x = get_render_x(c, r, level);
            const int start_y = get_render_y(r, c, level);

            for (int pr = 0; pr < pixel; pr++) {
                for (int pc = 0; pc < pixel; pc++) {
                    const int x = start_x + pc;
                    const int y = start_y + pr;
                    if (x >= 0 && x < w && y >= 0 && y < h) {
                        auto *line = reinterpret_cast<lv_color32_t *>(base + y * stride);
                        line[x] = px;
                    }
                }
            }
        }
    }

    lv_obj_invalidate(canvas);
}

void clear_dog_canvas(lv_obj_t *canvas) {
    if (canvas == nullptr) return;
    lv_draw_buf_t *db = lv_canvas_get_draw_buf(canvas);
    if (db != nullptr && db->data != nullptr && db->data_size > 0) {
        std::memset(db->data, 0, db->data_size);
    }
}

void dog_sprite_timer_cb(lv_timer_t *timer) {
    (void)timer;
    // Copy by value immediately to avoid race with sync task overwriting the static buffer
    const DogState local_state = *dog_model_get();
    const DogState *state = &local_state;
    if (s_sprite.canvas == nullptr || s_sprite.buf == nullptr) return;

    if (state->status != s_sprite.current_status) {
        ESP_LOGI(kSpriteTag, "status change: %d -> %d", s_sprite.current_status, state->status);
        s_sprite.current_status = state->status;
        s_sprite.current_frame = 0;
        // Reset walk state when entering walking mode
        if (state->status == DOG_STATUS_IDLE || state->status == DOG_STATUS_WALKING) {
            s_sprite.walk_x = 0;
            s_sprite.walk_y = 0;
            s_sprite.walk_target_x = 0;
            s_sprite.walk_target_y = 0;
            s_sprite.walk_hold = 0;
            lv_obj_center(s_sprite.canvas);
        }
    }

    if (state->status == DOG_STATUS_FOCUS) {
        ESP_LOGD(kSpriteTag, "Focus active, drawing frame %d", s_sprite.current_frame);
    }

    if (state->level != s_sprite.level) {
        s_sprite.level = state->level;
        float scale = dog_level_scale(state->level);
        s_sprite.pixel_scale = (int)(4 * scale);
        if (s_sprite.pixel_scale < 2) s_sprite.pixel_scale = 2;
        if (s_sprite.pixel_scale > 12) s_sprite.pixel_scale = 12;
    }

    const int pixel = s_sprite.pixel_scale;
    const int w = kDogCols * pixel;
    const int h = kDogRows * pixel;

    lv_canvas_set_buffer(s_sprite.canvas, s_sprite.buf, w, h, LV_COLOR_FORMAT_ARGB8888);
    lv_obj_set_size(s_sprite.canvas, w, h);
    clear_dog_canvas(s_sprite.canvas);

    const int level = s_sprite.level;
    const int frame_idx = s_sprite.current_frame;

    switch (s_sprite.current_status) {
        case DOG_STATUS_IDLE:
        case DOG_STATUS_WALKING: {
            // Random walk: step towards target, pick new target every ~3s
            constexpr int kStepX = 15;
            constexpr int kStepY = 10;
            constexpr int kHoldMax = 6;

            // Compute safe walk bounds so the full canvas is always visible in parent
            int range_x = 20;
            int range_y = 15;
            lv_obj_t *parent = lv_obj_get_parent(s_sprite.canvas);
            if (parent != nullptr) {
                int pw = lv_obj_get_width(parent);
                int ph = lv_obj_get_height(parent);
                int margin_x = (pw - w) / 2;
                int margin_y = (ph - h) / 2;
                if (margin_x > 5) range_x = margin_x - 5;
                if (margin_y > 5) range_y = margin_y - 5;
            }

            s_sprite.walk_hold++;
            if (s_sprite.walk_hold >= kHoldMax) {
                s_sprite.walk_hold = 0;
                s_sprite.walk_target_x = (rand() % (range_x * 2 + 1)) - range_x;
                s_sprite.walk_target_y = (rand() % (range_y * 2 + 1)) - range_y;
            }

            // Step towards target
            int dx = s_sprite.walk_target_x - s_sprite.walk_x;
            int dy = s_sprite.walk_target_y - s_sprite.walk_y;
            if (dx > kStepX) s_sprite.walk_x += kStepX;
            else if (dx < -kStepX) s_sprite.walk_x -= kStepX;
            else s_sprite.walk_x = s_sprite.walk_target_x;
            if (dy > kStepY) s_sprite.walk_y += kStepY;
            else if (dy < -kStepY) s_sprite.walk_y -= kStepY;
            else s_sprite.walk_y = s_sprite.walk_target_y;

            lv_obj_set_pos(s_sprite.canvas, s_sprite.walk_x, s_sprite.walk_y);
            draw_dog_frame(s_sprite.canvas, &kDogWalkFrames[frame_idx], pixel, level);
            draw_dog_items(s_sprite.canvas, state->equipped_items, pixel);
            s_sprite.current_frame = (s_sprite.current_frame + 1) % 4;
            break;
        }
        case DOG_STATUS_HAPPY:
            lv_obj_center(s_sprite.canvas);
            draw_dog_frame(s_sprite.canvas, &kDogHappyFrames[frame_idx], pixel, level);
            draw_dog_items(s_sprite.canvas, state->equipped_items, pixel);
            s_sprite.current_frame = (s_sprite.current_frame + 1) % 3;
            break;
        case DOG_STATUS_PETTING:
            lv_obj_center(s_sprite.canvas);
            draw_dog_frame(s_sprite.canvas, &kDogPettingFrames[frame_idx], pixel, level);
            draw_dog_items(s_sprite.canvas, state->equipped_items, pixel);
            s_sprite.current_frame = (s_sprite.current_frame + 1) % 3;
            break;
        case DOG_STATUS_GREETING:
            lv_obj_center(s_sprite.canvas);
            draw_dog_frame(s_sprite.canvas, &kDogGreetingFrames[frame_idx], pixel, level);
            draw_dog_items(s_sprite.canvas, state->equipped_items, pixel);
            s_sprite.current_frame = (s_sprite.current_frame + 1) % 4;
            break;
        case DOG_STATUS_FOCUS:
            lv_obj_center(s_sprite.canvas);
            ESP_LOGD(kSpriteTag, "Drawing Focus frame %d (pixel=%d level=%d)", frame_idx, pixel, level);
            draw_dog_frame(s_sprite.canvas, &kDogFocusFrames[frame_idx], pixel, level);
            draw_dog_items(s_sprite.canvas, state->equipped_items, pixel);
            s_sprite.current_frame = (s_sprite.current_frame + 1) % 2;
            break;
        case DOG_STATUS_SLEEPING:
            lv_obj_center(s_sprite.canvas);
            draw_dog_frame(s_sprite.canvas, &kDogSleepFrames[frame_idx], pixel, level);
            draw_dog_items(s_sprite.canvas, state->equipped_items, pixel);
            s_sprite.current_frame = (s_sprite.current_frame + 1) % 2;
            break;
        default:
            lv_obj_center(s_sprite.canvas);
            draw_dog_frame(s_sprite.canvas, &kDogIdleFrames[frame_idx], pixel, level);
            draw_dog_items(s_sprite.canvas, state->equipped_items, pixel);
            s_sprite.current_frame = (s_sprite.current_frame + 1) % 2;
            break;
    }

    // Force canvas redraw and display refresh
    lv_obj_invalidate(s_sprite.canvas);
    lv_refr_now(nullptr);
}
}  // namespace

extern "C" {

void dog_sprite_init(void) {
    memset(&s_sprite, 0, sizeof(s_sprite));
}

lv_obj_t *dog_sprite_create(lv_obj_t *parent, int pixel_scale) {
    // Seed random walk once per boot
    static bool seeded = false;
    if (!seeded) {
        seeded = true;
        srand(static_cast<unsigned>(esp_log_timestamp()));
    }

    const int w = kDogCols * pixel_scale;
    const int h = kDogRows * pixel_scale;
    const size_t buf_size = w * h * sizeof(lv_color32_t);

    if (s_sprite.buf == nullptr) {
        s_sprite.buf = static_cast<lv_color32_t *>(heap_caps_malloc(buf_size, MALLOC_CAP_SPIRAM | MALLOC_CAP_8BIT));
        if (s_sprite.buf == nullptr) {
            s_sprite.buf = static_cast<lv_color32_t *>(heap_caps_malloc(buf_size, MALLOC_CAP_8BIT));
        }
    }

    s_sprite.canvas = lv_canvas_create(parent);
    if (s_sprite.buf != nullptr) {
        lv_canvas_set_buffer(s_sprite.canvas, s_sprite.buf, w, h, LV_COLOR_FORMAT_ARGB8888);
    }
    lv_obj_set_size(s_sprite.canvas, w, h);
    lv_obj_set_style_bg_opa(s_sprite.canvas, LV_OPA_TRANSP, 0);
    lv_obj_set_style_border_width(s_sprite.canvas, 0, 0);
    lv_obj_remove_flag(s_sprite.canvas, LV_OBJ_FLAG_CLICKABLE);
    lv_obj_remove_flag(s_sprite.canvas, LV_OBJ_FLAG_SCROLLABLE);

    s_sprite.pixel_scale = pixel_scale;
    s_sprite.current_status = DOG_STATUS_IDLE;
    s_sprite.current_frame = 0;
    s_sprite.level = 1;

    if (s_timer == nullptr) {
        s_timer = lv_timer_create(dog_sprite_timer_cb, 500, nullptr);
    }

    if (s_sprite.buf != nullptr) {
        clear_dog_canvas(s_sprite.canvas);
        draw_dog_frame(s_sprite.canvas, &kDogIdleFrames[0], pixel_scale, 1);
    }

    return s_sprite.canvas;
}

void dog_sprite_update(void) {
    if (s_timer != nullptr) {
        lv_timer_reset(s_timer);
    }
}

void dog_sprite_set_scale(int pixel_scale) {
    s_sprite.pixel_scale = pixel_scale;
    if (s_sprite.canvas != nullptr && s_sprite.buf != nullptr) {
        const int w = kDogCols * pixel_scale;
        const int h = kDogRows * pixel_scale;
        lv_canvas_set_buffer(s_sprite.canvas, s_sprite.buf, w, h, LV_COLOR_FORMAT_ARGB8888);
        lv_obj_set_size(s_sprite.canvas, w, h);
    }
}

void dog_sprite_set_status(DogStatus status) {
    s_sprite.current_status = status;
    s_sprite.current_frame = 0;
}

void dog_sprite_set_level(int level) {
    s_sprite.level = level;
    float scale = dog_level_scale(level);
    s_sprite.pixel_scale = (int)(4 * scale);
    if (s_sprite.pixel_scale < 2) s_sprite.pixel_scale = 2;
    if (s_sprite.pixel_scale > 12) s_sprite.pixel_scale = 12;
}

}  // extern "C"
