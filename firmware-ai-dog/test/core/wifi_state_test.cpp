// WiFi 连接状态机：连接/掉线/重试/退避决策。纯 host 测试。

#include "core/wifi_state.h"
#include "unity.h"

using namespace kyle;

TEST_CASE("StartConnect enters connecting with zero retries", "[wifi_state]") {
    WifiStateMachine m;
    m.StartConnect();
    TEST_ASSERT_EQUAL_INT(static_cast<int>(WifiLinkState::kConnecting),
                          static_cast<int>(m.state()));
    TEST_ASSERT_EQUAL_INT(0, m.retry_count());
    TEST_ASSERT_TRUE(m.should_retry());
}

TEST_CASE("GotIp moves to connected and resets retries", "[wifi_state]") {
    WifiStateMachine m;
    m.StartConnect();
    m.OnDisconnected();  // 首次失败
    TEST_ASSERT_EQUAL_INT(1, m.retry_count());
    m.OnGotIp();
    TEST_ASSERT_EQUAL_INT(static_cast<int>(WifiLinkState::kConnected),
                          static_cast<int>(m.state()));
    TEST_ASSERT_EQUAL_INT(0, m.retry_count());
}

TEST_CASE("Never-connected failures stop after max_retries", "[wifi_state]") {
    WifiPolicy policy;
    policy.max_retries = 3;
    WifiStateMachine m(policy);
    m.StartConnect();
    for (int i = 0; i < 3; ++i) {
        m.OnDisconnected();
    }
    // 3 次后应仍 connecting（retries=3，should_retry 为 false），下一次回未连接
    TEST_ASSERT_EQUAL_INT(3, m.retry_count());
    TEST_ASSERT_FALSE(m.should_retry());
    m.OnDisconnected();
    TEST_ASSERT_EQUAL_INT(static_cast<int>(WifiLinkState::kDisconnected),
                          static_cast<int>(m.state()));
}

TEST_CASE("Drop after connected auto-reconnects without limit", "[wifi_state]") {
    WifiPolicy policy;
    policy.max_retries = 2;  // 曾连上后掉线不设上限
    WifiStateMachine m(policy);
    m.StartConnect();
    m.OnGotIp();
    // 掉线多次：每次都应回 connecting 继续重连，即使超过 max_retries
    for (int i = 0; i < 10; ++i) {
        m.OnDisconnected();
        TEST_ASSERT_EQUAL_INT(static_cast<int>(WifiLinkState::kConnecting),
                              static_cast<int>(m.state()));
    }
}

TEST_CASE("Associated keeps connecting until IP", "[wifi_state]") {
    WifiStateMachine m;
    m.StartConnect();
    m.OnAssociated();
    TEST_ASSERT_EQUAL_INT(static_cast<int>(WifiLinkState::kConnecting),
                          static_cast<int>(m.state()));
}

TEST_CASE("Retry delay backoffs exponentially up to 30s", "[wifi_state]") {
    WifiPolicy policy;
    policy.retry_delay_ms = 1000;
    WifiStateMachine m(policy);
    m.StartConnect();
    m.OnGotIp();  // 先连上，此后掉线走无限重连路径（退避只受封顶约束）
    m.OnDisconnected();  // 第 1 次掉线
    TEST_ASSERT_EQUAL_INT(2000, m.retry_delay_ms());
    m.OnDisconnected();  // 第 2 次
    TEST_ASSERT_EQUAL_INT(4000, m.retry_delay_ms());
    m.OnDisconnected();  // 第 3 次
    TEST_ASSERT_EQUAL_INT(8000, m.retry_delay_ms());
    // 长时间掉线退避封顶 30s
    for (int i = 0; i < 10; ++i) {
        m.OnDisconnected();
    }
    TEST_ASSERT_EQUAL_INT(30000, m.retry_delay_ms());
    TEST_ASSERT_EQUAL_INT(static_cast<int>(WifiLinkState::kConnecting),
                          static_cast<int>(m.state()));
}

TEST_CASE("Reset returns to disconnected and clears retry budget", "[wifi_state]") {
    WifiStateMachine m;
    m.StartConnect();
    m.OnGotIp();
    m.Reset();
    TEST_ASSERT_EQUAL_INT(static_cast<int>(WifiLinkState::kDisconnected),
                          static_cast<int>(m.state()));
    TEST_ASSERT_EQUAL_INT(0, m.retry_count());
    // 重试预算重置回满：should_retry 为 true（0 < max_retries）
    TEST_ASSERT_TRUE(m.should_retry());
}
