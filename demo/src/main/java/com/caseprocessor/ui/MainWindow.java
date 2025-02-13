package com.caseprocessor.ui;

import com.caseprocessor.data.DataStore;
import com.caseprocessor.filehandler.FileConverter;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
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
        companyListView = new ListView<>();
        centerBox.getChildren().addAll(new Label("处理结果:"), resultArea, new Label("公司列表:"), companyListView);
        root.setCenter(centerBox);

        // Bottom part (Buttons)
        HBox bottomBox = new HBox();
        Button fileInputButton = new Button("选择文件");
        fileInputButton.setOnAction(event -> handleFileInput(primaryStage));

        exportButton = new Button("导出");
        exportButton.setOnAction(event -> handleExport(primaryStage));

        historyButton = new Button("历史查询");
        historyButton.setOnAction(event -> handleHistory());

        bottomBox.getChildren().addAll(fileInputButton, exportButton, historyButton);
        root.setBottom(bottomBox);

        // Setup scene and stage
        Scene scene = new Scene(root, 800, 600);
        primaryStage.setTitle("案件智能检测程序");
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);  // Full screen
        primaryStage.show();
    }

    // File Input handling: choose PDF/Word files
    private void handleFileInput(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF/Word Files", "*.pdf", "*.docx"));
        List<File> files = fileChooser.showOpenMultipleDialog(stage);
        if (files != null) {
            processFiles(files);
        }
    }

    // Process the selected files
    private void processFiles(List<File> files) {
        for (File file : files) {
            try {
                String fileType = getFileType(file);
                String content = FileConverter.convertToText(file, fileType);

                resultArea.appendText("处理文件: " + file.getName() + "\n" + content + "\n\n");
                // Process company data (mocking the process)
                List<String> companies = DataStore.loadCompanies(file);  // Assuming this method loads companies related to the case
                companyListView.getItems().addAll(companies);

            } catch (IOException e) {
                showAlert("错误", "文件处理失败：" + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    // Get file type by extension
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

    // Export selected results to desired format (Word, JSON, etc.)
    private void handleExport(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Word Files", "*.docx"));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));

        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            // Save the content to the selected file (mocked here)
            saveToFile(file);
        }
    }

    // Save data to file (mock implementation)
    private void saveToFile(File file) {
        showAlert("导出", "数据已成功保存到文件: " + file.getPath(), Alert.AlertType.INFORMATION);
    }

    // Show historical queries
    private void handleHistory() {
        List<String> history = DataStore.loadHistory();
        companyListView.getItems().setAll(history);
        showAlert("历史查询", "加载历史数据完成", Alert.AlertType.INFORMATION);
    }

    // Utility to show alerts
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
