package com.caseprocessor.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class DataStore {

    private static final int MAX_COMPANIES = 30;
    private static final String DATA_FILE_PATH = "company_data.json";

    // Mock method to load companies for a given file
    public static List<String> loadCompanies(File file) {
        List<String> companies = new ArrayList<>();
        companies.add("公司 A");
        companies.add("公司 B");
        companies.add("公司 C");
        return companies;
    }

    // Load history from a JSON file
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

    // Save a new company to the file (if there are more than 30 companies, delete the oldest one)
    public static void save(String companyName, String caseData) {
        List<String> companies = loadHistory();

        if (companies.size() >= MAX_COMPANIES) {
            // If there are more than 30 companies, remove the oldest
            companies.remove(0);
        }

        // Add new company data
        companies.add(companyName + ": " + caseData);

        // Save updated list to file
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(DATA_FILE_PATH))) {
            for (String company : companies) {
                bw.write(company);
                bw.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Remove the JSON file if there are more than 30 companies
    private static void cleanupDataFile() {
        File file = new File(DATA_FILE_PATH);
        if (file.exists()) {
            file.delete();
        }
    }
}
