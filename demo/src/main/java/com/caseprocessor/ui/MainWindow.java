package com.caseprocessor.ui;

import com.caseprocessor.textprocessor.CompanyNameExtractor;
import com.caseprocessor.textprocessor.CompanyNameExtractor.Company;
import com.caseprocessor.textprocessor.CompanyNameExtractor.ExtractionResult;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

public class MainWindow extends Application {

    private TextArea resultArea;
    private TableView<Company> companyTableView;
    private ListView<String> filteredResultsListView;
    private ListView<String> companyInfoListView;
    private Button exportButton;
    private Button queryButton;

    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        // 顶部标题
        Label title = new Label("案件智能检测程序");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        VBox titleBox = new VBox(title);
        titleBox.setAlignment(Pos.CENTER);
        titleBox.setPadding(new Insets(10));
        root.setTop(titleBox);

        // 中间部分
        SplitPane centerSplitPane = new SplitPane();
        
        // 左侧结果区域
        VBox leftPane = new VBox(10);
        leftPane.setPadding(new Insets(5));
        
        Label resultLabel = new Label("扫描结果:");
        resultArea = new TextArea();
        resultArea.setEditable(false);
        resultArea.setWrapText(true);
        resultArea.setPrefRowCount(10);
        
        // 公司信息表格
        Label companyLabel = new Label("公司列表:");
        companyTableView = createCompanyTableView();
        
        leftPane.getChildren().addAll(resultLabel, resultArea, companyLabel, companyTableView);
        
        // 右侧信息区域
        VBox rightPane = new VBox(10);
        rightPane.setPadding(new Insets(5));
        
        Label filteredLabel = new Label("筛选结果:");
        filteredResultsListView = new ListView<>();
        
        Label companyInfoLabel = new Label("公司详细信息:");
        companyInfoListView = new ListView<>();
        
        rightPane.getChildren().addAll(filteredLabel, filteredResultsListView, 
                                     companyInfoLabel, companyInfoListView);
        
        centerSplitPane.getItems().addAll(leftPane, rightPane);
        centerSplitPane.setDividerPositions(0.6);
        
        root.setCenter(centerSplitPane);

        // 底部按钮区域
        HBox bottomBox = new HBox(10);
        bottomBox.setPadding(new Insets(10));
        bottomBox.setAlignment(Pos.CENTER);

        Button fileInputButton = new Button("选择文件");
        fileInputButton.setOnAction(event -> handleFileInput(primaryStage));

        exportButton = new Button("导出结果");
        exportButton.setOnAction(event -> handleExport(primaryStage));

        queryButton = new Button("企业查询");
        queryButton.setOnAction(event -> handleQuery());

        bottomBox.getChildren().addAll(fileInputButton, exportButton, queryButton);
        root.setBottom(bottomBox);

        // 设置场景
        Scene scene = new Scene(root, 1200, 800);
        primaryStage.setTitle("案件智能检测程序");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private TableView<Company> createCompanyTableView() {
        TableView<Company> tableView = new TableView<>();

        TableColumn<Company, String> nameCol = new TableColumn<>("公司名称");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(200);

        TableColumn<Company, String> identityCol = new TableColumn<>("身份");
        identityCol.setCellValueFactory(new PropertyValueFactory<>("identity"));
        identityCol.setPrefWidth(100);

        TableColumn<Company, String> roleCol = new TableColumn<>("角色");
        roleCol.setCellValueFactory(new PropertyValueFactory<>("role"));
        roleCol.setPrefWidth(100);

        tableView.getColumns().addAll(nameCol, identityCol, roleCol);
        tableView.setPrefHeight(200);

        return tableView;
    }

    private void handleFileInput(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Word Files", "*.docx")
        );
        
        List<File> files = fileChooser.showOpenMultipleDialog(stage);
        if (files != null) {
            processFiles(files);
        }
    }

    private void processFiles(List<File> files) {
        try {
            List<ExtractionResult> results = CompanyNameExtractor.batchExtractInformation(files);
            displayResults(results, files);
            updateCompanyTable(results);
            updateFilteredResults(results);
        } catch (IOException e) {
            showAlert("错误", "文件处理失败：" + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void displayResults(List<ExtractionResult> results, List<File> files) {
        StringBuilder display = new StringBuilder();
        for (int i = 0; i < results.size(); i++) {
            ExtractionResult result = results.get(i);
            display.append("文件名：").append(files.get(i).getName()).append("\n");
            
            if (result.getCaseName() != null) {
                display.append("案件名称：").append(result.getCaseName()).append("\n");
            }
            if (result.getCaseNumber() != null) {
                display.append("案号：").append(result.getCaseNumber()).append("\n");
            }
            
            display.append("当事人信息：\n");
            for (Company company : result.getCompanies()) {
                display.append(company.toString()).append("\n");
            }
            
            if (result.getDate() != null) {
                display.append("判决日期：").append(result.getDate()).append("\n");
            }
            
            display.append("\n");
        }
        resultArea.setText(display.toString());
    }

    private void updateCompanyTable(List<ExtractionResult> results) {
        ObservableList<Company> companies = FXCollections.observableArrayList();
        for (ExtractionResult result : results) {
            companies.addAll(result.getCompanies());
        }
        companyTableView.setItems(companies);
    }

    private void updateFilteredResults(List<ExtractionResult> results) {
        ObservableList<String> filteredResults = FXCollections.observableArrayList();
        for (ExtractionResult result : results) {
            if (result.getCaseNumber() != null) {
                filteredResults.add(result.getCaseNumber() + ": " + result.getCaseName());
            }
        }
        filteredResultsListView.setItems(filteredResults);
    }

    private void handleExport(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Word Files", "*.docx"),
            new FileChooser.ExtensionFilter("Text Files", "*.txt")
        );
        
        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            try {
                String content = resultArea.getText();
                // 这里简单地将内容保存为文本文件
                java.nio.file.Files.writeString(file.toPath(), content);
                showAlert("成功", "结果已成功导出至: " + file.getPath(), Alert.AlertType.INFORMATION);
            } catch (IOException e) {
                showAlert("错误", "导出失败：" + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    private void handleQuery() {
        Company selectedCompany = companyTableView.getSelectionModel().getSelectedItem();
        if (selectedCompany == null) {
            showAlert("提示", "请先选择要查询的公司", Alert.AlertType.INFORMATION);
            return;
        }

        // 这里简单显示公司信息
        companyInfoListView.getItems().clear();
        companyInfoListView.getItems().add("公司名称：" + selectedCompany.getName());
        companyInfoListView.getItems().add("身份：" + selectedCompany.getIdentity());
        if (selectedCompany.getRole() != null) {
            companyInfoListView.getItems().add("角色：" + selectedCompany.getRole());
        }
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}