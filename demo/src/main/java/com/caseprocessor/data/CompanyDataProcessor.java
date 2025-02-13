package com.caseprocessor.data;

import com.google.gson.JsonObject;

import java.util.List;

public class CompanyDataProcessor {
    public static boolean isCompanyCancelled(JsonObject companyData) {
        String status = companyData.get("status").getAsString();
        return "cancelled".equalsIgnoreCase(status);
    }

    public static boolean compareCancellationDate(String companyCancelledDate, String judgmentDate) {
        return companyCancelledDate.compareTo(judgmentDate) < 0;
    }

    public static void processCompanyData(List<JsonObject> companyDataList, String judgmentDate) {
        for (JsonObject companyData : companyDataList) {
            if (isCompanyCancelled(companyData)) {
                String cancelledDate = companyData.get("cancelledDate").getAsString();
                if (compareCancellationDate(cancelledDate, judgmentDate)) {
                    System.out.println("Company " + companyData.get("name") + " is cancelled before judgment.");
                }
            }
        }
    }
}
