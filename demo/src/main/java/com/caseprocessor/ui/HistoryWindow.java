package com.caseprocessor.ui;

import com.caseprocessor.data.DataStore;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.collections.FXCollections;
import javafx.beans.property.SimpleStringProperty;
import java.awt.Desktop;
import java.io.File;
import java.util.List;
import java.util.Map;

public class HistoryWindow extends Stage {
    private TableView<Map<String, Object>> historyTable;
    private TextArea detailArea;
    private DataStore dataStore;
    
    public HistoryWindow() {
        this.dataStore = DataStore.getInstance();
        
        setTitle("历史查询");
        setWidth(1000);
        setHeight(600);
        
        VBox root = new VBox(20);
        root.setPadding(new Insets(20));
        
        // 创建标题
        Label title = new Label("历史查询记录");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        
        // 创建表格
        historyTable = createHistoryTable();
        
        // 创建详情区域
        detailArea = new TextArea();
        detailArea.setEditable(false);
        detailArea.setPrefRowCount(5);
        detailArea.setWrapText(true);
        
        // 创建按钮区域
        HBox buttonBox = createButtonBox();
        
        root.getChildren().addAll(title, historyTable, 
            new Label("详细信息："), detailArea, buttonBox);
        
        setScene(new Scene(root));
        
        // 加载历史记录
        loadHistoryRecords();
    }
    
    private TableView<Map<String, Object>> createHistoryTable() {
        TableView<Map<String, Object>> table = new TableView<>();
        
        TableColumn<Map<String, Object>, String> timeCol = 
            new TableColumn<>("查询时间");
        timeCol.setCellValueFactory(cellData -> 
            new SimpleStringProperty(
                cellData.getValue().get("createTime").toString()));
        timeCol.setPrefWidth(150);
        
        TableColumn<Map<String, Object>, String> caseNumberCol = 
            new TableColumn<>("案号");
        caseNumberCol.setCellValueFactory(cellData -> 
            new SimpleStringProperty(
                cellData.getValue().get("caseNumber").toString()));
        caseNumberCol.setPrefWidth(150);
        
        TableColumn<Map<String, Object>, String> courtCol = 
            new TableColumn<>("法院");
        courtCol.setCellValueFactory(cellData -> 
            new SimpleStringProperty(
                cellData.getValue().get("court").toString()));
        courtCol.setPrefWidth(200);
        
        TableColumn<Map<String, Object>, String> companyCountCol = 
            new TableColumn<>("涉及公司数");
        companyCountCol.setCellValueFactory(cellData -> 
            new SimpleStringProperty(
                cellData.getValue().get("companyCount").toString()));
        companyCountCol.setPrefWidth(100);
        
        table.getColumns().addAll(timeCol, caseNumberCol, courtCol, companyCountCol);
        
        // 选择监听器
        table.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    updateDetailArea(newSelection);
                }
            });
        
        return table;
    }
    
    private HBox createButtonBox() {
        HBox buttonBox = new HBox(20);
        buttonBox.setAlignment(Pos.CENTER);
        
        Button openDocButton = new Button("打开原始文档");
        openDocButton.setOnAction(e -> openSelectedDocument());
        
        Button deleteButton = new Button("删除记录");
        deleteButton.setOnAction(e -> deleteSelectedRecord());
        
        Button clearButton = new Button("清空历史记录");
        clearButton.setOnAction(e -> clearHistory());
        
        buttonBox.getChildren().addAll(openDocButton, deleteButton, clearButton);
        return buttonBox;
    }
    
    private void loadHistoryRecords() {
        List<Map<String, Object>> records = dataStore.getCaseHistory();
        historyTable.setItems(FXCollections.observableArrayList(records));
    }
    
    private void updateDetailArea(Map<String, Object> record) {
        StringBuilder detail = new StringBuilder();
        detail.append("案号：").append(record.get("caseNumber")).append("\n");
        detail.append("法院：").append(record.get("court")).append("\n");
        detail.append("审判阶段：").append(record.get("trialStage")).append("\n");
        detail.append("案件类型：").append(record.get("caseType")).append("\n");
        detail.append("创建时间：").append(record.get("createTime")).append("\n");
        detail.append("涉及公司数：").append(record.get("companyCount")).append("\n");
        detail.append("文档数：").append(record.get("documentCount"));
        
        detailArea.setText(detail.toString());
    }
    
    private void openSelectedDocument() {
        Map<String, Object> selected = historyTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Long caseId = (Long) selected.get("id");
            List<String> documentPaths = dataStore.getCaseDocuments(caseId);
            
            if (!documentPaths.isEmpty()) {
                try {
                    File file = new File(documentPaths.get(0));
                    if (file.exists()) {
                        Desktop.getDesktop().open(file);
                    } else {
                        showAlert("错误", "找不到原始文档", Alert.AlertType.ERROR);
                    }
                } catch (Exception e) {
                    showAlert("错误", "无法打开文档: " + e.getMessage(), 
                        Alert.AlertType.ERROR);
                }
            } else {
                showAlert("提示", "没有关联的文档", Alert.AlertType.INFORMATION);
            }
        } else {
            showAlert("提示", "请先选择一条记录", Alert.AlertType.INFORMATION);
        }
    }
    
    private void deleteSelectedRecord() {
        Map<String, Object> selected = historyTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
            confirmation.setTitle("确认");
            confirmation.setHeaderText(null);
            confirmation.setContentText("确定要删除这条记录吗？");
            
            if (confirmation.showAndWait().get() == ButtonType.OK) {
                Long caseId = (Long) selected.get("id");
                dataStore.deleteCase(caseId);
                loadHistoryRecords();
                detailArea.clear();
            }
        } else {
            showAlert("提示", "请先选择一条记录", Alert.AlertType.INFORMATION);
        }
    }
    
    private void clearHistory() {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("确认");
        confirmation.setHeaderText(null);
        confirmation.setContentText("确定要清空所有历史记录吗？此操作不可恢复。");
        
        if (confirmation.showAndWait().get() == ButtonType.OK) {
            dataStore.clearAllData();
            historyTable.getItems().clear();
            detailArea.clear();
            showAlert("成功", "历史记录已清空", Alert.AlertType.INFORMATION);
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