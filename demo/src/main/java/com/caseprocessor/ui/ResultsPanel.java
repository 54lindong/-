package com.caseprocessor.ui;

import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;

import java.util.List;

public class ResultsPanel extends VBox {

    private ListView<String> companyListView;

    public ResultsPanel() {
        companyListView = new ListView<>();
        this.getChildren().addAll(companyListView);
    }

    public ListView<String> getCompanyListView() {
        return companyListView;
    }

    public void setCompanyListViewData(List<String> companies) {
        companyListView.getItems().setAll(companies);
    }
}
