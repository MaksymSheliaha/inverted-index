package com.example.invertedindex.model.index;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.nio.file.Path;

@Getter
@AllArgsConstructor
public class Document {
    long id;
    long location;
    Path path;
}
