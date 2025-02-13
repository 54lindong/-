package com.caseprocessor.history;

import com.caseprocessor.data.DataStore;

public class HistoryManager {
    public static void saveQueryHistory(String companyName, String caseData) {
        DataStore.save(companyName, caseData);
    }

    public static void loadHistory() {
        DataStore.loadHistory();
    }
}
