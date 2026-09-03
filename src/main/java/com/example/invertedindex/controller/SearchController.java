package com.example.invertedindex.controller;

import com.example.invertedindex.model.request.IndexRequest;
import com.example.invertedindex.model.request.SearchRequest;
import com.example.invertedindex.service.IndexService;
import com.example.invertedindex.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SearchController {

    private final IndexService indexService;
    private final SearchService searchService;

    @PostMapping("/index")
    public ResponseEntity indexData(@RequestBody IndexRequest indexRequest){
        return ResponseEntity.ok(indexService.indexDataset(indexRequest.getThreadNum()));
    }

    @PostMapping("/search")
    public ResponseEntity search(@RequestBody SearchRequest searchRequest){
        return ResponseEntity.ok(searchService.findDocs(searchRequest));
    }
}
