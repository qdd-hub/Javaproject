package com.schedulerfx.ui;

import com.schedulerfx.dao.EventDao;
import com.schedulerfx.dao.SqliteEventDao;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.util.Map;

// 파이 차트로 카테고리 시각화 기능
// ex) 달력에 자기가 한 일정의 카테고리의 비율을 보여줘서 일정 관리를 도와줌

public class StatsView extends VBox {

    public StatsView() {
        // 1. 화면 기본 설정
        this.setSpacing(20);
        this.setAlignment(Pos.CENTER);
        this.setPadding(new javafx.geometry.Insets(30));
        this.setStyle("-fx-background-color: #ffffff;");

        // 2. 제목 라벨
        Label titleLabel = new Label("📅 카테고리별 일정 통계");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #333;");

        // 3. DAO를 통해 데이터 가져오기
        //
        //db 이름 정하기
        //
        //
        EventDao dao = new SqliteEventDao("db이름"); 
        Map<String, Integer> stats = dao.getCategoryStats();

        // 4. 데이터를 차트용 형식(PieChart.Data)으로 변환
        ObservableList<PieChart.Data> chartData = FXCollections.observableArrayList();
        
        if (stats.isEmpty()) {
            // 데이터가 하나도 없을 때
            chartData.add(new PieChart.Data("데이터 없음", 1));
        } else {
            // 데이터가 있으면 반복문으로 차트에 추가
            for (Map.Entry<String, Integer> entry : stats.entrySet()) {
                String category = entry.getKey();
                int count = entry.getValue();
                
                // "업무 (5)" 형식으로 라벨 표시
                chartData.add(new PieChart.Data(category + " (" + count + ")", count));
            }
        }

        // 5. 차트 생성 및 설정
        PieChart pieChart = new PieChart(chartData);
        pieChart.setTitle("일정 비율");
        pieChart.setLabelsVisible(true);
        pieChart.setLegendVisible(true);
        pieChart.setPrefSize(500, 400); // 차트 크기 지정

        // 6. 닫기 버튼
        Button closeBtn = new Button("닫기");
        closeBtn.setPrefWidth(100);
        closeBtn.setStyle("-fx-font-size: 14px;");
        closeBtn.setOnAction(e -> {
            // 현재 창(Stage)을 찾아서 닫음
            Stage stage = (Stage) this.getScene().getWindow();
            stage.close();
        });

        // 7. 화면에 모든 요소 추가 (제목, 차트, 버튼)
        this.getChildren().addAll(titleLabel, pieChart, closeBtn);
    }
}
