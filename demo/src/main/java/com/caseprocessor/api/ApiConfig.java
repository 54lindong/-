package com.caseprocessor.api;

import java.io.*;
import java.util.Properties;
import java.nio.file.Files;
import java.nio.file.Paths;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Insets;

public class ApiConfig {
    private static final String CONFIG_FILE = "config/api.properties";
    private static ApiConfig instance;
    private Properties properties;
    
    private ApiConfig() {
        properties = new Properties();
        loadConfig();
    }
    
    public static synchronized ApiConfig getInstance() {
        if (instance == null) {
            instance = new ApiConfig();
        }
        return instance;
    }
    
    private void loadConfig() {
        try {
            // 确保配置目录存在
            Files.createDirectories(Paths.get("config"));
            
            File configFile = new File(CONFIG_FILE);
            if (!configFile.exists()) {
                createDefaultConfig();
            }
            
            try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
                properties.load(fis);
            }
        } catch (IOException e) {
            System.err.println("加载配置文件失败: " + e.getMessage());
        }
    }
    
    private void createDefaultConfig() {
        properties.setProperty("qichacha.api.key", "");
        properties.setProperty("qichacha.api.secret", "");
        properties.setProperty("qichacha.api.url", "https://api.qichacha.com/api/v1");
        properties.setProperty("api.request.timeout", "10");
        properties.setProperty("api.retry.count", "3");
        
        saveConfig();
    }
    
    public void saveConfig() {
        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            properties.store(fos, "API Configuration");
        } catch (IOException e) {
            System.err.println("保存配置文件失败: " + e.getMessage());
        }
    }
    
    // Getters and Setters
    public String getApiKey() {
        return properties.getProperty("qichacha.api.key", "");
    }
    
    public void setApiKey(String apiKey) {
        properties.setProperty("qichacha.api.key", apiKey);
        saveConfig();
    }
    
    public String getApiSecret() {
        return properties.getProperty("qichacha.api.secret", "");
    }
    
    public void setApiSecret(String apiSecret) {
        properties.setProperty("qichacha.api.secret", apiSecret);
        saveConfig();
    }
    
    public String getApiUrl() {
        return properties.getProperty("qichacha.api.url");
    }
    
    public int getRequestTimeout() {
        return Integer.parseInt(properties.getProperty("api.request.timeout", "10"));
    }
    
    public void setRequestTimeout(int timeout) {
        properties.setProperty("api.request.timeout", String.valueOf(timeout));
        saveConfig();
    }
    
    public int getRetryCount() {
        return Integer.parseInt(properties.getProperty("api.retry.count", "3"));
    }
    
    public void setRetryCount(int count) {
        properties.setProperty("api.retry.count", String.valueOf(count));
        saveConfig();
    }
    
    // API配置窗口
    public static class ApiConfigWindow extends Stage {
        private ApiConfig config;
        
        public ApiConfigWindow() {
            config = ApiConfig.getInstance();
            
            setTitle("API配置");
            
            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(20));
            
            // API Key输入框
            Label keyLabel = new Label("API Key:");
            TextField keyField = new TextField(config.getApiKey());
            
            // API Secret输入框
            Label secretLabel = new Label("API Secret:");
            PasswordField secretField = new PasswordField();
            secretField.setText(config.getApiSecret());
            
            // 超时设置
            Label timeoutLabel = new Label("请求超时(秒):");
            TextField timeoutField = new TextField(
                String.valueOf(config.getRequestTimeout()));
            
            // 重试次数设置
            Label retryLabel = new Label("重试次数:");
            TextField retryField = new TextField(
                String.valueOf(config.getRetryCount()));
            
            // 测试连接按钮
            Button testButton = new Button("测试连接");
            testButton.setOnAction(e -> testApiConnection());
            
            // 保存按钮
            Button saveButton = new Button("保存");
            saveButton.setOnAction(e -> {
                config.setApiKey(keyField.getText());
                config.setApiSecret(secretField.getText());
                try {
                    config.setRequestTimeout(Integer.parseInt(timeoutField.getText()));
                    config.setRetryCount(Integer.parseInt(retryField.getText()));
                    
                    showAlert("成功", "配置已保存", Alert.AlertType.INFORMATION);
                    close();
                } catch (NumberFormatException ex) {
                    showAlert("错误", "请输入有效的数字", Alert.AlertType.ERROR);
                }
            });
            
            // 布局
            grid.add(keyLabel, 0, 0);
            grid.add(keyField, 1, 0);
            grid.add(secretLabel, 0, 1);
            grid.add(secretField, 1, 1);
            grid.add(timeoutLabel, 0, 2);
            grid.add(timeoutField, 1, 2);
            grid.add(retryLabel, 0, 3);
            grid.add(retryField, 1, 3);
            
            HBox buttonBox = new HBox(10);
            buttonBox.getChildren().addAll(testButton, saveButton);
            grid.add(buttonBox, 1, 4);
            
            setScene(new Scene(grid));
        }
        
        private void testApiConnection() {
            try {
                QichachaApi api = new QichachaApi(
                    config.getApiKey(), 
                    config.getApiSecret()
                );
                api.getCompanyInfo("测试公司");
                showAlert("成功", "API连接测试成功", Alert.AlertType.INFORMATION);
            } catch (Exception e) {
                showAlert("错误", "API连接测试失败：" + e.getMessage(), 
                    Alert.AlertType.ERROR);
            }
        }
        
        private void showAlert(String title, String content, Alert.AlertType type) {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        }
    }
}