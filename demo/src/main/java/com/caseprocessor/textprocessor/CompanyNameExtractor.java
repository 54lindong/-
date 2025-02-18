package com.caseprocessor.textprocessor;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class CompanyNameExtractor {
    // Company类用于存储公司信息
    public static class Company {
        private String name;        // 公司名称
        private String identity;    // 身份（上诉人/被上诉人等）
        private String role;        // 原告/被告/第三人角色（如果有）

        public Company(String name, String identity, String role) {
            this.name = name;
            this.identity = identity;
            this.role = role;
        }

        public String getName() { return name; }
        public String getIdentity() { return identity; }
        public String getRole() { return role; }

        @Override
        public String toString() {
            if (role != null && !role.isEmpty()) {
                return identity + "（" + role + "）：" + name;
            }
            return identity + "：" + name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Company company = (Company) o;
            return Objects.equals(name, company.name) && 
                   Objects.equals(identity, company.identity) &&
                   Objects.equals(role, company.role);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, identity, role);
        }
    }

    // 提取文档中的所有文本
    public static String extractTextFromDocx(File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        XWPFDocument document = new XWPFDocument(fis);
        StringBuilder text = new StringBuilder();

        document.getParagraphs().forEach(paragraph -> text.append(paragraph.getText()).append("\n"));

        fis.close();
        return text.toString();
    }

    // 使用正则表达式提取公司名称和身份
    public static List<Company> extractCompanies(String text) {
        List<Company> companies = new ArrayList<>();

        // 正则表达式匹配公司名称、身份和角色
        String companyPattern = "(上诉人|被上诉人|申请人|被申请人)(?:（((?:原审)?(?:原告|被告|第三人))）)?(?::|：|,|，)\\s*([\\u4e00-\\u9fa5A-Za-z0-9\\-()（）]+?(?:公司|集团|医院|事务所))";
        
        Pattern pattern = Pattern.compile(companyPattern);
        Matcher matcher = pattern.matcher(text);

        // 提取匹配到的公司信息
        while (matcher.find()) {
            String identity = matcher.group(1);  // 身份
            String role = matcher.group(2);      // 角色（可能为null）
            String name = matcher.group(3);      // 公司名称
            
            if (name != null && identity != null) {
                companies.add(new Company(name.trim(), identity, role));
            }
        }

        // 去重并返回结果
        return new ArrayList<>(new LinkedHashSet<>(companies));
    }

    // 提取审判时间
    public static String extractJudgmentDate(String text) {
        String judgmentDate = null;

        // 正则表达式:提取日期格式
        String datePattern = "(?:二[〇Ｏ])?[一二三四五六七八九零〇Ｏ]{2,4}年(?:[一二三四五六七八九十〇Ｏ]{1,2}|十[一二]?)月(?:[一二三四五六七八九十〇Ｏ]{1,3}|二?十[一二三四五六七八九]?)日";

        Pattern pattern = Pattern.compile(datePattern);
        Matcher matcher = pattern.matcher(text);

        // 查找最后一个匹配的日期
        while (matcher.find()) {
            judgmentDate = matcher.group();
        }

        if (judgmentDate != null) {
            judgmentDate = convertChineseNumberToArabic(judgmentDate);
        }

        return judgmentDate;
    }

    // 将中文数字转换为阿拉伯数字
    private static String convertChineseNumberToArabic(String chineseNumber) {
        // 基础数字映射
        Map<String, Integer> numMap = new HashMap<>();
        numMap.put("零", 0); numMap.put("一", 1); numMap.put("二", 2); numMap.put("三", 3);
        numMap.put("四", 4); numMap.put("五", 5); numMap.put("六", 6); numMap.put("七", 7);
        numMap.put("八", 8); numMap.put("九", 9); numMap.put("十", 10);
        numMap.put("〇", 0); numMap.put("Ｏ", 0);

        // 分离年月日
        String[] parts = chineseNumber.split("[年月日]");
        StringBuilder result = new StringBuilder();

        // 处理年份
        String year = parts[0];
        if (year.startsWith("二〇") || year.startsWith("二Ｏ")) {
            year = "2" + year.substring(2);
        }
        result.append(convertChineseNumberPart(year, numMap)).append("-");

        // 处理月份
        String month = parts[1];
        int monthNum = parseChineseNumber(month, numMap);
        result.append(monthNum).append("-");

        // 处理日期
        String day = parts[2];
        int dayNum = parseChineseNumber(day, numMap);
        result.append(dayNum);

        return result.toString();
    }

    // 解析中文数字部分
    private static int parseChineseNumber(String chineseNum, Map<String, Integer> numMap) {
        if (chineseNum.length() == 1) {
            return numMap.get(chineseNum);
        }
        
        if (chineseNum.startsWith("十")) {
            if (chineseNum.length() == 1) {
                return 10;
            }
            return 10 + numMap.get(chineseNum.substring(1));
        }
        
        if (chineseNum.contains("十")) {
            String[] parts = chineseNum.split("十");
            if (parts.length == 1) {
                return numMap.get(parts[0]) * 10;
            }
            return numMap.get(parts[0]) * 10 + (parts[1].isEmpty() ? 0 : numMap.get(parts[1]));
        }
        
        return numMap.get(chineseNum);
    }

    // 转换中文数字部分
    private static String convertChineseNumberPart(String chinesePart, Map<String, Integer> numMap) {
        StringBuilder result = new StringBuilder();
        for (char c : chinesePart.toCharArray()) {
            String digit = String.valueOf(c);
            if (numMap.containsKey(digit)) {
                result.append(numMap.get(digit));
            }
        }
        return result.toString();
    }

    public static class ExtractionResult {
        private List<Company> companies;
        private String date;
        private String caseName;    // 案件名称
        private String caseNumber;  // 案号

        public ExtractionResult(List<Company> companies, String date, String caseName, String caseNumber) {
            this.companies = companies;
            this.date = date;
            this.caseName = caseName;
            this.caseNumber = caseNumber;
        }

        public List<Company> getCompanies() { return companies; }
        public String getDate() { return date; }
        public String getCaseName() { return caseName; }
        public String getCaseNumber() { return caseNumber; }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (caseName != null) sb.append("案件名称：").append(caseName).append("\n");
            if (caseNumber != null) sb.append("案号：").append(caseNumber).append("\n");
            sb.append("当事人信息：\n");
            for (Company company : companies) {
                sb.append(company.toString()).append("\n");
            }
            if (date != null) sb.append("判决日期：").append(date);
            return sb.toString();
        }
    }

    // 提取案件名称
    private static String extractCaseName(String text) {
        Pattern pattern = Pattern.compile("^.*?(?:判决书|裁定书|决定书|通知书)");
        Matcher matcher = pattern.matcher(text.trim());
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    // 提取案号
    private static String extractCaseNumber(String text) {
        Pattern pattern = Pattern.compile("\\（?[0-9]{4}\\）?[京津沪渝冀豫云辽黑湘皖鲁新苏浙赣鄂桂甘晋蒙陕吉闽贵粤青藏川宁琼使军].*?号");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    // 提取单个文件的完整信息
    public static ExtractionResult extractInformation(String text) {
        List<Company> companies = extractCompanies(text);
        String date = extractJudgmentDate(text);
        String caseName = extractCaseName(text);
        String caseNumber = extractCaseNumber(text);
        
        return new ExtractionResult(companies, date, caseName, caseNumber);
    }

    // 批量处理多个判决书文件
    public static List<ExtractionResult> batchExtractInformation(List<File> files) throws IOException {
        List<ExtractionResult> results = new ArrayList<>();

        for (File file : files) {
            String text = extractTextFromDocx(file);
            ExtractionResult extractedInfo = extractInformation(text);
            results.add(extractedInfo);
        }

        return results;
    }
}