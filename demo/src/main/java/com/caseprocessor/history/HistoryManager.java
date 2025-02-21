package com.caseprocessor.history;

import com.caseprocessor.filehandler.DocumentProcessor.CompanyInfo;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class HistoryManager {
    private static final String HISTORY_DIR = "history";
    private static final String COMPANY_DATA_DIR = HISTORY_DIR + "/company_data";
    private static final String HISTORY_LOG_FILE = HISTORY_DIR + "/history.log";
    
    public static class HistoryRecord {
        private String timestamp;
        private String companyName;
        private String documentPath;
        private String jsonPath;
        
        public HistoryRecord(String timestamp, String companyName, 
                           String documentPath, String jsonPath) {
            this.timestamp = timestamp;
            this.companyName = companyName;
            this.documentPath = documentPath;
            this.jsonPath = jsonPath;
        }
        
        // Getters
        public String getTimestamp() { return timestamp; }
        public String getCompanyName() { return companyName; }
        public String getDocumentPath() { return documentPath; }
        public String getJsonPath() { return jsonPath; }
    }
    
    public HistoryManager() {
        initializeDirectories();
    }
    
    private void initializeDirectories() {
        try {
            Files.createDirectories(Paths.get(HISTORY_DIR));
            Files.createDirectories(Paths.get(COMPANY_DATA_DIR));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void saveRecord(String companyName, File originalDoc, CompanyInfo companyInfo) {
        try {
            // 生成时间戳
            String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            
            // 复制原始文档
            String docCopyPath = COMPANY_DATA_DIR + "/" + timestamp + "_" + 
                originalDoc.getName();
            Files.copy(originalDoc.toPath(), Paths.get(docCopyPath), 
                StandardCopyOption.REPLACE_EXISTING);
            
            // 保存公司信息为JSON
            String jsonPath = COMPANY_DATA_DIR + "/" + timestamp + "_" + 
                companyName + ".json";
            saveCompanyInfoAsJson(companyInfo, jsonPath);
            
            // 记录到历史日志
            String logEntry = String.format("%s|%s|%s|%s%n",
                timestamp, companyName, docCopyPath, jsonPath);
            Files.write(Paths.get(HISTORY_LOG_FILE), 
                logEntry.getBytes(), 
                StandardOpenOption.CREATE, 
                StandardOpenOption.APPEND);
                
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void saveCompanyInfoAsJson(CompanyInfo companyInfo, String path) 
        throws IOException {
        // TODO: 使用JSON库将CompanyInfo对象序列化为JSON
        String jsonContent = String.format(
            "{\n" +
            "  \"companyName\": \"%s\",\n" +
            "  \"role\": \"%s\",\n" +
            "  \"judgmentDate\": \"%s\",\n" +
            "  \"registrationStatus\": \"%s\",\n" +
            "  \"registrationDate\": \"%s\",\n" +
            "  \"operatingStatus\": \"%s\"\n" +
            "}",
            companyInfo.getCompanyName(),
            companyInfo.getRole(),
            companyInfo.getJudgmentDate(),
            companyInfo.getRegistrationStatus(),
            companyInfo.getRegistrationDate(),
            companyInfo.getOperatingStatus()
        );
        
        Files.write(Paths.get(path), jsonContent.getBytes());
    }
    
    public List<HistoryRecord> getHistoryRecords() {
        List<HistoryRecord> records = new ArrayList<>();
        try {
            if (Files.exists(Paths.get(HISTORY_LOG_FILE))) {
                List<String> lines = Files.readAllLines(Paths.get(HISTORY_LOG_FILE));
                for (String line : lines) {
                    String[] parts = line.split("\\|");
                    if (parts.length == 4) {
                        records.add(new HistoryRecord(
                            parts[0], parts[1], parts[2], parts[3]));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return records;
    }
    
    public CompanyInfo loadCompanyInfo(String jsonPath) {
        try {
            String jsonContent = new String(Files.readAllBytes(Paths.get(jsonPath)));
            // TODO: 使用JSON库将JSON内容反序列化为CompanyInfo对象
            return null; // 临时返回空，等待实现
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public void clearHistory() {
        try {
            // 删除历史记录文件
            Files.deleteIfExists(Paths.get(HISTORY_LOG_FILE));
            // 删除公司数据目录中的所有文件
            if (Files.exists(Paths.get(COMPANY_DATA_DIR))) {
                Files.walk(Paths.get(COMPANY_DATA_DIR))
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            }
            // 重新创建目录
            initializeDirectories();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}