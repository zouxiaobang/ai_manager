/* 极简 Unity 兼容运行器（C）。注册表 + setjmp/longjmp 失败中断。 */
#include "unity.h"

#include <setjmp.h>
#include <stdio.h>
#include <string.h>

#define UNITY_MAX_TESTS 256

typedef struct {
    const char* name;
    const char* file;
    UnityTestFunction fn;
} UnityTestRecord;

static UnityTestRecord g_tests[UNITY_MAX_TESTS];
static int g_test_count = 0;
static jmp_buf g_jump;
static int g_failure = 0;
static int g_pass_count = 0;
static int g_fail_count = 0;
static char g_fail_msg[1024];

int UnityRegisterTest(const char* name, const char* file, UnityTestFunction fn) {
    if (g_test_count < UNITY_MAX_TESTS && name != NULL && fn != NULL) {
        g_tests[g_test_count].name = name;
        g_tests[g_test_count].file = file ? file : "";
        g_tests[g_test_count].fn = fn;
        ++g_test_count;
        return 1;
    }
    return 0;
}

void UnityFail(const char* msg, const char* file, int line) {
    if (msg == NULL) msg = "assertion failed";
    snprintf(g_fail_msg, sizeof(g_fail_msg), "%s:%d: %s", file ? file : "?", line, msg);
    g_failure = 1;
    longjmp(g_jump, 1);
}

void UnityRunAll(void) {
    printf("\n=== xiaozhi host unit tests ===\n");
    printf("registered %d test(s)\n", g_test_count);
    for (int i = 0; i < g_test_count; ++i) {
        g_failure = 0;
        g_fail_msg[0] = '\0';
        if (setjmp(g_jump) == 0) {
            g_tests[i].fn();
        }
        if (g_failure) {
            ++g_fail_count;
            printf("[FAIL] %s\n    %s\n", g_tests[i].name, g_fail_msg);
        } else {
            ++g_pass_count;
            printf("[PASS] %s\n", g_tests[i].name);
        }
    }
    printf("\n%d passed, %d failed, %d total\n", g_pass_count, g_fail_count,
           g_pass_count + g_fail_count);
}

int main(void) {
    UnityRunAll();
    return g_fail_count == 0 ? 0 : 1;
}
