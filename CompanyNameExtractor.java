package com.caseprocessor.textprocessor;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class CompanyNameExtractor {

    // 提取文档中的所有文本
    public static String extractTextFromDocx(File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        XWPFDocument document = new XWPFDocument(fis);
        StringBuilder text = new StringBuilder();

        document.getParagraphs().forEach(paragraph -> text.append(paragraph.getText()).append("\n"));

        fis.close();
        return text.toString();
    }

    // 使用正则表达式提取公司名称
    public static List<String> extractCompanies(String text) {
        List<String> companies = new ArrayList<>();

        // 正则表达式匹配公司名称
        String companyPattern = "(?:上诉人|被上诉人|申请人|被申请人)(?:（(?:原审)?(?:原告|被告|第三人)）)?(?::|：|,|，)\\s*([\\u4e00-\\u9fa5A-Za-z0-9\\-()（）]+?(?:公司|集团|医院|事务所))";
        
        Pattern pattern = Pattern.compile(companyPattern);
        Matcher matcher = pattern.matcher(text);

        // 提取匹配到的公司名称
        while (matcher.find()) {
            String companyName = matcher.group(matcher.groupCount());
            if (companyName != null) {
                companies.add(companyName.trim());
            }
        }

        // 去重并返回结果
        return new ArrayList<>(new HashSet<>(companies));
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

    // 提取公司名称和审判时间并返回格式化的List
    public static List<String> extractCompaniesAndJudgmentDate(String text) {
        List<String> results = new ArrayList<>();
        
        // 提取公司名
        results.addAll(extractCompanies(text));
        
        // 提取审判时间
        String judgmentDate = extractJudgmentDate(text);
        if (judgmentDate != null) {
            results.add(judgmentDate);
        }

        return results;
    }

    // 批量处理多个判决书文件
    public static List<List<String>> batchExtractCompaniesAndJudgmentDates(List<File> files) throws IOException {
        List<List<String>> results = new ArrayList<>();

        for (File file : files) {
            String text = extractTextFromDocx(file);
            List<String> extractedInfo = extractCompaniesAndJudgmentDate(text);
            results.add(extractedInfo);
        }

        return results;
    }

    // 示例使用方法
    public static void main(String[] args) throws IOException {
        // 示例：处理多个文件并输出结果
        List<File> files = Arrays.asList(
            new File("document1.docx"),
            new File("document2.docx")
        );

        List<List<String>> results = batchExtractCompaniesAndJudgmentDates(files);
        
        for (int i = 0; i < results.size(); i++) {
            System.out.println("第" + (i + 1) + "份文件：");
            List<String> fileResults = results.get(i);
            if (fileResults.size() > 0) {
                // 最后一个元素是日期，其他都是公司名
                String date = fileResults.get(fileResults.size() - 1);
                List<String> companies = fileResults.subList(0, fileResults.size() - 1);
                
                System.out.println("公司：" + String.join("、", companies));
                System.out.println("判决日期：" + date);
            }
            System.out.println();
        }
    }
}