package com.caseprocessor.data;

import com.caseprocessor.api.QichachaApi;
import com.caseprocessor.api.QichachaApi.CompanyInfo;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.time.format.DateTimeParseException;

public class CompanyDataProcessor {
    private final QichachaApi api;
    private final DataStore dataStore;
    
    public static class CompanyAnalysisResult {
        private String companyName;
        private String caseRole;
        private String judgmentDate;
        private String companyStatus;
        private String registrationDate;
        private String cancelDate;
        private String businessScope;
        private String legalPerson;
        private boolean hasProblem;
        private String problemDescription;
        private long caseId;
        
        public CompanyAnalysisResult(String companyName, String caseRole) {
            this.companyName = companyName;
            this.caseRole = caseRole;
        }
        
        // Getters and Setters
        public String getCompanyName() { return companyName; }
        public void setCompanyName(String name) { this.companyName = name; }
        
        public String getCaseRole() { return caseRole; }
        public void setCaseRole(String role) { this.caseRole = role; }
        
        public String getJudgmentDate() { return judgmentDate; }
        public void setJudgmentDate(String date) { this.judgmentDate = date; }
        
        public String getCompanyStatus() { return companyStatus; }
        public void setCompanyStatus(String status) { this.companyStatus = status; }
        
        public String getRegistrationDate() { return registrationDate; }
        public void setRegistrationDate(String date) { this.registrationDate = date; }
        
        public String getCancelDate() { return cancelDate; }
        public void setCancelDate(String date) { this.cancelDate = date; }
        
        public String getBusinessScope() { return businessScope; }
        public void setBusinessScope(String scope) { this.businessScope = scope; }
        
        public String getLegalPerson() { return legalPerson; }
        public void setLegalPerson(String person) { this.legalPerson = person; }
        
        public boolean isHasProblem() { return hasProblem; }
        public void setHasProblem(boolean problem) { this.hasProblem = problem; }
        
        public String getProblemDescription() { return problemDescription; }
        public void setProblemDescription(String description) { this.problemDescription = description; }
        
        public long getCaseId() { return caseId; }
        public void setCaseId(long id) { this.caseId = id; }
    }
    
    public CompanyDataProcessor(QichachaApi api) {
        this.api = api;
        this.dataStore = DataStore.getInstance();
    }
    
    public List<CompanyAnalysisResult> analyzeCompanies(
            List<Map.Entry<String, String>> companies, String judgmentDate) {
        List<CompanyAnalysisResult> results = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(5);
        List<Future<CompanyAnalysisResult>> futures = new ArrayList<>();
        
        // 创建新的案件记录
        long caseId = dataStore.saveCaseInfo(
            "", judgmentDate, "", "", ""
        );
        
        // 提交所有查询任务
        for (Map.Entry<String, String> company : companies) {
            futures.add(executor.submit(() -> 
                analyzeCompany(company.getKey(), company.getValue(), judgmentDate, caseId)));
        }
        
        // 收集结果
        for (Future<CompanyAnalysisResult> future : futures) {
            try {
                CompanyAnalysisResult result = future.get(30, TimeUnit.SECONDS);
                results.add(result);
                // 保存公司分析结果
                dataStore.saveCompanyInfo(caseId, result);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        executor.shutdown();
        return results;
    }
    
    private CompanyAnalysisResult analyzeCompany(
            String companyName, String role, String judgmentDate, long caseId) {
        CompanyAnalysisResult result = new CompanyAnalysisResult(companyName, role);
        result.setJudgmentDate(judgmentDate);
        result.setCaseId(caseId);
        
        try {
            CompanyInfo info = api.getCompanyInfoWithRetry(companyName);
            
            result.setCompanyStatus(info.getStatus());
            result.setRegistrationDate(info.getRegistrationDate());
            result.setCancelDate(info.getCancelDate());
            result.setBusinessScope(info.getBusinessScope());
            result.setLegalPerson(info.getLegalPerson());
            
            // 检查公司状态
            checkCompanyStatus(result, info);
            
        } catch (Exception e) {
            result.setCompanyStatus("查询失败");
            result.setProblemDescription("无法获取公司信息: " + e.getMessage());
        }
        
        return result;
    }
    
    private void checkCompanyStatus(CompanyAnalysisResult result, CompanyInfo info) {
        boolean hasIssue = false;
        StringBuilder issueDesc = new StringBuilder();
        
        if ("注销".equals(info.getStatus()) || "吊销".equals(info.getStatus())) {
            try {
                LocalDate cancelDate = parseDate(info.getCancelDate());
                LocalDate judgmentDate = parseDate(result.getJudgmentDate());
                
                if (cancelDate != null && judgmentDate != null && 
                    cancelDate.isBefore(judgmentDate)) {
                    hasIssue = true;
                    issueDesc.append(String.format(
                        "公司于%s已%s，但判决时间为%s",
                        info.getCancelDate(),
                        info.getStatus(),
                        result.getJudgmentDate()
                    ));
                }
            } catch (DateTimeParseException e) {
                // 日期解析失败，记录但不标记为问题
                issueDesc.append("日期格式无法解析，请手动核实 ");
            }
        }
        
        // 其他合规性检查
        if (!checkBusinessScope(info.getBusinessScope())) {
            if (hasIssue) issueDesc.append("; ");
            hasIssue = true;
            issueDesc.append("经营范围可能存在问题");
        }
        
        result.setHasProblem(hasIssue);
        if (hasIssue) {
            result.setProblemDescription(issueDesc.toString());
        }
    }
    
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        
        try {
            // 处理常见的日期格式
            String normalizedDate = dateStr.replace("年", "-")
                                        .replace("月", "-")
                                        .replace("日", "");
            return LocalDate.parse(normalizedDate, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
    
    private boolean checkBusinessScope(String scope) {
        if (scope == null || scope.trim().isEmpty()) {
            return true;
        }
        
        // 检查经营范围中的敏感词
        List<String> sensitiveWords = Arrays.asList(
            "违法", "违规", "非法", "禁止"
        );
        
        for (String word : sensitiveWords) {
            if (scope.contains(word)) {
                return false;
            }
        }
        
        return true;
    }
    
    public void saveAnalysisResult(List<CompanyAnalysisResult> results) {
        for (CompanyAnalysisResult result : results) {
            dataStore.saveCompanyInfo(result.getCaseId(), result);
        }
    }
    
    public List<CompanyAnalysisResult> getHistoricalAnalysis(long caseId) {
        return dataStore.getCaseCompanies(caseId);
    }
    
    public void exportToExcel(List<CompanyAnalysisResult> results, String filePath) {
        // TODO: 实现导出到Excel的功能
    }
    
    public void exportToWord(List<CompanyAnalysisResult> results, String filePath) {
        // TODO: 实现导出到Word的功能
    }
}