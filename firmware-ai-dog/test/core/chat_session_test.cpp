#include <cstdio>

#include "core/chat_session.h"
#include "unity.h"

using namespace kyle;

TEST_CASE("happy path idle->connecting->listening->speaking->idle", "[chat]") {
    ChatSession s;
    TEST_ASSERT_EQUAL(kIdle, s.state());
    TEST_ASSERT_TRUE(s.HandleEvent(kStart));
    TEST_ASSERT_EQUAL(kConnecting, s.state());
    TEST_ASSERT_TRUE(s.HandleEvent(kChannelOpened));
    TEST_ASSERT_EQUAL(kListening, s.state());
    TEST_ASSERT_TRUE(s.HandleEvent(kSpeakingStarted));
    TEST_ASSERT_EQUAL(kSpeaking, s.state());
    TEST_ASSERT_TRUE(s.HandleEvent(kSpeakingStopped));  // TTS 结束自动续听
    TEST_ASSERT_EQUAL(kListening, s.state());
    TEST_ASSERT_TRUE(s.HandleEvent(kStopListening));
    TEST_ASSERT_EQUAL(kIdle, s.state());
}

TEST_CASE("illegal transition from idle is rejected", "[chat]") {
    ChatSession s;
    TEST_ASSERT_FALSE(s.HandleEvent(kSpeakingStarted));
    TEST_ASSERT_EQUAL(kIdle, s.state());
    TEST_ASSERT_FALSE(s.HandleEvent(kSpeakingStopped));
    TEST_ASSERT_FALSE(s.HandleEvent(kStopListening));
    TEST_ASSERT_EQUAL(kIdle, s.state());
}

TEST_CASE("listening->start is rejected", "[chat]") {
    ChatSession s;
    s.OnEvent(kStart);
    s.OnEvent(kChannelOpened);
    TEST_ASSERT_EQUAL(kListening, s.state());
    TEST_ASSERT_FALSE(s.HandleEvent(kStart));
    TEST_ASSERT_EQUAL(kListening, s.state());
}

TEST_CASE("wake word barge-in aborts speaking with correct reason", "[chat]") {
    ChatSession s;
    s.OnEvent(kStart);
    s.OnEvent(kChannelOpened);
    s.OnEvent(kSpeakingStarted);
    TEST_ASSERT_EQUAL(kSpeaking, s.state());
    TEST_ASSERT_TRUE(s.HandleEvent(kWakeWordDetected));
    TEST_ASSERT_EQUAL(kListening, s.state());
    TEST_ASSERT_EQUAL(kAbortWakeWordDetected, s.last_abort_reason());
}

TEST_CASE("error during connecting resets to idle", "[chat]") {
    ChatSession s;
    s.OnEvent(kStart);
    TEST_ASSERT_EQUAL(kConnecting, s.state());
    TEST_ASSERT_TRUE(s.HandleEvent(kError));
    TEST_ASSERT_EQUAL(kIdle, s.state());
    TEST_ASSERT_TRUE(s.IsActive() == false);
}

TEST_CASE("state transition matrix matches spec", "[chat]") {
    const SessionState kStates[] = {kIdle, kConnecting, kListening, kSpeaking};
    const SessionEvent kEvents[] = {
        kStart, kChannelOpened, kStopListening, kSpeakingStarted,
        kSpeakingStopped, kWakeWordDetected, kError, kDisconnected,
    };
    // 期望矩阵 [state][event]
    const bool kExpected[4][8] = {
        /* idle       */ {true,  false, false, false, false, true,  false, false},
        /* connecting */ {false, true,  true,  false, false, false, true,  true},
        /* listening  */ {false, false, true,  true,  false, false, true,  true},
        /* speaking   */ {false, false, true,  false, true,  true,  true,  true},
    };
    for (int i = 0; i < 4; ++i) {
        for (int j = 0; j < 8; ++j) {
            bool actual = ChatSession::IsValidTransition(kStates[i], kEvents[j]);
            char buf[128];
            std::snprintf(buf, sizeof(buf), "matrix mismatch at state=%d event=%d (expect %d, got %d)",
                          i, j, kExpected[i][j] ? 1 : 0, actual ? 1 : 0);
            TEST_ASSERT_TRUE_MESSAGE(kExpected[i][j] == actual, buf);
        }
    }
}
