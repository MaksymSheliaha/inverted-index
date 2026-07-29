package com.example.invertedindex.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class SearchController {
    @GetMapping("/health")
    public ResponseEntity health(){
        return ResponseEntity.ok().build();
    }

    @PostMapping("/index")
    public ResponseEntity indexData(){
        return ResponseEntity.ok().build();
    }

    @PostMapping("/search")
    public ResponseEntity search(){
        return ResponseEntity.ok().build();
    }

    @GetMapping("/stats")
    public ResponseEntity getStats(){
        return ResponseEntity.ok().build();
    }
}
