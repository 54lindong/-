package com.caseprocessor.textprocessor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.HashSet;

public class CompanyNameExtractor {

    // 使用正则表达式提取公司名称
    public static List<String> extractCompanies(String text) {
        List<String> companies = new ArrayList<>();

        // 正则表达式用于提取以"公司"结尾的公司名称
        Pattern pattern = Pattern.compile("([\\u4e00-\\u9fa5A-Za-z0-9\\-]+公司)");
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            companies.add(matcher.group());
        }

        // 去重
        return new ArrayList<>(new HashSet<>(companies));
    }

    // 增加提取审判时间的功能
    public static String extractJudgmentDate(String text) {
        String judgmentDate = null;

        // 正则表达式：匹配常见的日期格式，提取审判时间
        Pattern datePattern = Pattern.compile("[二零一二三四五六七八九十0-9年]*[月日]");
        Matcher dateMatcher = datePattern.matcher(text);

        if (dateMatcher.find()) {
            judgmentDate = dateMatcher.group();
        }

        return judgmentDate;
    }

    // 提取公司名称和审判时间
    public static List<String> extractCompaniesAndJudgmentDate(String text) {
        List<String> results = new ArrayList<>();

        // 提取公司名
        List<String> companies = extractCompanies(text);
        results.add("提取到的公司名称：");
        companies.forEach(results::add);

        // 提取审判时间
        String judgmentDate = extractJudgmentDate(text);
        results.add("审判时间：" + judgmentDate);

        return results;
    }
}
