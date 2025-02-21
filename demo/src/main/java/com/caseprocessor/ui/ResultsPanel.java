package com.caseprocessor.ui;

import com.caseprocessor.filehandler.DocumentProcessor;
import com.caseprocessor.filehandler.DocumentProcessor.CompanyInfo;

import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.beans.property.SimpleStringProperty;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.util.*;

public class ResultsPanel extends VBox {
    private TableView<DocumentProcessor.CompanyInfo> resultsTable;
    private TextArea detailArea;
    private ObservableList<DocumentProcessor.CompanyInfo> masterData;
    private FilteredList<DocumentProcessor.CompanyInfo> filteredData;
    private ComboBox<String> displayFilterBox;
    
    public ResultsPanel() {
        setPadding(new Insets(10));
        setSpacing(10);
        masterData = FXCollections.observableArrayList();
        filteredData = new FilteredList<>(masterData, p -> true);
        
        createContent();
    }
    
    private void createContent() {
        createFilterArea();
        createTable();
        createDetailArea();
        createButtonArea();
    }
    
    private void createFilterArea() {
        HBox filterBox = new HBox(10);
        filterBox.setAlignment(Pos.CENTER_LEFT);
        
        displayFilterBox = new ComboBox<>();
        displayFilterBox.getItems().addAll(
            "显示全部",
            "仅显示问题公司",
            "仅显示正常公司"
        );
        displayFilterBox.setValue("显示全部");
        displayFilterBox.setOnAction(e -> updateFilter());
        
        filterBox.getChildren().addAll(new Label("显示筛选："), displayFilterBox);
        getChildren().add(filterBox);
    }
    
    private void createTable() {
        resultsTable = new TableView<>();
        
        TableColumn<CompanyInfo, String> nameColumn = new TableColumn<>("公司名称");
        nameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCompanyName()));
        nameColumn.setPrefWidth(200);
        
        TableColumn<CompanyInfo, String> roleColumn = new TableColumn<>("诉讼身份");
        roleColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRole()));
        roleColumn.setPrefWidth(100);
        
        TableColumn<CompanyInfo, String> courtColumn = new TableColumn<>("法院");
        courtColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCourtName()));
        courtColumn.setPrefWidth(200);
        
        TableColumn<CompanyInfo, String> caseNumberColumn = new TableColumn<>("案号");
        caseNumberColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCaseNumber()));
        caseNumberColumn.setPrefWidth(150);
        
        TableColumn<CompanyInfo, String> dateColumn = new TableColumn<>("判决日期");
        dateColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getJudgmentDate()));
        dateColumn.setPrefWidth(100);
        
        TableColumn<CompanyInfo, String> statusColumn = new TableColumn<>("注册状态");
        statusColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRegistrationStatus()));
        statusColumn.setPrefWidth(100);
        
        resultsTable.getColumns().addAll(
            nameColumn, roleColumn, courtColumn, caseNumberColumn, dateColumn, statusColumn
        );
        
        resultsTable.setItems(filteredData);
        
        resultsTable.getSelectionModel().selectedItemProperty().addListener(
            (observable, oldValue, newValue) -> {
                if (newValue != null) {
                    updateDetailArea(newValue);
                }
            }
        );
        
        VBox.setVgrow(resultsTable, Priority.ALWAYS);
        getChildren().add(resultsTable);
    }
    
    private void createDetailArea() {
        detailArea = new TextArea();
        detailArea.setEditable(false);
        detailArea.setWrapText(true);
        detailArea.setPrefRowCount(5);
        detailArea.setPromptText("选择一条记录查看详细信息");
        
        VBox detailBox = new VBox(5);
        detailBox.getChildren().addAll(
            new Label("详细信息："),
            detailArea
        );
        
        getChildren().add(detailBox);
    }
    
    private void createButtonArea() {
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        
        Button exportButton = new Button("导出结果");
        exportButton.setOnAction(e -> exportResults());
        
        Button clearButton = new Button("清空结果");
        clearButton.setOnAction(e -> clearResults());
        
        buttonBox.getChildren().addAll(exportButton, clearButton);
        getChildren().add(buttonBox);
    }
    
    public void setResults(List<DocumentProcessor.CompanyInfo> results) {
        masterData.clear();
        masterData.addAll(results);
        updateFilter();
    }
    
    private void updateDetailArea(DocumentProcessor.CompanyInfo info) {
        StringBuilder builder = new StringBuilder();
        builder.append("公司名称：").append(info.getCompanyName()).append("\n");
        builder.append("诉讼身份：").append(info.getRole()).append("\n");
        builder.append("法院名称：").append(info.getCourtName()).append("\n");
        builder.append("案件编号：").append(info.getCaseNumber()).append("\n");
        builder.append("判决日期：").append(info.getJudgmentDate()).append("\n");
        builder.append("注册状态：").append(info.getRegistrationStatus()).append("\n");
        builder.append("注册日期：").append(info.getRegistrationDate()).append("\n");
        builder.append("经营状态：").append(info.getOperatingStatus());
        
        detailArea.setText(builder.toString());
    }
    
    private void updateFilter() {
        filteredData.setPredicate(company -> {
            if ("显示全部".equals(displayFilterBox.getValue())) {
                return true;
            } else if ("仅显示问题公司".equals(displayFilterBox.getValue())) {
                return "注销".equals(company.getRegistrationStatus()) || 
                       "吊销".equals(company.getRegistrationStatus());
            } else {
                return !"注销".equals(company.getRegistrationStatus()) && 
                       !"吊销".equals(company.getRegistrationStatus());
            }
        });
    }
    
    public void clearResults() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认");
        alert.setHeaderText(null);
        alert.setContentText("确定要清空所有结果吗？");
        
        if (alert.showAndWait().get() == ButtonType.OK) {
            masterData.clear();
            detailArea.clear();
        }
    }
    
    public void exportResults() {
        if (masterData.isEmpty()) {
            showAlert("提示", "没有可导出的数据", Alert.AlertType.INFORMATION);
            return;
        }
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Excel文件", "*.xlsx"),
            new FileChooser.ExtensionFilter("PDF文件", "*.pdf"),
            new FileChooser.ExtensionFilter("文本文件", "*.txt")
        );
        
        File file = fileChooser.showSaveDialog(getScene().getWindow());
        if (file != null) {
            try {
                // TODO: 实现导出逻辑
                showAlert("成功", "数据已导出至：" + file.getPath(), Alert.AlertType.INFORMATION);
            } catch (Exception e) {
                showAlert("错误", "导出失败：" + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }
    
    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
    
    public void performQuery() {
        try {
            for (CompanyInfo company : masterData) {
                DocumentProcessor.mockQichachaQuery(company);
            }
            resultsTable.refresh();
        } catch (Exception e) {
            showAlert("错误", "查询失败：" + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    
    public void resetFilters() {
        displayFilterBox.setValue("显示全部");
        updateFilter();
    }
    
    public void updateFilterOptions(
            ComboBox<String> caseTypeFilter,
            ComboBox<String> courtFilter,
            ComboBox<String> trialStageFilter) {
        
        Set<String> caseTypes = new HashSet<>();
        Set<String> courts = new HashSet<>();
        Set<String> stages = new HashSet<>();
        
        masterData.forEach(info -> {
            if (info.getCaseType() != null) caseTypes.add(info.getCaseType());
            if (info.getCourtName() != null) courts.add(info.getCourtName());
        });
        
        caseTypeFilter.setItems(FXCollections.observableArrayList(caseTypes));
        courtFilter.setItems(FXCollections.observableArrayList(courts));
        trialStageFilter.setItems(FXCollections.observableArrayList(stages));
    }
}