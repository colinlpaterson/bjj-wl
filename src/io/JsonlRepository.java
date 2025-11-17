package io;

import model.Workout;

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class JsonlRepository implements WorkoutRepository {
    private final Path dataFile;
    private final Path dataDir;

    public JsonlRepository() {
        this.dataDir = Path.of("data");
        this.dataFile = dataDir.resolve("workouts.jsonl");
        ensureDataDir();
    }

    private void ensureDataDir() {
        try {
            Files.createDirectories(dataDir);
        } catch (Exception e) {
            System.err.println("Warning: couldn't create data/ directory: " + e.getMessage());
        }
    }

    @Override
    public void append(Workout w) throws Exception {
        String jsonLine = w.toJson() + System.lineSeparator();
        try (BufferedWriter bw = Files.newBufferedWriter(
                dataFile,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND)) {
            bw.write(jsonLine);
        }
    }

    @Override
    public List<String> readAllJsonLines() throws Exception {
        if (!Files.exists(dataFile)) return new ArrayList<>();
        return Files.readAllLines(dataFile, StandardCharsets.UTF_8);
    }

    @Override
    public void replaceLine(int index, Workout replacement) throws Exception {
        List<String> lines = readAllJsonLines();
        if (index < 0 || index >= lines.size()) {
            throw new IllegalArgumentException("Index out of range: " + index);
        }

        Path bak = dataDir.resolve("workouts.jsonl.bak");
        try {
            Files.copy(dataFile, bak, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            System.err.println("Warning: couldn't create backup: " + e.getMessage());
        }

        lines.set(index, replacement.toJson());
        Path tmp = dataDir.resolve("workouts.jsonl.tmp");
        try (BufferedWriter bw = Files.newBufferedWriter(
                tmp, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING)) {
            for (String line : lines) {
                bw.write(line);
                bw.write(System.lineSeparator());
            }
        }
        Files.move(tmp, dataFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    @Override
    public void deleteLine(int index) throws Exception {
        List<String> lines = readAllJsonLines();
        if (index < 0 || index >= lines.size()) {
            throw new IllegalArgumentException("Index out of range: " + index);
        }

        Path bak = dataDir.resolve("workouts.jsonl.bak");
        try {
            Files.copy(dataFile, bak, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            System.err.println("Warning: couldn't create backup: " + e.getMessage());
        }

        Path tmp = dataDir.resolve("workouts.jsonl.tmp");
        try (BufferedWriter bw = Files.newBufferedWriter(
                tmp, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING)) {
            for (int i = 0; i < lines.size(); i++) {
                if (i == index) continue; // skip the deleted line
                bw.write(lines.get(i));
                bw.write(System.lineSeparator());
            }
        }
        Files.move(tmp, dataFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    // ---------- helpers for callers ----------

    public static String extract(String text, String startMarker, String endMarker) {
        int s = text.indexOf(startMarker);
        if (s < 0) return null;
        s += startMarker.length();
        int e = text.indexOf(endMarker, s);
        if (e < 0) return null;
        return text.substring(s, e);
    }

    public static int safeInt(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    // split {...},{...} inside an array block
    public static List<String> splitRoundObjects(String roundsBlock) {
        List<String> parts = new ArrayList<>();
        String trimmed = roundsBlock == null ? "" : roundsBlock.trim();
        if (trimmed.isEmpty()) return parts;
        String[] raw = trimmed.split("\\},\\{");
        for (String r : raw) {
            if (!r.startsWith("{")) r = "{" + r;
            if (!r.endsWith("}")) r = r + "}";
            parts.add(r);
        }
        return parts;
    }
}

