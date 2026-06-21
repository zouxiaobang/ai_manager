package com.ai.manager.system.util;

import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class NoteContentUtils {

    private NoteContentUtils() {
    }

    public static String sha256(String content) {
        String value = content == null ? "" : content;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public static String htmlToExcerpt(String html, int maxLen) {
        if (!StringUtils.hasText(html)) {
            return "";
        }
        String text = html.replaceAll("<[^>]+>", " ")
                .replace("&nbsp;", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (!StringUtils.hasText(text)) {
            return "";
        }
        if (text.length() <= maxLen) {
            return text;
        }
        return text.substring(0, maxLen) + "…";
    }

    public static long contentSize(String content) {
        if (content == null) {
            return 0L;
        }
        return content.getBytes(StandardCharsets.UTF_8).length;
    }

    /**
     * 识别百度网盘下载接口误返回的错误 JSON，避免当作笔记正文展示。
     */
    public static boolean isBaiduApiErrorBody(String content) {
        if (!StringUtils.hasText(content)) {
            return false;
        }
        String trimmed = content.trim();
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
            return false;
        }
        if (trimmed.contains("<") || trimmed.contains(">")) {
            return false;
        }
        return trimmed.contains("\"error_code\"") || trimmed.contains("\"errno\"");
    }
}
