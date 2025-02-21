package com.caseprocessor.filehandler;

import java.util.*;
import java.util.regex.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class CompanyExtractor {
    public static class LegalPartyInfo {
        private String companyName;      // 公司名称
        private String partyRole;        // 诉讼身份
        private String judgmentDate;     // 判决日期
        private String caseNumber;       // 案号
        private String courtName;        // 法院名称
        private String caseType;         // 案件类型
        private String judgmentResult;   // 判决结果
        private String description;      // 相关描述

        public LegalPartyInfo(String companyName, String partyRole) {
            this.companyName = companyName;
            this.partyRole = partyRole;
        }

        // Getters and Setters
        public String getCompanyName() { return companyName; }
        public void setCompanyName(String name) { this.companyName = name; }
        public String getPartyRole() { return partyRole; }
        public void setPartyRole(String role) { this.partyRole = role; }
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
        public String getDescription() { return description; }
        public void setDescription(String desc) { this.description = desc; }
    }

    // 正则表达式模式
    private static final Pattern COMPANY_PATTERN = Pattern.compile(
        "(?<role>原告|被告|上诉人|被上诉人|第三人)(?:（.*?）)?[：:](.*?(?:公司|集团|企业|工厂|商行|商店))",
        Pattern.UNICODE_CHARACTER_CLASS
    );
    
    private static final Pattern CASE_NUMBER_PATTERN = Pattern.compile(
        "（?(\\d{4})[）()].*?第[\\d-]*?号"
    );
    
    private static final Pattern COURT_NAME_PATTERN = Pattern.compile(
        "(.+?[省市区县])?(.+?人民法院)"
    );
    
    private static final Pattern JUDGMENT_DATE_PATTERN = Pattern.compile(
        "(?:二[〇Ｏ零])?[一二三四五六七八九零〇Ｏ]{2,4}年[一二三四五六七八九十]{1,2}月[一二三四五六七八九十]{1,2}日"
    );

    public static List<LegalPartyInfo> extractCompanyInfo(String content) {
        List<LegalPartyInfo> parties = new ArrayList<>();
        Map<String, LegalPartyInfo> uniqueCompanies = new HashMap<>();

        // 提取文书基本信息
        String caseNumber = extractCaseNumber(content);
        String courtName = extractCourtName(content);
        String judgmentDate = extractJudgmentDate(content);
        String caseType = determineCaseType(content);
        String judgmentResult = determineJudgmentResult(content);

        // 提取公司信息
        Matcher matcher = COMPANY_PATTERN.matcher(content);
        while (matcher.find()) {
            String role = matcher.group("role");
            String companyName = matcher.group(2).trim();
            
            if (!isValidCompanyName(companyName)) continue;

            LegalPartyInfo partyInfo = uniqueCompanies.getOrDefault(companyName, 
                new LegalPartyInfo(companyName, role));
            
            partyInfo.setPartyRole(role);
            partyInfo.setCaseNumber(caseNumber);
            partyInfo.setCourtName(courtName);
            partyInfo.setJudgmentDate(judgmentDate);
            partyInfo.setCaseType(caseType);
            partyInfo.setJudgmentResult(judgmentResult);
            
            String description = extractCompanyDescription(content, companyName);
            partyInfo.setDescription(description);
            
            uniqueCompanies.put(companyName, partyInfo);
        }

        return new ArrayList<>(uniqueCompanies.values());
    }

    private static String extractCaseNumber(String content) {
        Matcher matcher = CASE_NUMBER_PATTERN.matcher(content);
        return matcher.find() ? matcher.group() : "";
    }

    private static String extractCourtName(String content) {
        Matcher matcher = COURT_NAME_PATTERN.matcher(content);
        return matcher.find() ? matcher.group() : "";
    }

    private static String extractJudgmentDate(String content) {
        Matcher matcher = JUDGMENT_DATE_PATTERN.matcher(content);
        if (matcher.find()) {
            String chineseDate = matcher.group();
            return convertChineseDate(chineseDate);
        }
        return "";
    }

    private static String convertChineseDate(String chineseDate) {
        Map<String, String> numMap = new HashMap<>();
        numMap.put("〇", "0"); numMap.put("零", "0"); numMap.put("一", "1");
        numMap.put("二", "2"); numMap.put("三", "3"); numMap.put("四", "4");
        numMap.put("五", "5"); numMap.put("六", "6"); numMap.put("七", "7");
        numMap.put("八", "8"); numMap.put("九", "9"); numMap.put("十", "10");
        
        String result = chineseDate;
        for (Map.Entry<String, String> entry : numMap.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        
        result = result.replace("十", "10");
        result = result.replace("年", "-").replace("月", "-").replace("日", "");
        
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-M-d");
            LocalDate date = LocalDate.parse(result, formatter);
            return date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception e) {
            return result;
        }
    }

    private static String determineCaseType(String content) {
        if (content.contains("知识产权") || content.contains("专利") || content.contains("商标")) 
            return "知识产权纠纷";
        if (content.contains("合同") || content.contains("协议")) 
            return "合同纠纷";
        if (content.contains("劳动") || content.contains("工伤")) 
            return "劳动纠纷";
        if (content.contains("金融") || content.contains("借贷")) 
            return "金融纠纷";
        if (content.contains("刑事")) 
            return "刑事案件";
        return "其他纠纷";
    }

    private static String determineJudgmentResult(String content) {
        if (content.contains("驳回上诉") || content.contains("维持原判")) 
            return "维持原判";
        if (content.contains("改判")) 
            return "改判";
        if (content.contains("调解") || content.contains("和解")) 
            return "调解结案";
        if (content.contains("撤诉")) 
            return "撤诉";
        return "其他";
    }

    private static String extractCompanyDescription(String content, String companyName) {
        int index = content.indexOf(companyName);
        if (index == -1) return "";

        int start = Math.max(0, index - 200);
        int end = Math.min(content.length(), index + 200);
        String context = content.substring(start, end);

        Pattern sentencePattern = Pattern.compile("[^。！？]+[。！？]");
        Matcher matcher = sentencePattern.matcher(context);
        
        while (matcher.find()) {
            String sentence = matcher.group();
            if (sentence.contains(companyName)) {
                return sentence.trim();
            }
        }
        return "";
    }

    private static boolean isValidCompanyName(String name) {
        if (name == null || name.trim().isEmpty()) return false;
        
        return name.matches(".*?(?:公司|集团|企业|工厂|商行|商店)$") &&
               !name.contains("暂无") &&
               !name.contains("未知") &&
               !name.contains("该公司") &&
               !name.contains("此公司");
    }
}