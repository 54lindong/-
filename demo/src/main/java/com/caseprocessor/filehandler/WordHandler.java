package com.caseprocessor.filehandler;

import org.apache.poi.xwpf.usermodel.*;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class WordHandler {
    
    public static String extractTextFromDOCX(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             XWPFDocument document = new XWPFDocument(fis)) {
            
            StringBuilder text = new StringBuilder();
            
            // 处理段落
            document.getParagraphs().forEach(paragraph -> {
                text.append(paragraph.getText()).append("\n");
            });
            
            // 处理表格
            document.getTables().forEach(table -> {
                table.getRows().forEach(row -> {
                    row.getTableCells().forEach(cell -> {
                        text.append(cell.getText()).append(" ");
                    });
                    text.append("\n");
                });
                text.append("\n");
            });
            
            return text.toString();
        }
    }
    
    public static String extractTextFromDOC(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             HWPFDocument document = new HWPFDocument(fis)) {
            
            WordExtractor extractor = new WordExtractor(document);
            String[] paragraphs = extractor.getParagraphText();
            
            StringBuilder text = new StringBuilder();
            for (String paragraph : paragraphs) {
                text.append(paragraph.trim()).append("\n");
            }
            
            return text.toString();
        }
    }
    
    public static void createDOCX(String content, File outputFile) throws IOException {
        try (XWPFDocument document = new XWPFDocument()) {
            // 创建段落
            XWPFParagraph paragraph = document.createParagraph();
            XWPFRun run = paragraph.createRun();
            
            // 设置字体和大小
            run.setFontFamily("宋体");
            run.setFontSize(12);
            
            // 添加内容
            String[] lines = content.split("\n");
            for (int i = 0; i < lines.length; i++) {
                run.setText(lines[i]);
                if (i < lines.length - 1) {
                    run.addBreak();
                }
            }
            
            // 保存文件
            try (java.io.FileOutputStream out = new java.io.FileOutputStream(outputFile)) {
                document.write(out);
            }
        }
    }
    
    public static boolean isWordDocument(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".doc") || name.endsWith(".docx");
    }
    
    public static boolean isDOCX(File file) {
        return file.getName().toLowerCase().endsWith(".docx");
    }
    
    public static boolean isDOC(File file) {
        return file.getName().toLowerCase().endsWith(".doc");
    }
    
    public static XWPFDocument createEmptyDOCX() {
        return new XWPFDocument();
    }
    
    public static void appendText(XWPFDocument document, String text) {
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setFontFamily("宋体");
        run.setFontSize(12);
        run.setText(text);
    }
    
    public static void appendTable(XWPFDocument document, String[][] data) {
        if (data == null || data.length == 0) return;
        
        XWPFTable table = document.createTable(data.length, data[0].length);
        
        // 填充数据
        for (int i = 0; i < data.length; i++) {
            XWPFTableRow row = table.getRow(i);
            for (int j = 0; j < data[i].length; j++) {
                XWPFTableCell cell = row.getCell(j);
                cell.setText(data[i][j]);
            }
        }
        
        // 添加一个空段落作为间隔
        document.createParagraph();
    }
}