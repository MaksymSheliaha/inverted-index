package com.example.invertedindex.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

@Slf4j
@Service
public class DataProvider {

    @Value("${source.input-path:src/main/resources/batched_dataset}")
    private String inputPath;

    public Stream<Path> getInputFiles() {
        Path inputFolder = Path.of(inputPath);
        try {
            if(!Files.exists(inputFolder)) {
                log.warn("Input folder does not exist or is not a directory: {}", inputFolder);
                return Stream.of();
            }
            if(!Files.isDirectory(inputFolder)) {
                log.warn("Input folder is not a directory: {}", inputFolder);
                return Stream.of(inputFolder)
                        .filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".jsonl"));
            }
             return Files.list(inputFolder)
                     .filter(Files::isRegularFile)
                     .filter(path -> path.toString().endsWith(".jsonl"));
        } catch (Exception e) {
            throw new RuntimeException("Failed to read input files", e);
        }
    }
}
