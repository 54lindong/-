package com.caseprocessor.filehandler;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;

public class PdfHandler {

    // 提取 .pdf 文件中的文本
    public static String extractText(File pdfFile) throws IOException {
        if (!pdfFile.getName().endsWith(".pdf")) {
            throw new IllegalArgumentException("仅支持 .pdf 文件");
        }

        PDDocument document = PDDocument.load(pdfFile);
        PDFTextStripper stripper = new PDFTextStripper();
        String text = stripper.getText(document);
        document.close();
        return text;
    }
}
