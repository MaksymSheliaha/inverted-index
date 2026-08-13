package com.example.invertedindex.index;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.springframework.util.StopWatch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@UtilityClass
public class AnalyzeUtils {
    private static final Analyzer ENGLISH_ANALYZER = new EnglishAnalyzer();
    private static final int TOKEN_LENGTH_THRESHOLD = 2;

    public List<String> analyze(@NonNull String text) {
        // Implementation for text analysis
        return analyze(text, false);
    }

    public List<String> analyze(String text, boolean debug) {
        List<String> result = new ArrayList<>();
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("analyze");
        try (TokenStream tokenStream = ENGLISH_ANALYZER.tokenStream("content", text)) {
            CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
            tokenStream.reset();
            while (tokenStream.incrementToken()) {
                String term = charTermAttribute.toString();
                if(term.length() > TOKEN_LENGTH_THRESHOLD) {
                    result.add(charTermAttribute.toString());
                }
            }

            tokenStream.end();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        stopWatch.stop();
        if(debug) {
            log.info("Analyzed text \"{}\" in {} ms", text, stopWatch.getTotalTimeMillis());
        }
        return result;
    }
}
