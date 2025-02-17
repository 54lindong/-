package com.caseprocessor.filehandler;

import java.io.File;
import java.io.IOException;

public class FileConverter {

    // 只支持 .docx 和 .pdf 文件
    public static String convertToText(File inputFile, String fileType) throws IOException {
        if (fileType.equalsIgnoreCase("pdf")) {
            return PdfHandler.extractText(inputFile);
        } else if (fileType.equalsIgnoreCase("docx")) {
            return WordHandler.extractText(inputFile);
        } else {
            throw new IllegalArgumentException("不支持的文件类型: " + fileType);
        }
    }
}
