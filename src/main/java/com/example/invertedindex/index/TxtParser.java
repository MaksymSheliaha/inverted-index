package com.example.invertedindex.index;

import java.util.ArrayList;
import java.util.List;

public class TxtParser {
    /**
     * Parses a combined text document into a list of individual works.
     *
     * @param textContent The full text string containing all documents.
     * @return A list where each element is a separate document (work).
     */
    public static List<String> parseShakespeareDocuments(String textContent) {
        List<String> documents = new ArrayList<>();

        // Define the regex pattern for the recurring disclaimer.
        // (?s) enables DOTALL mode so that '.' matches newline characters.
        // (?i) enables CASE_INSENSITIVE matching.
        String separatorPattern = "(?si)<<THIS ELECTRONIC VERSION OF THE COMPLETE WORKS OF WILLIAM.*?FOR MEMBERSHIP\\.>>";

        // Split the text based on the separator pattern
        String[] rawDocuments = textContent.split(separatorPattern);

        // Clean up the output by stripping excess whitespace and removing empty strings
        for (String doc : rawDocuments) {
            String trimmedDoc = doc.trim();
            if (!trimmedDoc.isEmpty()) {
                documents.add(trimmedDoc);
            }
        }

        return documents;
    }
}
