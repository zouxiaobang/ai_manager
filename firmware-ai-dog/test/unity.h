#ifndef UNITY_H
#define UNITY_H

// 极简 Unity 兼容测试框架（自包含，零外部依赖）。
// C 侧（unity.c）提供注册/运行器/失败原语；C++ 侧提供 TEST_CASE 宏与断言宏。

#ifdef __cplusplus
extern "C" {
#endif

typedef void (*UnityTestFunction)(void);

int  UnityRegisterTest(const char* name, const char* file, UnityTestFunction fn);
void UnityRunAll(void);
void UnityFail(const char* msg, const char* file, int line);

#ifdef __cplusplus
}  // extern "C"

#include <cstdint>
#include <cstdio>
#include <cstring>
#include <sstream>
#include <string>

// ---- 测试注册：静态初始化器把 TEST_CASE 自动注册进运行器 ----
struct UnityRegistrar {
    UnityRegistrar(const char* name, const char* file, UnityTestFunction fn) {
        UnityRegisterTest(name, file, fn);
    }
};

#define UNITY_JOIN_IMPL(a, b) a##b
#define UNITY_JOIN(a, b) UNITY_JOIN_IMPL(a, b)
#define UNITY_UNIQUE(prefix) UNITY_JOIN(prefix, __LINE__)

// name 是字符串字面量（可含空格/中文），函数/注册器符号用行号生成，避免 ## 粘贴字符串字面量。
#define TEST_CASE(name, attr)                                                     \
    static void UNITY_UNIQUE(UnityTestBody_)(void);                               \
    static UnityRegistrar UNITY_UNIQUE(UnityRegistrar_)(name, __FILE__,           \
                                                        &UNITY_UNIQUE(UnityTestBody_)); \
    static void UNITY_UNIQUE(UnityTestBody_)(void)

namespace unity_detail {

template <typename A, typename B>
inline void AssertEqual(const char* aexpr, const char* bexpr, const A& a, const B& b,
                        const char* file, int line) {
    if (!(a == b)) {
        std::ostringstream os;
        os << "expected [" << aexpr << "] == [" << bexpr << "], got " << a << " vs " << b;
        UnityFail(os.str().c_str(), file, line);
    }
}

inline void AssertStringEqual(const char* expected, const char* actual, const char* file,
                              int line) {
    if (expected == nullptr || actual == nullptr || std::strcmp(expected, actual) != 0) {
        char buf[512];
        std::snprintf(buf, sizeof(buf), "expected string \"%s\" but got \"%s\"",
                      expected ? expected : "(null)", actual ? actual : "(null)");
        UnityFail(buf, file, line);
    }
}

inline void AssertBytesEqual(const uint8_t* expected, const uint8_t* actual, size_t len,
                             const char* file, int line) {
    if (expected == nullptr || actual == nullptr || std::memcmp(expected, actual, len) != 0) {
        char buf[256];
        if (expected != nullptr && actual != nullptr) {
            size_t i = 0;
            while (i < len && expected[i] == actual[i]) ++i;
            std::snprintf(buf, sizeof(buf), "byte arrays differ at index %zu: 0x%02x vs 0x%02x",
                          i, i < len ? expected[i] : 0, i < len ? actual[i] : 0);
        } else {
            std::snprintf(buf, sizeof(buf), "byte arrays: one of them is null");
        }
        UnityFail(buf, file, line);
    }
}

}  // namespace unity_detail

// ---- 断言宏 ----
#define TEST_ASSERT_TRUE(cond) \
    do { if (!(cond)) UnityFail(#cond " expected TRUE", __FILE__, __LINE__); } while (0)
#define TEST_ASSERT_FALSE(cond) \
    do { if (cond) UnityFail(#cond " expected FALSE", __FILE__, __LINE__); } while (0)
#define TEST_ASSERT_NULL(v) \
    do { if ((v) != nullptr) UnityFail(#v " expected NULL", __FILE__, __LINE__); } while (0)
#define TEST_ASSERT_NOT_NULL(v) \
    do { if ((v) == nullptr) UnityFail(#v " expected NOT NULL", __FILE__, __LINE__); } while (0)
#define TEST_ASSERT_TRUE_MESSAGE(cond, msg) \
    do { if (!(cond)) UnityFail((msg), __FILE__, __LINE__); } while (0)

#define TEST_ASSERT_EQUAL(a, b) \
    do { ::unity_detail::AssertEqual(#a, #b, (a), (b), __FILE__, __LINE__); } while (0)

// Unity 风格别名：整型/十六进制/无符号
#define TEST_ASSERT_EQUAL_INT(expected, actual)  TEST_ASSERT_EQUAL(expected, actual)
#define TEST_ASSERT_EQUAL_INT32(expected, actual) TEST_ASSERT_EQUAL(expected, actual)
#define TEST_ASSERT_EQUAL_UINT32(expected, actual) TEST_ASSERT_EQUAL(expected, actual)
#define TEST_ASSERT_EQUAL_SIZE_T(expected, actual) TEST_ASSERT_EQUAL(expected, actual)
#define TEST_ASSERT_EQUAL_UINT8(expected, actual) TEST_ASSERT_EQUAL(expected, actual)
#define TEST_ASSERT_EQUAL_UINT16(expected, actual) TEST_ASSERT_EQUAL(expected, actual)
#define TEST_ASSERT_EQUAL_HEX8(expected, actual) TEST_ASSERT_EQUAL(expected, actual)
#define TEST_ASSERT_EQUAL_HEX16(expected, actual) TEST_ASSERT_EQUAL(expected, actual)
#define TEST_ASSERT_EQUAL_HEX32(expected, actual) TEST_ASSERT_EQUAL(expected, actual)

#define TEST_ASSERT_EQUAL_STRING(expected, actual) \
    do { ::unity_detail::AssertStringEqual((expected), (actual), __FILE__, __LINE__); } while (0)

#define TEST_ASSERT_BYTES_EQUAL(expected, actual, len) \
    do { ::unity_detail::AssertBytesEqual((expected), (actual), (len), __FILE__, __LINE__); } while (0)

#endif  // __cplusplus
#endif  // UNITY_H
