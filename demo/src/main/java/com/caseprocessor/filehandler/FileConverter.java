
package com.caseprocessor.filehandler;

import java.util.concurrent.CompletionException;

import org.apache.poi.xwpf.usermodel.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;


public class FileConverter {
    
    // 文件转换的回调接口
    public interface ConversionProgressCallback {
        void onProgress(int percentage, String message);
        void onComplete(File outputFile);
        void onError(Exception e);
    }
    
    // 支持的文件类型枚举
    public enum FileType {
        PDF(".pdf"),
        DOCX(".docx"),
        DOC(".doc"),
        TXT(".txt"),
        XML(".xml"),
        HTML(".html"),
        JSON(".json");
        
        private final String extension;
        
        FileType(String extension) {
            this.extension = extension;
        }
        
        public String getExtension() {
            return extension;
        }
        
        public static FileType fromFile(File file) {
            String name = file.getName().toLowerCase();
            for (FileType type : values()) {
                if (name.endsWith(type.extension)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("不支持的文件类型: " + file.getName());
        }
    }
    
    // 异步转换文件
    public CompletableFuture<File> convertAsync(File inputFile, FileType targetType, 
            ConversionProgressCallback callback) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                callback.onProgress(0, "开始转换文件...");
                File outputFile = convert(inputFile, targetType, percentage -> 
                    callback.onProgress(percentage, "正在转换..."));
                callback.onProgress(100, "转换完成");
                callback.onComplete(outputFile);
                return outputFile;
            } catch (Exception e) {
                callback.onError(e);
                throw new CompletionException(e);
            }
        });
    }
    
    // 同步转换文件
    public File convert(File inputFile, FileType targetType, Consumer<Integer> progressCallback) 
            throws IOException {
        FileType sourceType = FileType.fromFile(inputFile);
        
        // 创建输出文件
        String outputPath = createOutputPath(inputFile, targetType);
        File outputFile = new File(outputPath);
        
        // 根据源文件类型和目标类型选择转换方法
        String content = extractContent(inputFile, sourceType, progressCallback);
        createTargetFile(content, outputFile, targetType, progressCallback);
        
        return outputFile;
    }
    
    // 提取文件内容
    private String extractContent(File file, FileType type, Consumer<Integer> progressCallback) 
            throws IOException {
        progressCallback.accept(20);
        
        switch (type) {
            case PDF:
                return PdfHandler.extractTextFromPDF(file);
            case DOCX:
                return WordHandler.extractTextFromDOCX(file);
            case DOC:
                return WordHandler.extractTextFromDOC(file);
            case TXT:
                return Files.readString(file.toPath());
            case XML:
                return extractFromXML(file);
            case HTML:
                return extractFromHTML(file);
            case JSON:
                return Files.readString(file.toPath());
            default:
                throw new IllegalArgumentException("不支持的源文件类型：" + type);
        }
    }
    
    // 创建目标文件
    private void createTargetFile(String content, File outputFile, FileType targetType,
            Consumer<Integer> progressCallback) throws IOException {
        progressCallback.accept(60);
        
        switch (targetType) {
            case PDF:
                PdfHandler.createPDF(content, outputFile);
                break;
            case DOCX:
                WordHandler.createDOCX(content, outputFile);
                break;
            case TXT:
                Files.writeString(outputFile.toPath(), content);
                break;
            case XML:
                createXML(content, outputFile);
                break;
            case HTML:
                createHTML(content, outputFile);
                break;
            case JSON:
                createJSON(content, outputFile);
                break;
            default:
                throw new IllegalArgumentException("不支持的目标文件类型：" + targetType);
        }
        
        progressCallback.accept(90);
    }
    
    // 从XML提取文本
    private String extractFromXML(File file) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(file);
            return doc.getDocumentElement().getTextContent();
        } catch (Exception e) {
            throw new IOException("XML文件解析失败", e);
        }
    }
    
    // 从HTML提取文本
    private String extractFromHTML(File file) throws IOException {
        return Files.readString(file.toPath()).replaceAll("<[^>]*>", "");
    }
    
    // 创建XML文件
    private void createXML(String content, File outputFile) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();
            
            Element rootElement = doc.createElement("document");
            doc.appendChild(rootElement);
            
            Element contentElement = doc.createElement("content");
            contentElement.setTextContent(content);
            rootElement.appendChild(contentElement);
            
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(outputFile);
            transformer.transform(source, result);
            
        } catch (Exception e) {
            throw new IOException("创建XML文件失败", e);
        }
    }
    
    // 创建HTML文件
    private void createHTML(String content, File outputFile) throws IOException {
        String html = String.format(
            "<!DOCTYPE html>\n" +
            "<html>\n" +
            "<head>\n" +
            "    <meta charset=\"UTF-8\">\n" +
            "    <title>转换文档</title>\n" +
            "    <style>\n" +
            "        body { font-family: Arial, sans-serif; line-height: 1.6; padding: 20px; }\n" +
            "        pre { white-space: pre-wrap; }\n" +
            "    </style>\n" +
            "</head>\n" +
            "<body>\n" +
            "<pre>%s</pre>\n" +
            "</body>\n" +
            "</html>",
            escapeHtml(content)
        );
        
        Files.writeString(outputFile.toPath(), html);
    }
    
    // 创建JSON文件
    private void createJSON(String content, File outputFile) throws IOException {
        String json = String.format(
            "{\n" +
            "    \"content\": %s,\n" +
            "    \"metadata\": {\n" +
            "        \"timestamp\": \"%s\",\n" +
            "        \"format\": \"text\"\n" +
            "    }\n" +
            "}",
            escapeJson(content),
            new Date().toString()
        );
        
        Files.writeString(outputFile.toPath(), json);
    }
    
    // 创建输出文件路径
    private String createOutputPath(File inputFile, FileType targetType) {
        String baseName = inputFile.getName();
        int dotIndex = baseName.lastIndexOf('.');
        if (dotIndex > 0) {
            baseName = baseName.substring(0, dotIndex);
        }
        
        String outputPath = inputFile.getParent() + File.separator + 
                          baseName + "_converted" + targetType.getExtension();
        
        // 如果文件已存在，添加数字后缀
        File outputFile = new File(outputPath);
        int counter = 1;
        while (outputFile.exists()) {
            outputPath = inputFile.getParent() + File.separator + 
                        baseName + "_converted_" + counter + targetType.getExtension();
            outputFile = new File(outputPath);
            counter++;
        }
        
        return outputPath;
    }
    
    // HTML转义
    private String escapeHtml(String content) {
        return content.replace("&", "&amp;")
                     .replace("<", "&lt;")
                     .replace(">", "&gt;")
                     .replace("\"", "&quot;")
                     .replace("'", "&#39;");
    }
    
    // JSON转义
    private String escapeJson(String content) {
        return "\"" + content.replace("\\", "\\\\")
                            .replace("\"", "\\\"")
                            .replace("\n", "\\n")
                            .replace("\r", "\\r")
                            .replace("\t", "\\t") + "\"";
    }
}