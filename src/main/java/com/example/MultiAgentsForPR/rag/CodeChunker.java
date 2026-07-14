package com.example.MultiAgentsForPR.rag;

import java.util.ArrayList;
import java.util.List;

public class CodeChunker {

    private static final int CHUNK_LINES = 40;
    private static final int OVERLAP_LINES = 10;

    public static List<String> chunk(String fileContent) {
        String[] lines = fileContent.split("\n");
        List<String> chunks = new ArrayList<>();

        int i = 0;
        while (i < lines.length) {
            int end = Math.min(i + CHUNK_LINES, lines.length);
            StringBuilder chunk = new StringBuilder();
            for (int j = i; j < end; j++) {
                chunk.append(lines[j]).append("\n");
            }
            chunks.add(chunk.toString());
            if (end == lines.length) break;
            i += (CHUNK_LINES - OVERLAP_LINES);
        }
        return chunks;
    }
}