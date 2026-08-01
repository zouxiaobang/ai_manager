-- ============================================================
-- v2: 将 ai_chat_conversation.messages (JSON) 迁移到
--      ai_chat_message 分表，并删除旧列
-- ============================================================
USE ai_manager_admin;

-- Step 1: 将现有 JSON 消息解析并插入到 ai_chat_message 表
-- 使用 JSON_TABLE 将 JSON 数组展开为多行
INSERT INTO ai_chat_message (conversation_id, msg_id, role, content, sort_order, create_time)
SELECT
    c.id                   AS conversation_id,
    JSON_UNQUOTE(
        JSON_EXTRACT(msg.value, '$.id')
    )                      AS msg_id,
    JSON_UNQUOTE(
        JSON_EXTRACT(msg.value, '$.role')
    )                      AS role,
    JSON_UNQUOTE(
        JSON_EXTRACT(msg.value, '$.content')
    )                      AS content,
    msg.ordinality - 1     AS sort_order,
    FROM_UNIXTIME(
        JSON_EXTRACT(msg.value, '$.timestamp') / 1000
    )                      AS create_time
FROM ai_chat_conversation c,
JSON_TABLE(
    c.messages,
    '$[*]' COLUMNS (
        value       JSON PATH '$',
        ordinality  FOR ORDINALITY
    )
) msg
WHERE c.messages IS NOT NULL
  AND c.messages != '[]'
  AND JSON_VALID(c.messages)
  AND JSON_LENGTH(c.messages) > 0;

-- Step 2: 删除旧的 messages 列
ALTER TABLE ai_chat_conversation DROP COLUMN messages;

-- ============================================================
-- 验证迁移结果
-- ============================================================
SELECT 'ai_chat_message count' AS info, COUNT(*) AS total FROM ai_chat_message
UNION ALL
SELECT 'conversations total', COUNT(*) FROM ai_chat_conversation;
