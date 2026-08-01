package com.ai.manager.system.service.support.rag;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 文档解析器实现
 *
 * <p>支持 PDF / TXT / Markdown / HTML / DOCX 格式解析。</p>
 */
@Slf4j
@Component
public class DocumentParserImpl implements DocumentParser {

    @Override
    public String parse(InputStream inputStream, String fileType) {
        return switch (fileType.toLowerCase()) {
            case "pdf" -> parsePdf(inputStream);
            case "txt", "md" -> parseText(inputStream);
            case "html", "htm" -> parseHtml(inputStream);
            case "docx" -> parseDocx(inputStream);
            default -> throw new IllegalArgumentException("不支持的文件类型: " + fileType);
        };
    }

    /**
     * 解析 PDF 文件
     */
    private String parsePdf(InputStream inputStream) {
        try {
            // PDFBox 3.x Loader.loadPDF 不支持 InputStream，需先读 byte[]
            byte[] bytes = inputStream.readAllBytes();
            try (PDDocument document = Loader.loadPDF(bytes)) {
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setSortByPosition(true);
                return stripper.getText(document);
            }
        } catch (IOException e) {
            log.error("PDF 解析失败", e);
            throw new RuntimeException("PDF 解析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析纯文本文件（TXT / Markdown）
     */
    private String parseText(InputStream inputStream) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        } catch (IOException e) {
            log.error("文本解析失败", e);
            throw new RuntimeException("文本解析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析 HTML 文件（提取纯文本）
     */
    private String parseHtml(InputStream inputStream) {
        try {
            org.jsoup.nodes.Document doc = Jsoup.parse(inputStream, "UTF-8", "");
            // 移除 script 和 style 标签
            doc.select("script, style, nav, footer, header").remove();
            // 获取纯文本，保留段落结构
            return doc.body().text();
        } catch (IOException e) {
            log.error("HTML 解析失败", e);
            throw new RuntimeException("HTML 解析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析 DOCX 文件
     */
    private String parseDocx(InputStream inputStream) {
        try (XWPFDocument document = new XWPFDocument(inputStream);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        } catch (IOException e) {
            log.error("DOCX 解析失败", e);
            throw new RuntimeException("DOCX 解析失败: " + e.getMessage(), e);
        }
    }
}
