package com.caseprocessor.ui;

import com.caseprocessor.filehandler.DocumentProcessor;
import com.caseprocessor.filehandler.DocumentProcessor.CompanyInfo;
import com.caseprocessor.data.CompanyDataProcessor;
import com.caseprocessor.data.DataStore;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.File;
import java.util.List;
import java.util.ArrayList;

public class MainWindow extends Application {
    private TableView<CompanyInfo> scanResultTable;
    private TableView<CompanyInfo> companyStatusTable;
    private TextArea detailArea;
    private ComboBox<String> caseTypeFilter;
    private ComboBox<String> courtFilter;
    private ComboBox<String> trialStageFilter;
    private ObservableList<CompanyInfo> masterData = FXCollections.observableArrayList();
    private DataStore dataStore;
    private ResultsPanel resultsPanel;

    @Override
    public void start(Stage primaryStage) {
        dataStore = DataStore.getInstance();
        resultsPanel = new ResultsPanel();
        
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        // 顶部标题和筛选区域
        VBox topBox = createTopSection();
        root.setTop(topBox);

        // 中间部分
        root.setCenter(resultsPanel);

        // 底部按钮区域
        HBox bottomBox = createBottomSection(primaryStage);
        root.setBottom(bottomBox);

        Scene scene = new Scene(root, 1200, 800);
        primaryStage.setTitle("案件智能检测程序");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private VBox createTopSection() {
        VBox topBox = new VBox(10);
        topBox.setPadding(new Insets(10));
        topBox.setAlignment(Pos.CENTER);

        Label title = new Label("案件智能检测程序");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        // 筛选区域
        HBox filterBox = createFilterBox();
        
        topBox.getChildren().addAll(title, filterBox);
        return topBox;
    }

    private HBox createFilterBox() {
        HBox filterBox = new HBox(10);
        filterBox.setAlignment(Pos.CENTER);

        caseTypeFilter = new ComboBox<>();
        caseTypeFilter.setPromptText("案件类型");

        courtFilter = new ComboBox<>();
        courtFilter.setPromptText("法院名称");

        trialStageFilter = new ComboBox<>();
        trialStageFilter.setPromptText("审判阶段");

        Button resetButton = new Button("重置筛选");
        resetButton.setOnAction(e -> resetFilters());

        filterBox.getChildren().addAll(
            new Label("筛选条件："), 
            caseTypeFilter, 
            courtFilter, 
            trialStageFilter,
            resetButton
        );

        return filterBox;
    }

    private HBox createBottomSection(Stage primaryStage) {
        HBox bottomBox = new HBox(10);
        bottomBox.setPadding(new Insets(10));
        bottomBox.setAlignment(Pos.CENTER);

        Button fileInputButton = new Button("选择文件");
        fileInputButton.setOnAction(event -> handleFileInput(primaryStage));

        Button queryButton = new Button("查询");
        queryButton.setOnAction(event -> handleQuery());

        Button exportButton = new Button("导出结果");
        exportButton.setOnAction(event -> handleExport(primaryStage));

        Button historyButton = new Button("历史查询");
        historyButton.setOnAction(event -> showHistoryWindow());

        bottomBox.getChildren().addAll(fileInputButton, queryButton, exportButton, historyButton);
        return bottomBox;
    }

    private void handleFileInput(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("支持的文档", "*.pdf", "*.docx", "*.doc"),
            new FileChooser.ExtensionFilter("PDF文件", "*.pdf"),
            new FileChooser.ExtensionFilter("Word文件", "*.docx", "*.doc")
        );

        List<File> files = fileChooser.showOpenMultipleDialog(stage);
        if (files != null) {
            processFiles(files);
        }
    }

    private void processFiles(List<File> files) {
        try {
            List<CompanyInfo> results = DocumentProcessor.processBatch(files);
            resultsPanel.setResults(results);
            updateFilters();
            showAlert("成功", "成功处理 " + files.size() + " 个文件", Alert.AlertType.INFORMATION);
        } catch (Exception e) {
            showAlert("错误", "处理文件失败：" + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void handleQuery() {
        resultsPanel.performQuery();
    }

    private void handleExport(Stage stage) {
        resultsPanel.exportResults();
    }

    private void showHistoryWindow() {
        HistoryWindow historyWindow = new HistoryWindow();
        historyWindow.show();
    }

    private void resetFilters() {
        caseTypeFilter.setValue(null);
        courtFilter.setValue(null);
        trialStageFilter.setValue(null);
        resultsPanel.resetFilters();
    }

    private void updateFilters() {
        // 更新筛选条件
        resultsPanel.updateFilterOptions(
            caseTypeFilter, 
            courtFilter, 
            trialStageFilter
        );
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}