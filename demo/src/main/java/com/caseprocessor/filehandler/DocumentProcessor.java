package com.caseprocessor.filehandler;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class DocumentProcessor {
    public static class CompanyInfo {
        private String companyName;
        private String role;
        private String judgmentDate;
        private String caseNumber;
        private String courtName;
        private String caseType;
        private String judgmentResult;
        private String registrationStatus = "未查询";
        private String registrationDate = "未查询";
        private String operatingStatus = "未查询";

        // 构造函数
        public CompanyInfo(CompanyExtractor.LegalPartyInfo partyInfo) {
            this.companyName = partyInfo.getCompanyName();
            this.role = partyInfo.getPartyRole();
            this.judgmentDate = partyInfo.getJudgmentDate();
            this.caseNumber = partyInfo.getCaseNumber();
            this.courtName = partyInfo.getCourtName();
            this.caseType = partyInfo.getCaseType();
            this.judgmentResult = partyInfo.getJudgmentResult();
        }

        // Getters and Setters
        public String getCompanyName() { return companyName; }
        public void setCompanyName(String name) { this.companyName = name; }
        
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        
        public String getJudgmentDate() { return judgmentDate; }
        public void setJudgmentDate(String date) { this.judgmentDate = date; }
        
        public String getCaseNumber() { return caseNumber; }
        public void setCaseNumber(String number) { this.caseNumber = number; }
        
        public String getCourtName() { return courtName; }
        public void setCourtName(String court) { this.courtName = court; }
        
        public String getCaseType() { return caseType; }
        public void setCaseType(String type) { this.caseType = type; }
        
        public String getJudgmentResult() { return judgmentResult; }
        public void setJudgmentResult(String result) { this.judgmentResult = result; }
        
        public String getRegistrationStatus() { return registrationStatus; }
        public void setRegistrationStatus(String status) { this.registrationStatus = status; }
        
        public String getRegistrationDate() { return registrationDate; }
        public void setRegistrationDate(String date) { this.registrationDate = date; }
        
        public String getOperatingStatus() { return operatingStatus; }
        public void setOperatingStatus(String status) { this.operatingStatus = status; }
    }

    public static List<CompanyInfo> processFile(File file) throws IOException {
        String content;
        String extension = getFileExtension(file.getName()).toLowerCase();
        
        // 根据文件类型提取文本内容
        switch (extension) {
            case "pdf":
                content = PdfHandler.extractTextFromPDF(file);
                break;
            case "docx":
                content = WordHandler.extractTextFromDOCX(file);
                break;
            case "doc":
                content = WordHandler.extractTextFromDOC(file);
                break;
            default:
                throw new IOException("不支持的文件格式: " + extension);
        }
        
        // 使用CompanyExtractor提取公司信息
        List<CompanyExtractor.LegalPartyInfo> legalParties = CompanyExtractor.extractCompanyInfo(content);
        
        // 转换为CompanyInfo列表
        List<CompanyInfo> companies = new ArrayList<>();
        for (CompanyExtractor.LegalPartyInfo party : legalParties) {
            companies.add(new CompanyInfo(party));
        }
        
        return companies;
    }

    public static List<CompanyInfo> processBatch(List<File> files) throws IOException {
        List<CompanyInfo> allCompanies = new ArrayList<>();
        for (File file : files) {
            allCompanies.addAll(processFile(file));
        }
        return allCompanies;
    }

    private static String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0) {
            return fileName.substring(lastDot + 1);
        }
        return "";
    }

    // 模拟企查查API查询
    public static void mockQichachaQuery(CompanyInfo company) {
        try {
            Thread.sleep(500); // 模拟网络延迟
            
            // 模拟返回数据
            Random random = new Random();
            String[] statuses = {"在营", "注销", "吊销"};
            company.setRegistrationStatus(statuses[random.nextInt(statuses.length)]);
            company.setRegistrationDate("2010-" + (random.nextInt(12) + 1) + "-" + (random.nextInt(28) + 1));
            company.setOperatingStatus(company.getRegistrationStatus().equals("在营") ? "正常经营" : "已停业");
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}