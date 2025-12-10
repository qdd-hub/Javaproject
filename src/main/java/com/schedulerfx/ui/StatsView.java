package com.schedulerfx.ui;

import com.schedulerfx.dao.EventDao;
import com.schedulerfx.dao.MySqlEventDao;
import com.schedulerfx.service.GeminiService;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox; // ★ 추가
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.util.Map;

public class StatsView extends VBox {

    private PieChart pieChart;
    private TextArea adviceBox;
    private ObservableList<PieChart.Data> chartData;

    public StatsView() {
        // 1. 화면 기본 설정
        this.setSpacing(15);
        this.setAlignment(Pos.CENTER);
        this.setPadding(new javafx.geometry.Insets(20));
        this.setStyle("-fx-background-color: #ffffff;");

        // 2. 제목
        Label titleLabel = new Label("📅 카테고리별 일정 통계");
        titleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #333;");

        // 기간 선택 기능
        Label periodLabel = new Label("기간 선택: ");
        ComboBox<String> periodComboBox = new ComboBox<>();
        periodComboBox.getItems().addAll("최근 1주일", "최근 1개월", "전체 기간");
        periodComboBox.setValue("최근 1개월"); // 기본값

        HBox controlBox = new HBox(10, periodLabel, periodComboBox);
        controlBox.setAlignment(Pos.CENTER);
        
        // ---------------------------------------------------------

        // 3. 차트 초기화
        chartData = FXCollections.observableArrayList();
        pieChart = new PieChart(chartData);
        pieChart.setLabelsVisible(true);
        pieChart.setLegendVisible(true);
        pieChart.setPrefSize(400, 250);

        // 4. AI 조언 화면
        Label adviceTitle = new Label("💡 AI의 조언:");
        adviceTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2980b9;");
        
        adviceBox = new TextArea("기간을 선택하면 분석을 시작합니다...");
        adviceBox.setWrapText(true);
        adviceBox.setEditable(false);
        adviceBox.setPrefRowCount(3);
        adviceBox.setMaxWidth(450);
        adviceBox.setStyle("-fx-control-inner-background: #f8f9fa; -fx-font-family: 'Malgun Gothic'; -fx-font-size: 13px;");

        // 5. 닫기 버튼
        Button closeBtn = new Button("닫기");
        closeBtn.setPrefWidth(100);
        closeBtn.setOnAction(e -> ((Stage) getScene().getWindow()).close());

        periodComboBox.setOnAction(e -> {
            String selected = periodComboBox.getValue();
            updateStats(selected); 
        });

        // 화면 추가
        this.getChildren().addAll(titleLabel, controlBox, pieChart, adviceTitle, adviceBox, closeBtn);

        // 기본값 1개월 로딩
        updateStats("최근 1개월");
    }

    // 화면 갱신 메서드
    private void updateStats(String period) {
        chartData.clear(); // 기존 차트 지우기
        adviceBox.setText("AI가 '" + period + "' 데이터를 분석 중입니다...");

        // 날짜 계산
        LocalDateTime startDate = null;
        LocalDateTime now = LocalDateTime.now();

        if (period.equals("최근 1주일")) {
            startDate = now.minusWeeks(1);
        } else if (period.equals("최근 1개월")) {
            startDate = now.minusMonths(1);
        } else {
            // 전체 기간
            startDate = now.minusYears(100); 
        }

        // DB에서 데이터 가져오기
        Map<String, Integer> stats = null;
        try {
            EventDao dao = new MySqlEventDao();
            // 기간별 조회
            stats = dao.getCategoryStatsByDate(startDate);

            if (stats.isEmpty()) {
                chartData.add(new PieChart.Data("데이터 없음", 1));
                adviceBox.setText("이 기간에는 완료된 일정이 없습니다.");
                return; // 데이터 없으면 AI 호출 안함
            }

            // 차트에 데이터 채우기
            for (Map.Entry<String, Integer> entry : stats.entrySet()) {
                chartData.add(new PieChart.Data(entry.getKey() + " (" + entry.getValue() + ")", entry.getValue()));
            }

            // AI 분석 시작
            loadAiAdvice(stats, adviceBox, period);

        } catch (Exception e) {
            e.printStackTrace();
            adviceBox.setText("DB 오류가 발생했습니다.");
        }
    }

    // AI 서비스 호출 (기존 코드와 거의 동일, period 인자만 추가됨)
    private void loadAiAdvice(Map<String, Integer> stats, TextArea adviceBox, String period) {
        StringBuilder dataString = new StringBuilder();
        // 프롬프트에 기간 정보도 같이 넣어줌
        dataString.append("기간: ").append(period).append(", 통계: ");
        stats.forEach((k, v) -> dataString.append(k).append(":").append(v).append(", "));

        Task<String> aiTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                GeminiService service = new GeminiService();
                return service.getAdvice(dataString.toString());
            }
        };

        aiTask.setOnSucceeded(e -> adviceBox.setText(aiTask.getValue()));
        aiTask.setOnFailed(e -> adviceBox.setText("AI 분석 실패"));

        new Thread(aiTask).start();
    }
}