package com.ai.manager.system.domain.enums;

import lombok.Getter;
import org.springframework.util.StringUtils;

@Getter
public enum DocFileCategory {

    IMAGE,
    DOCUMENT,
    ARCHIVE,
    VIDEO,
    AUDIO,
    CODE,
    OTHER;

    public static DocFileCategory fromExtension(String ext) {
        if (!StringUtils.hasText(ext)) {
            return OTHER;
        }
        String lower = ext.trim().toLowerCase();
        return switch (lower) {
            case "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "ico", "tiff", "tif", "heic", "heif" -> IMAGE;
            case "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "md", "csv", "rtf", "odt", "ods", "odp" -> DOCUMENT;
            case "zip", "rar", "7z", "tar", "gz", "bz2", "xz", "zst", "tgz" -> ARCHIVE;
            case "mp4", "avi", "mkv", "mov", "wmv", "flv", "webm", "m4v", "mts" -> VIDEO;
            case "mp3", "wav", "flac", "aac", "ogg", "wma", "m4a", "opus" -> AUDIO;
            case "java", "py", "js", "ts", "html", "css", "json", "xml", "yaml", "yml", "sql", "sh", "bat",
                 "c", "cpp", "h", "hpp", "go", "rs", "kt", "scala", "php", "rb", "swift", "dart", "lua", "r",
                 "vue", "jsx", "tsx", "gradle", "toml", "ini", "cfg", "conf", "properties" -> CODE;
            default -> OTHER;
        };
    }
}
