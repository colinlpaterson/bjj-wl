import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * BJJ Workout Log (menu version)
 * - Menu: Add workout / View recent / Quit
 * - Persists each workout as one JSON object per line (NDJSON) in data/workouts.jsonl
 * - Keeps design simple & scalable so a DB layer can be swapped in later
 */
public class BJJWorkoutLog {

    // ---------- Data Models ----------
    static class Round {
        String beltLevel;
        String partnerSize;            // free text for now
        Integer partnerAge;
        Integer timesYouWereSubmitted;
        String submissionTypesAgainst; // comma-separated
        Integer timesYouSubmittedPartner;
        String submissionTypesFor;     // comma-separated
        String observations;

        String toJson() {
            return new StringBuilder()
                    .append("{")
                    .append("\"beltLevel\":\"").append(escape(beltLevel)).append("\",")
                    .append("\"partnerSize\":\"").append(escape(partnerSize)).append("\",")
                    .append("\"partnerAge\":").append(partnerAge).append(",")
                    .append("\"timesYouWereSubmitted\":").append(timesYouWereSubmitted).append(",")
                    .append("\"submissionTypesAgainst\":\"").append(escape(submissionTypesAgainst)).append("\",")
                    .append("\"timesYouSubmittedPartner\":").append(timesYouSubmittedPartner).append(",")
                    .append("\"submissionTypesFor\":\"").append(escape(submissionTypesFor)).append("\",")
                    .append("\"observations\":\"").append(escape(observations)).append("\"")
                    .append("}")
                    .toString();
        }
    }

    static class Workout {
        String date;     // ISO yyyy-MM-dd
        String drills;
        Integer roundsCount;
        List<Round> rounds = new ArrayList<>();
        String notes;

        String toJson() {
            StringBuilder sb = new StringBuilder();
            sb.append("{")
                    .append("\"date\":\"").append(escape(date)).append("\",")
                    .append("\"drills\":\"").append(escape(drills)).append("\",")
                    .append("\"roundsCount\":").append(roundsCount).append(",")
                    .append("\"rounds\":[");
            for (int i = 0; i < rounds.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(rounds.get(i).toJson());
            }
            sb.append("],")
                    .append("\"notes\":\"").append(escape(notes)).append("\"")
                    .append("}");
            return sb.toString();
        }
    }

    // ---------- Main ----------
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        ensureDataDir();

        while (true) {
            System.out.println("\n=== BJJ Workout Log ===");
            System.out.println("1) Add workout");
            System.out.println("2) View recent workouts");
            System.out.println("3) Quit");
            int choice = promptIntRange(in, "Choose an option (1-3): ", 1, 3);

            if (choice == 1) {
                addWorkout(in);
            } else if (choice == 2) {
                viewRecent(in);
            } else {
                System.out.println("Good training. See you next time!");
                break;
            }
        }
        in.close();
    }

    // ---------- Actions ----------
    private static void addWorkout(Scanner input) {
        System.out.println("\n-- Add Workout --");

        String date = promptString(input, "Enter date (YYYY-MM-DD) [Enter for today]: ");
        if (date.isBlank()) {
            date = LocalDate.now().toString();
            System.out.println("Using today's date: " + date);
        }

        String drills = promptString(input, "Enter drills practiced: ");
        int roundsCount = promptInt(input, "Enter number of sparring rounds: ");

        List<Round> rounds = new ArrayList<>();
        for (int i = 1; i <= roundsCount; i++) {
            System.out.println("\n-- Round " + i + " details --");
            Round r = new Round();
            r.beltLevel = promptString(input, "1) Belt level of partner (white/blue/purple/brown/black): ");
            r.partnerSize = promptString(input, "2) Size of partner (light/med/heavy or lbs): ");
            r.partnerAge = promptInt(input, "3) Age of partner (integer): ");
            r.timesYouWereSubmitted = promptInt(input, "4) Times you were submitted: ");
            r.submissionTypesAgainst = promptString(input, "5) Submission type(s) they used (comma-separated): ");
            r.timesYouSubmittedPartner = promptInt(input, "6) Times you submitted partner: ");
            r.submissionTypesFor = promptString(input, "7) Submission type(s) you used (comma-separated): ");
            r.observations = promptString(input, "8) Observations of the round: ");
            rounds.add(r);
        }

        String notes = promptString(input, "\nAny overall notes? ");

        Workout w = new Workout();
        w.date = date;
        w.drills = drills;
        w.roundsCount = roundsCount;
        w.rounds = rounds;
        w.notes = notes;

        Path out = Path.of("data", "workouts.jsonl");
        String jsonLine = w.toJson() + System.lineSeparator();
        try (BufferedWriter bw = Files.newBufferedWriter(
                out,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND)) {
            bw.write(jsonLine);
        } catch (IOException e) {
            System.err.println("Failed to write workout: " + e.getMessage());
            return;
        }

        System.out.println("\n--- Workout Saved ---");
        System.out.println("Date: " + w.date);
        System.out.println("Drills: " + w.drills);
        System.out.println("Rounds logged: " + w.roundsCount);
        System.out.println("Notes: " + w.notes);
        System.out.println("Appended to: " + out.toAbsolutePath());
    }

    private static void viewRecent(Scanner input) {
        System.out.println("\n-- View Recent Workouts --");
        Path p = Path.of("data", "workouts.jsonl");
        if (!Files.exists(p)) {
            System.out.println("No workouts found yet. Log one first!");
            return;
        }

        int n = promptIntRange(input, "How many most recent workouts to show? ", 1, 1000);
        List<String> lines;
        try {
            lines = Files.readAllLines(p, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Failed to read workouts: " + e.getMessage());
            return;
        }

        if (lines.isEmpty()) {
            System.out.println("No workouts found.");
            return;
        }

        int start = Math.max(0, lines.size() - n);
        List<String> slice = lines.subList(start, lines.size());

        System.out.println("\n--- Showing " + slice.size() + " workout(s) ---");
        for (int i = 0; i < slice.size(); i++) {
            String json = slice.get(i);
            // Lightweight summary pulled from our known JSON keys
            String date  = extract(json, "\"date\":\"", "\"");
            String drills = extract(json, "\"drills\":\"", "\"");
            String roundsCount = extract(json, "\"roundsCount\":", ",");
            if (roundsCount == null) roundsCount = "?";

            System.out.println("\n#" + (start + i + 1));
            System.out.println("Date:   " + (date != null ? date : "(unknown)"));
            System.out.println("Drills: " + (drills != null ? drills : "(unknown)"));
            System.out.println("Rounds: " + roundsCount);
            System.out.println("Raw JSON:");
            System.out.println(json);
        }
        System.out.println("\n(End of list)");
    }

    // ---------- Helpers ----------
    private static void ensureDataDir() {
        try {
            Files.createDirectories(Path.of("data"));
        } catch (IOException e) {
            System.err.println("Warning: couldn't create data/ directory: " + e.getMessage());
        }
    }

    private static String promptString(Scanner sc, String msg) {
        System.out.print(msg);
        return sc.nextLine().trim();
    }

    private static int promptInt(Scanner sc, String msg) {
        while (true) {
            System.out.print(msg);
            String line = sc.nextLine().trim();
            try {
                return Integer.parseInt(line);
            } catch (NumberFormatException nfe) {
                System.out.println("Please enter a whole number.");
            }
        }
    }

    private static int promptIntRange(Scanner sc, String msg, int min, int max) {
        while (true) {
            int v = promptInt(sc, msg);
            if (v >= min && v <= max) return v;
            System.out.println("Please enter a number between " + min + " and " + max + ".");
        }
    }

    // Extracts a simple value between startMarker and endMarker; returns null if not found.
    private static String extract(String text, String startMarker, String endMarker) {
        int s = text.indexOf(startMarker);
        if (s < 0) return null;
        s += startMarker.length();
        int e = text.indexOf(endMarker, s);
        if (e < 0) return null;
        return text.substring(s, e);
    }

    // Minimal JSON string escape (sufficient for our inputs)
    private static String escape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }
}

