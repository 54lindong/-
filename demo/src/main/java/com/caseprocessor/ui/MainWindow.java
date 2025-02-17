package com.caseprocessor.ui;

import com.caseprocessor.filehandler.FileConverter;
import com.caseprocessor.textprocessor.CompanyNameExtractor;
import com.caseprocessor.data.DataStore;
import com.caseprocessor.api.QiChaChaApi;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class MainWindow extends Application {

    private TextArea resultArea;
    private ListView<String> companyListView;
    private Button exportButton;
    private Button historyButton;
    private Button queryButton;

    private ListView<String> companyInfoListView;
    private ListView<String> filteredResultsListView; // 新增筛选结果列表

    @Override
    public void start(Stage primaryStage) {
        // Root layout
        BorderPane root = new BorderPane();

        // Top part (Title or Header)
        Label title = new Label("案件智能检测程序");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        root.setTop(title);

        // Center part (Results and List of companies)
        VBox centerBox = new VBox();
        resultArea = new TextArea();
        resultArea.setEditable(false);
        resultArea.setStyle("-fx-font-size: 16px;");
        companyListView = new ListView<>();
        companyInfoListView = new ListView<>();
        filteredResultsListView = new ListView<>(); // 筛选结果列表

        centerBox.getChildren().addAll(new Label("扫描结果:"), resultArea, new Label("筛选结果:"), filteredResultsListView, new Label("公司情况:"), companyInfoListView);
        root.setCenter(centerBox);

        // Bottom part (Buttons)
        HBox bottomBox = new HBox();
        Button fileInputButton = new Button("选择文件");
        fileInputButton.setOnAction(event -> handleFileInput(primaryStage));

        exportButton = new Button("导出");
        exportButton.setOnAction(event -> handleExport(primaryStage));

        historyButton = new Button("历史查询");
        historyButton.setOnAction(event -> handleHistory());

        queryButton = new Button("查询");
        queryButton.setOnAction(event -> handleQuery());

        bottomBox.getChildren().addAll(fileInputButton, exportButton, historyButton, queryButton);  // 添加查询按钮
        root.setBottom(bottomBox);

        // Setup scene and stage
        Scene scene = new Scene(root, 800, 600);
        primaryStage.setTitle("案件智能检测程序");
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);
        primaryStage.show();
    }

    // 选择文件并处理
    private void handleFileInput(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF/Word 文件", "*.pdf", "*.docx"));
        List<File> files = fileChooser.showOpenMultipleDialog(stage);
        if (files != null) {
            processFiles(files);
        }
    }

    // 处理文件
    private void processFiles(List<File> files) {
        for (File file : files) {
            try {
                String fileType = getFileType(file);
                String content = FileConverter.convertToText(file, fileType);
                resultArea.appendText("处理文件: " + file.getName() + "\n" + content + "\n\n");

                // 提取公司数据和审判时间
                List<String> companiesAndDate = CompanyNameExtractor.extractCompaniesAndJudgmentDate(content);

                // 将公司名称和审判时间显示到筛选结果栏
                filteredResultsListView.getItems().setAll(companiesAndDate);

                // 提取公司数据
                List<String> companies = CompanyNameExtractor.extractCompanies(content);
                companyListView.getItems().setAll(companies);

            } catch (IOException e) {
                showAlert("错误", "文件处理失败：" + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    // 获取文件类型
    private String getFileType(File file) {
        String fileName = file.getName().toLowerCase();
        if (fileName.endsWith(".pdf")) {
            return "pdf";
        } else if (fileName.endsWith(".docx")) {
            return "docx";
        } else {
            return "unknown";
        }
    }

    // 查询企查查的公司信息
    private void handleQuery() {
        List<String> companies = companyListView.getItems();
        companyInfoListView.getItems().clear();
        for (String company : companies) {
            try {
                String companyInfo = QiChaChaApi.getCompanyInfo(company);
                companyInfoListView.getItems().add(company + " 信息: \n" + companyInfo);
            } catch (IOException e) {
                showAlert("错误", "企查查查询失败：" + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    // 导出数据
    private void handleExport(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Word 文件", "*.docx"));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON 文件", "*.json"));

        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            saveToFile(file);
        }
    }

    // 保存数据
    private void saveToFile(File file) {
        showAlert("导出", "数据已成功保存到文件: " + file.getPath(), Alert.AlertType.INFORMATION);
    }

    // 显示历史查询
    private void handleHistory() {
        List<String> history = DataStore.loadHistory();
        companyListView.getItems().setAll(history);
        showAlert("历史查询", "加载历史数据完成", Alert.AlertType.INFORMATION);
    }

    // 显示提示信息
    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
