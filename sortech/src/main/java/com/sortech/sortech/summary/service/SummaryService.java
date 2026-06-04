package com.sortech.sortech.summary.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile; // 💡 파일 처리를 위해 추가

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SummaryService {
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    // 재미나이 api key 가져오기
    private final String geminiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=";

    public SummaryService(@Value("${gemini.api.key}") String apiKey, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        // RestClient 초기화 시 주소에 발급받은 API Key를 바인딩합니다.
        this.restClient = RestClient.builder()
                .baseUrl(geminiUrl + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

//    파일 업로드 summarize
    public String summarize(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return "업로드된 파일이 비어있거나 존재하지 않습니다.";
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            // 파일 안의 텍스트 줄바꿈을 포함해 하나의 긴 문자열로 결합
            String fileContent = reader.lines().collect(Collectors.joining("\n"));

            // 추출한 텍스트 문자열을 기존의 텍스트 요약 메서드로 전달
            return summarize(fileContent);

        } catch (Exception e) {
            log.error("파일 내용을 읽는 도중 오류가 발생했습니다.", e);
            return "파일 읽기 실패: " + e.getMessage();
        }
    }

// 텍스트 입력 summarize
    public String summarize(String content) {
        if (content == null || content.trim().isEmpty()) {
            return "요약할 본문 내용이 없습니다.";
        }

        // AI에게 내릴 페르소나와 제약조건 프롬프트 설정 (요약 품질 향상을 위해 프롬프트 수정)
        String promptText = """
                요약할 내용을 입력하세요.
                [본문]
                """ + content;

        // Gemini API가 요구하는 중첩 JSON 바디 규격 조립
        var requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", promptText)
                        ))
                )
        );

        try {
            // 구글 AI 서버로 전송
            String responseBody = restClient.post()
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            // 트리 파싱을 통해 요약 텍스트 결과만 추출
            JsonNode root = objectMapper.readTree(responseBody);
            return root.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText();

        } catch (Exception e) {
            log.error("Gemini API 통신 도중 에러가 발생했습니다.", e);
            return "AI 요약 처리 중 내부 오류가 발생했습니다: " + e.getMessage();
        }
    }
}