package com.example.invertedindex.controller;

import com.example.invertedindex.model.SearchRequest;
import com.example.invertedindex.service.IndexService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SearchController {

    private final IndexService indexService;


    @GetMapping("/health")
    public ResponseEntity health(){
        return ResponseEntity.ok().build();
    }

    @PostMapping("/index")
    public ResponseEntity indexData(){
        return ResponseEntity.ok(indexService.indexDataset(1));
    }

    @PostMapping("/search")
    public ResponseEntity search(@RequestBody SearchRequest searchRequest){
        return ResponseEntity.ok(indexService.findDocs(searchRequest.getSearchTerm()));
    }

    @GetMapping("/stats")
    public ResponseEntity getStats(){
        return ResponseEntity.ok().build();
    }
}
