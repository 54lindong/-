package com.caseprocessor.filehandler;

import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class WordHandler {
    public static String extractText(File wordFile) throws IOException {
        FileInputStream fis = new FileInputStream(wordFile);
        XWPFDocument document = new XWPFDocument(fis);
        StringBuilder text = new StringBuilder();

        document.getParagraphs().forEach(paragraph -> text.append(paragraph.getText()).append("\n"));
        return text.toString();
    }
}
