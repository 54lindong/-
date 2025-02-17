package com.caseprocessor.api;

import okhttp3.*;

import java.io.IOException;

public class QiChaChaApi {
    private static final String API_URL = "https://api.qichacha.com/...";  // Example API URL
    private static final String API_KEY = "your_api_key_here";

    // 获取公司信息
    public static String getCompanyInfo(String companyName) throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(API_URL + "?company=" + companyName)
                .header("Authorization", "Bearer " + API_KEY)
                .build();
        
        Response response = client.newCall(request).execute();
        if (response.isSuccessful()) {
            return response.body().string();
        } else {
            throw new IOException("Failed to retrieve data from QiChaCha API");
        }
    }
}
