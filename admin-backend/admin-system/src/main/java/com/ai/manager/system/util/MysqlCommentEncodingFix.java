package com.ai.manager.system.util;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 修复 information_schema 中 COMMENT 被错误解码导致的乱码。
 * <p>
 * 仅处理「UTF-8 字节被按 ISO-8859_1 / GBK 误读」的文本；已是正常中文时原样返回，
 * 避免把正确 UTF-8 再转码成乱码。
 */
public final class MysqlCommentEncodingFix {

    private MysqlCommentEncodingFix() {}

    public static String fix(String raw) {
        if (raw == null) {
            return "";
        }
        String text = raw.trim();
        if (text.isEmpty()) {
            return text;
        }
        if (looksLikeHealthyComment(text)) {
            return text;
        }

        String best = text;
        int bestScore = score(text);

        for (Charset from : new Charset[] {StandardCharsets.ISO_8859_1, Charset.forName("GBK")}) {
            String candidate = transcode(text, from, StandardCharsets.UTF_8);
            if (!looksLikeHealthyComment(candidate)) {
                continue;
            }
            int candidateScore = score(candidate);
            if (candidateScore > bestScore) {
                best = candidate;
                bestScore = candidateScore;
            }
        }

        return best.trim();
    }

    /**
     * 已是正常中文或纯 ASCII 注释（无 Latin-1 乱码痕迹）则不再转码。
     */
    static boolean looksLikeHealthyComment(String text) {
        if (text == null || text.isEmpty()) {
            return true;
        }
        for (int offset = 0; offset < text.length(); ) {
            int codePoint = text.codePointAt(offset);
            if (codePoint == '?' || codePoint == '\uFFFD') {
                return false;
            }
            if (isSuspiciousHan(codePoint)) {
                return false;
            }
            if (Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.LATIN && codePoint > 0x7F) {
                // æ å ç 等 Latin-1 补充字符，常见于 UTF-8 被误读
                return false;
            }
            offset += Character.charCount(codePoint);
        }
        return true;
    }

    private static String transcode(String text, Charset from, Charset to) {
        try {
            return new String(text.getBytes(from), to).trim();
        } catch (Exception ex) {
            return text;
        }
    }

    private static int score(String text) {
        if (text == null || text.isEmpty()) {
            return Integer.MIN_VALUE;
        }
        int han = 0;
        int question = 0;
        int replacement = 0;
        int suspicious = 0;
        for (int offset = 0; offset < text.length(); ) {
            int codePoint = text.codePointAt(offset);
            if (Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN) {
                han++;
                if (isSuspiciousHan(codePoint)) {
                    suspicious++;
                }
            }
            if (codePoint == '?') {
                question++;
            }
            if (codePoint == '\uFFFD') {
                replacement++;
            }
            offset += Character.charCount(codePoint);
        }
        return han * 10 - question * 8 - replacement * 12 - suspicious * 6;
    }

    private static boolean isSuspiciousHan(int codePoint) {
        return codePoint == 0x9F36 || codePoint == 0x7811 || codePoint == 0x8897;
    }
}
