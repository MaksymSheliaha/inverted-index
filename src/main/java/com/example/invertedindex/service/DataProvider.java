package com.example.invertedindex.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

@Slf4j
@Service
public class DataProvider {

    @Value("${input.folder:input}")
    private ClassPathResource inputFolder;

    public Stream<Path> getInputFiles() {
        try {
            if(inputFolder == null || !inputFolder.exists() || !inputFolder.getFile().isDirectory()) {
                log.warn("Input folder does not exist or is not a directory: {}", inputFolder);
                return Stream.of();
            }
             return Files.list(inputFolder.getFilePath())
                     .filter(Files::isRegularFile)
                     .filter(path -> path.toString().endsWith(".json") || path.toString().endsWith(".txt"));
        } catch (Exception e) {
            throw new RuntimeException("Failed to read input files", e);
        }
    }
}
