package com.example.invertedindex.controller;

import com.example.invertedindex.model.request.IndexRequest;
import com.example.invertedindex.model.request.SearchRequest;
import com.example.invertedindex.model.response.IndexResponse;
import com.example.invertedindex.service.IndexService;
import com.example.invertedindex.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SearchController {

    private final IndexService indexService;
    private final SearchService searchService;

    @PostMapping("/index")
    public ResponseEntity<IndexResponse> indexData(@RequestBody IndexRequest indexRequest){
        return ResponseEntity.ok(indexService.indexDataset(indexRequest.getThreadNum()));
    }

    @PostMapping("/search")
    public ResponseEntity search(@RequestBody SearchRequest searchRequest){
        return ResponseEntity.ok(searchService.findDocs(searchRequest));
    }

    @GetMapping("/documents")
    public ResponseEntity getByIds(@RequestParam(name = "ids") List<Long> ids){
        return ResponseEntity.ok(searchService.getById(ids));
    }
}
