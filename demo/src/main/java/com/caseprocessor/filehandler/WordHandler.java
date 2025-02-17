package com.caseprocessor.filehandler;

import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class WordHandler {

    // 提取 .docx 文件中的文本
    public static String extractText(File wordFile) throws IOException {
        if (!wordFile.getName().endsWith(".docx")) {
            throw new IllegalArgumentException("仅支持 .docx 文件");
        }

        FileInputStream fis = new FileInputStream(wordFile);
        XWPFDocument document = new XWPFDocument(fis);
        StringBuilder text = new StringBuilder();

        // 获取文档中的所有段落并提取
        document.getParagraphs().forEach(paragraph -> text.append(paragraph.getText()).append("\n"));
        return text.toString();  // 返回提取的文本
    }
}

