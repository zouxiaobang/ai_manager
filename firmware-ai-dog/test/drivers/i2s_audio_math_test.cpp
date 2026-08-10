#include <cstdint>
#include <limits>

#include "drivers/i2s_audio_math.h"
#include "unity.h"

using namespace kyle;

// ---------- OutputVolumeFactor ----------

TEST_CASE("Volume factor is zero at mute and clamps negatives", "[i2s_audio_math]") {
    TEST_ASSERT_EQUAL_INT32(0, OutputVolumeFactor(0));
    TEST_ASSERT_EQUAL_INT32(0, OutputVolumeFactor(-5));
}

TEST_CASE("Volume factor exact at power-of-two volumes", "[i2s_audio_math]") {
    // 100%: 1.0*65536*4=262144；50%: 0.25*262144=65536；25%: 0.0625*262144=16384
    TEST_ASSERT_EQUAL_INT32(262144, OutputVolumeFactor(100));
    TEST_ASSERT_EQUAL_INT32(65536, OutputVolumeFactor(50));
    TEST_ASSERT_EQUAL_INT32(16384, OutputVolumeFactor(25));
}

TEST_CASE("Volume factor clamps above 100", "[i2s_audio_math]") {
    TEST_ASSERT_EQUAL_INT32(262144, OutputVolumeFactor(150));
}

TEST_CASE("Volume factor default 70 near expected", "[i2s_audio_math]") {
    // pow(0.7,2)=0.49 → 0.49*262144≈128450，浮点误差容许 ±1
    const int32_t f = OutputVolumeFactor(70);
    TEST_ASSERT_TRUE(f >= 128449 && f <= 128451);
}

TEST_CASE("Volume factor monotonic non-decreasing", "[i2s_audio_math]") {
    int32_t prev = -1;
    for (int v = 0; v <= 100; v += 10) {
        const int32_t f = OutputVolumeFactor(v);
        TEST_ASSERT_TRUE(f >= prev);
        prev = f;
    }
}

// ---------- ScaleTo32 ----------

TEST_CASE("Scale to 32 fits int32 for moderate samples", "[i2s_audio_math]") {
    TEST_ASSERT_EQUAL_INT32(1000 * 65536, ScaleTo32(1000, 65536));
    TEST_ASSERT_EQUAL_INT32(-1000 * 65536, ScaleTo32(-1000, 65536));
    TEST_ASSERT_EQUAL_INT32(0, ScaleTo32(0, 262144));
}

TEST_CASE("Scale to 32 clamps at int32 bounds", "[i2s_audio_math]") {
    // 32767*262144≈8.6e9 超 int32 → 钳到 INT32_MAX / INT32_MIN
    TEST_ASSERT_EQUAL_INT32(std::numeric_limits<int32_t>::max(), ScaleTo32(32767, 262144));
    TEST_ASSERT_EQUAL_INT32(std::numeric_limits<int32_t>::min(), ScaleTo32(-32768, 262144));
}

TEST_CASE("Scale to 32 mute factor yields zero", "[i2s_audio_math]") {
    TEST_ASSERT_EQUAL_INT32(0, ScaleTo32(32767, 0));
}

// ---------- DownTo16 ----------

TEST_CASE("Down to 16 arithmetic shift", "[i2s_audio_math]") {
    TEST_ASSERT_EQUAL_INT(256, DownTo16(0x00100000));  // 0x100000 >> 12
    TEST_ASSERT_EQUAL_INT(1, DownTo16(4096));          // 0x1000 >> 12
    TEST_ASSERT_EQUAL_INT(-1, DownTo16(-4096));        // 算术右移保留符号
    TEST_ASSERT_EQUAL_INT(0, DownTo16(0));
}

TEST_CASE("Down to 16 clamps at int16 bounds", "[i2s_audio_math]") {
    TEST_ASSERT_EQUAL_INT(32767, DownTo16(std::numeric_limits<int32_t>::max()));
    TEST_ASSERT_EQUAL_INT(-32768, DownTo16(std::numeric_limits<int32_t>::min()));
}

// ---------- round trip ----------

TEST_CASE("Scale then down round trips when factor is 2^12", "[i2s_audio_math]") {
    // ScaleTo32(1000,4096)=0x3E8000，右移 12 位回到 0x3E8=1000（无溢出时无损）
    TEST_ASSERT_EQUAL_INT(1000, DownTo16(ScaleTo32(1000, 4096)));
    TEST_ASSERT_EQUAL_INT(-1000, DownTo16(ScaleTo32(-1000, 4096)));
}
