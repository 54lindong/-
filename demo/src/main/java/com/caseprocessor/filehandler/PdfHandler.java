package com.caseprocessor.filehandler;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class PdfHandler {
    
    public static String extractTextFromPDF(File file) throws IOException {
        try (PDDocument document = PDDocument.load(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            // 设置文本提取顺序（从左到右，从上到下）
            stripper.setSortByPosition(true);
            return stripper.getText(document);
        }
    }
    
    public static void createPDF(String content, File outputFile) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            
            // 加载中文字体
            InputStream fontStream = PdfHandler.class.getResourceAsStream("/fonts/SimSun.ttf");
            PDType0Font font = PDType0Font.load(document, fontStream);
            
            try (PDPageContentStream contentStream = 
                    new PDPageContentStream(document, page)) {
                
                contentStream.beginText();
                contentStream.setFont(font, 12);
                contentStream.newLineAtOffset(50, 700);
                
                // 处理换行
                String[] lines = content.split("\n");
                for (String line : lines) {
                    contentStream.showText(line);
                    contentStream.newLineAtOffset(0, -15);
                }
                
                contentStream.endText();
            }
            
            document.save(outputFile);
        }
    }
    
    public static boolean isPDF(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".pdf");
    }
    
    public static PDDocument createEmptyPDF() {
        PDDocument document = new PDDocument();
        document.addPage(new PDPage());
        return document;
    }
    
    public static void appendText(PDDocument document, String text) throws IOException {
        PDPage page = document.getPage(document.getNumberOfPages() - 1);
        
        try (PDPageContentStream contentStream = 
                new PDPageContentStream(document, page, 
                    PDPageContentStream.AppendMode.APPEND, true, true)) {
            
            contentStream.beginText();
            // 使用默认字体，实际使用时需要处理中文字体
            contentStream.setFont(PDType0Font.load(document, 
                PdfHandler.class.getResourceAsStream("/fonts/SimSun.ttf")), 12);
            contentStream.newLineAtOffset(50, 700);
            contentStream.showText(text);
            contentStream.endText();
        }
    }
}