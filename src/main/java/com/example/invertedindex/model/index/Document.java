package com.example.invertedindex.model.index;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.nio.file.Path;

@Getter
@AllArgsConstructor
@Setter
public class Document {
    long location;
    Path path;
}
