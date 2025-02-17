package com.caseprocessor.data;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class DataStore {

    private static final int MAX_COMPANIES = 30;
    private static final String DATA_FILE_PATH = "company_data.json";

    // 加载公司数据（模拟）
    public static List<String> loadCompanies(File file) {
        List<String> companies = new ArrayList<>();
        companies.add("公司 A");
        companies.add("公司 B");
        companies.add("公司 C");
        return companies;
    }

    // 加载历史记录
    public static List<String> loadHistory() {
        List<String> history = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(DATA_FILE_PATH))) {
            String line;
            while ((line = br.readLine()) != null) {
                history.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return history;
    }

    // 保存公司数据（最多30个历史记录）
    public static void save(String companyName, String caseData) {
        List<String> companies = loadHistory();
        if (companies.size() >= MAX_COMPANIES) {
            companies.remove(0);
        }
        companies.add(companyName + ": " + caseData);
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(DATA_FILE_PATH))) {
            for (String company : companies) {
                bw.write(company);
                bw.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
