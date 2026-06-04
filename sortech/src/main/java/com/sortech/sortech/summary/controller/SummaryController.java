package com.sortech.sortech.summary.controller;

import com.sortech.sortech.summary.dto.SummaryDto;
import com.sortech.sortech.summary.service.SummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/summary")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class SummaryController {

    private final SummaryService summaryService;
    
    @PostMapping("/text")
    public String summarizeText(@RequestBody SummaryDto request) {
        // DTO에서 content 문자열을 꺼내 서비스의 String 버전 summarize 호출
        return summaryService.summarize(request.content());
    }

    @PostMapping("/file")
    public String summarizeFile(@RequestParam("file") MultipartFile file) {
        // 업로드된 파일 객체 그대로 서비스의 MultipartFile 버전 summarize 호출
        return summaryService.summarize(file);
    }
}