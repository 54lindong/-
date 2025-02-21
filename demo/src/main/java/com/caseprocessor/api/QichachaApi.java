package com.caseprocessor.api;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class QichachaApi {
    private final String appKey;
    private final String secretKey;
    private final String baseUrl;
    private final HttpClient httpClient;
    private final Gson gson;
    
    public static class CompanyInfo {
        private String name;             // 公司名称
        private String status;           // 经营状态
        private String registrationDate; // 注册时间
        private String cancelDate;       // 注销时间（如果有）
        private String legalPerson;      // 法定代表人
        private String registeredCapital;// 注册资本
        private String industry;         // 所属行业
        private String creditCode;       // 统一社会信用代码
        private String address;          // 企业地址
        private String businessScope;    // 经营范围
        
        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getRegistrationDate() { return registrationDate; }
        public void setRegistrationDate(String date) { this.registrationDate = date; }
        public String getCancelDate() { return cancelDate; }
        public void setCancelDate(String date) { this.cancelDate = date; }
        public String getLegalPerson() { return legalPerson; }
        public void setLegalPerson(String person) { this.legalPerson = person; }
        public String getRegisteredCapital() { return registeredCapital; }
        public void setRegisteredCapital(String capital) { this.registeredCapital = capital; }
        public String getIndustry() { return industry; }
        public void setIndustry(String industry) { this.industry = industry; }
        public String getCreditCode() { return creditCode; }
        public void setCreditCode(String code) { this.creditCode = code; }
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        public String getBusinessScope() { return businessScope; }
        public void setBusinessScope(String scope) { this.businessScope = scope; }
    }
    
    public QichachaApi(String appKey, String secretKey) {
        this.appKey = appKey;
        this.secretKey = secretKey;
        this.baseUrl = ApiConfig.getInstance().getApiUrl();
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(
                ApiConfig.getInstance().getRequestTimeout()))
            .build();
        this.gson = new Gson();
    }
    
    public CompanyInfo getCompanyInfo(String companyName) throws IOException, InterruptedException {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String path = "/base/search";
        
        // 构建请求头
        String token = generateToken(path, timestamp);
        
        // 构建请求
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + path + "?keyword=" + 
                java.net.URLEncoder.encode(companyName, "UTF-8")))
            .header("Token", token)
            .header("Timestamp", timestamp)
            .header("AppKey", appKey)
            .GET()
            .build();
        
        // 发送请求
        HttpResponse<String> response = httpClient.send(request, 
            HttpResponse.BodyHandlers.ofString());
        
        // 解析响应
        return parseResponse(response.body());
    }
    
    private String generateToken(String path, String timestamp) {
        try {
            String content = path + "|" + appKey + "|" + timestamp;
            
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                secretKey.getBytes(), "HmacSHA256");
            hmac.init(secretKeySpec);
            
            byte[] bytes = hmac.doFinal(content.getBytes());
            return Base64.getEncoder().encodeToString(bytes);
            
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("生成Token失败", e);
        }
    }
    
    private CompanyInfo parseResponse(String responseBody) {
        JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
        
        // 检查响应状态
        int status = jsonResponse.get("status").getAsInt();
        if (status != 200) {
            throw new RuntimeException("API请求失败: " + 
                jsonResponse.get("message").getAsString());
        }
        
        // 解析公司信息
        JsonObject data = jsonResponse.getAsJsonObject("data");
        CompanyInfo info = new CompanyInfo();
        
        info.setName(getJsonString(data, "Name"));
        info.setStatus(getJsonString(data, "Status"));
        info.setRegistrationDate(getJsonString(data, "StartDate"));
        info.setCancelDate(getJsonString(data, "CancelDate"));
        info.setLegalPerson(getJsonString(data, "LegalPerson"));
        info.setRegisteredCapital(getJsonString(data, "RegistCapi"));
        info.setIndustry(getJsonString(data, "Industry"));
        info.setCreditCode(getJsonString(data, "CreditCode"));
        info.setAddress(getJsonString(data, "Address"));
        info.setBusinessScope(getJsonString(data, "BusinessScope"));
        
        return info;
    }
    
    private String getJsonString(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull() ? 
            json.get(key).getAsString() : "";
    }
    
    // 带重试的API调用
    public CompanyInfo getCompanyInfoWithRetry(String companyName) 
            throws IOException, InterruptedException {
        int maxRetries = ApiConfig.getInstance().getRetryCount();
        int retryCount = 0;
        
        while (true) {
            try {
                return getCompanyInfo(companyName);
            } catch (Exception e) {
                if (++retryCount >= maxRetries) {
                    throw e;
                }
                // 等待一段时间后重试
                Thread.sleep(1000 * retryCount);
            }
        }
    }
}