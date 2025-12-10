package com.schedulerfx.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class GeminiService {

    // 여기에 발급받은 Google Gemini API 키를 넣으세요
    private static final String API_KEY = "여기다가 api 키 넣으세요"; 
    // 사용할 모델
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + API_KEY;
    //private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + API_KEY;

    
    public String getAdvice(String promptData) {
    	int maxRetries = 3; // 최대 3번까지 재시도
        int retryCount = 0;

        while (retryCount < maxRetries) {
            try {
                // 1. 프롬프트 구성
                String userPrompt = "다음은 내 일정 통계야. " + promptData + 
                                    ". 이 데이터를 바탕으로 일정 관리에 대한 짧고 구체적인 조언을 한 문장으로 해줘. 한국어로 답변해.";

                // 2. JSON Body 만들기
                String requestBody = """
                    {
                      "contents": [{
                        "parts": [{
                          "text": "%s"
                        }]
                      }]
                    }
                    """.formatted(userPrompt.replace("\n", " ").replace("\"", "\\\"")); 

                // 3. HTTP 요청 생성
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_URL))
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(10))
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();

                // 4. 전송 및 응답 받기
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                // 5. 응답 코드 확인
                if (response.statusCode() == 200) {
                    // 성공하면 바로 결과 리턴
                    return extractTextFromGeminiResponse(response.body());
                } 
                else if (response.statusCode() == 503) {
                    // 재시도
                    retryCount++;
                    System.out.println("⚠️ 서버 과부하(503). " + retryCount + "번째 재시도 중...");
                    Thread.sleep(2000); // 2초 쉼 (Backoff)
                } 
                else {
                    // 에러면 즉시 종료
                    return "AI 호출 실패 (코드 " + response.statusCode() + "): " + response.body();
                }

            } catch (Exception e) {
                e.printStackTrace();
                return "오류 발생: " + e.getMessage();
            }
        }
        
        return "서버가 너무 바빠서 응답할 수 없습니다. (재시도 실패)";
    }

    // JSON 파싱 (라이브러리 없이 문자열 처리 - Gemini 응답 구조가 좀 깊습니다)
    private String extractTextFromGeminiResponse(String json) {
        try {
            // "text": "..." 부분을 찾습니다.
            String marker = "\"text\": \"";
            int start = json.indexOf(marker);
            if (start == -1) return "답변을 찾을 수 없습니다.";
            
            start += marker.length();
            int end = json.indexOf("\"", start);
            
            // 이스케이프 문자(\n 등)가 섞여있어서 원본 텍스트 복구가 필요할 수 있으나, 
            // 간단하게 내용만 가져옵니다.
            String result = json.substring(start, end);
            
            // 보기 좋게 줄바꿈 문자(\n) 등을 실제 줄바꿈으로 변환
            return result.replace("\\n", "\n");
        } catch (Exception e) {
            return "응답 해석 오류";
        }
    }
}