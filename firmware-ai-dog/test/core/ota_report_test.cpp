// OTA 升级状态上报协议：请求体黄金向量 + URL 拼接 + 进度计算 + 上报节流。
// 契约对齐后端 POST /api/iot/ota/status（OtaStatusRequest，snake_case 字段）。

#include <cstring>

#include "app/application.h"
#include "core/ota_report.h"
#include "mocks/mock_board.h"
#include "mocks/mock_storage.h"
#include "unity.h"

using namespace kyle;

TEST_CASE("Status body matches backend OtaStatusRequest golden vector", "[ota_report]") {
    TEST_ASSERT_EQUAL_STRING(
        "{\"mac\":\"f8ce215b3e3b\",\"state\":\"DOWNLOADING\",\"progress\":42}",
        BuildOtaStatusBody("f8ce215b3e3b", OtaReportState::kDownloading, 42).c_str());
    TEST_ASSERT_EQUAL_STRING(
        "{\"mac\":\"f8ce215b3e3b\",\"state\":\"SUCCESS\",\"progress\":100}",
        BuildOtaStatusBody("f8ce215b3e3b", OtaReportState::kSuccess, 100).c_str());
    TEST_ASSERT_EQUAL_STRING(
        "{\"mac\":\"f8ce215b3e3b\",\"state\":\"FAILED\",\"progress\":35}",
        BuildOtaStatusBody("f8ce215b3e3b", OtaReportState::kFailed, 35).c_str());
}

TEST_CASE("Status body escapes quotes and clamps progress", "[ota_report]") {
    // MAC 含引号会被 JSON 转义；progress 越界钳位到 0-100
    TEST_ASSERT_EQUAL_STRING(
        "{\"mac\":\"aa\\\"bb\",\"state\":\"SUCCESS\",\"progress\":100}",
        BuildOtaStatusBody("aa\"bb", OtaReportState::kSuccess, 999).c_str());
    TEST_ASSERT_EQUAL_STRING(
        "{\"mac\":\"aa\",\"state\":\"FAILED\",\"progress\":0}",
        BuildOtaStatusBody("aa", OtaReportState::kFailed, -5).c_str());
}

TEST_CASE("Status url strips trailing slash like check url", "[ota_report]") {
    TEST_ASSERT_EQUAL_STRING("http://192.168.0.114:8080/api/iot/ota/status",
                             BuildOtaStatusUrl("http://192.168.0.114:8080/api/iot/ota/").c_str());
    TEST_ASSERT_EQUAL_STRING("http://192.168.0.114:8080/api/iot/ota/status",
                             BuildOtaStatusUrl("http://192.168.0.114:8080/api/iot/ota").c_str());
    TEST_ASSERT_EQUAL_STRING("https://api.tenclass.net/kyle/ota/status",
                             BuildOtaStatusUrl("https://api.tenclass.net/kyle/ota/").c_str());
}

TEST_CASE("Percent computation handles unknown and complete sizes", "[ota_report]") {
    TEST_ASSERT_EQUAL_INT(0, ComputeOtaPercent(100, 0));        // Content-Length 未知
    TEST_ASSERT_EQUAL_INT(50, ComputeOtaPercent(500, 1000));    // 半数
    TEST_ASSERT_EQUAL_INT(100, ComputeOtaPercent(1000, 1000));  // 完成
    TEST_ASSERT_EQUAL_INT(100, ComputeOtaPercent(1200, 1000));  // 超出（服务端多写）钳位
    TEST_ASSERT_EQUAL_INT(0, ComputeOtaPercent(1, 1000));       // 小进度向下取整（0.1% → 0）
}

TEST_CASE("Throttle reports on step boundaries only", "[ota_report]") {
    OtaProgressThrottle t(10);
    TEST_ASSERT_FALSE(t.ShouldReport(0, false));   // 首个里程碑前不报
    TEST_ASSERT_FALSE(t.ShouldReport(5, false));
    TEST_ASSERT_TRUE(t.ShouldReport(10, false));   // 跨过 10%
    TEST_ASSERT_FALSE(t.ShouldReport(15, false));  // 距上次不足 10%
    TEST_ASSERT_FALSE(t.ShouldReport(19, false));
    TEST_ASSERT_TRUE(t.ShouldReport(20, false));
    TEST_ASSERT_TRUE(t.ShouldReport(30, false));
}

TEST_CASE("Throttle always reports terminal state", "[ota_report]") {
    OtaProgressThrottle t(10);
    t.ShouldReport(10, false);
    t.ShouldReport(20, false);
    TEST_ASSERT_TRUE(t.ShouldReport(37, true));  // 非里程碑也报（终态）
}

TEST_CASE("Throttle reports immediately when progress jumps over first step", "[ota_report]") {
    OtaProgressThrottle t(10);
    TEST_ASSERT_FALSE(t.ShouldReport(0, false));
    TEST_ASSERT_TRUE(t.ShouldReport(55, false));   // 直接跳过多个里程碑
    TEST_ASSERT_FALSE(t.ShouldReport(60, false));  // 距 55 未满 10%
    TEST_ASSERT_TRUE(t.ShouldReport(65, false));
}

TEST_CASE("Throttle clamps invalid step to 10", "[ota_report]") {
    OtaProgressThrottle t(0);   // 非法阈值兜底
    TEST_ASSERT_FALSE(t.ShouldReport(5, false));
    TEST_ASSERT_TRUE(t.ShouldReport(10, false));
}

TEST_CASE("Application reports OTA status to backend status url", "[application][ota_report]") {
    MockBoard board;
    MockStorage storage;
    Application app(board, storage);
    app.Initialize();

    // ReportOtaStatus 走 CreateHttp 共享日志：断言方法/URL/body 契约
    app.ReportOtaStatus(OtaReportState::kDownloading, 42);
    TEST_ASSERT_NOT_NULL(board.network_mock.http_log.get());
    TEST_ASSERT_EQUAL_STRING("POST", board.network_mock.http_log->method.c_str());
    TEST_ASSERT_EQUAL_STRING("https://api.tenclass.net/kyle/ota/status",
                             board.network_mock.http_log->url.c_str());
    // MAC 用归一化（小写去冒号），与 OTA check/WS 握手同一来源
    TEST_ASSERT_NOT_NULL(strstr(board.network_mock.http_log->body.c_str(),
                                "\"mac\":\"aabbccddeeff\""));
    TEST_ASSERT_NOT_NULL(strstr(board.network_mock.http_log->body.c_str(),
                                "\"state\":\"DOWNLOADING\""));
    TEST_ASSERT_NOT_NULL(strstr(board.network_mock.http_log->body.c_str(),
                                "\"progress\":42"));
}
