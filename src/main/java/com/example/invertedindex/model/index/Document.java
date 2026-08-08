package com.example.invertedindex.model.index;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.nio.file.Path;
import java.util.Map;

@Getter
@AllArgsConstructor
@Setter
public class Document{
    Map<String, Object> source;
    Path path;
}
