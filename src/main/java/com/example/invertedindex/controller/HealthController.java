package com.example.invertedindex.controller;

import com.example.invertedindex.model.response.StatsResponse;
import com.example.invertedindex.service.HealthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class HealthController {
    private final HealthService healthService;

    @GetMapping("/health")
    public ResponseEntity<?> health(){
        return ResponseEntity.ok(healthService.getHealth());
    }

    @GetMapping("/stats")
    public ResponseEntity<StatsResponse> getStats(){
        return ResponseEntity.ok(healthService.getStats());
    }
}
