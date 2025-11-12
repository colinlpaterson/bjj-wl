import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * BJJ Workout Log (terminal version)
 * - Prompts for workout info and N sparring rounds with details
 * - Appends each workout as one JSON line to workouts.jsonl (newline-delimited JSON)
 *   This is very friendly for future ingestion by a database or an app.
 */
public class BJJWorkoutLog {

    // -------- Data Models --------
    static class Round {
        String beltLevel;
        String partnerSize;      // free text for now (e.g., "light", "medium", "heavy", or lbs)
        Integer partnerAge;      // years
        Integer timesYouWereSubmitted;
        String submissionTypesAgainst; // e.g., "armbar, RNC"
        Integer timesYouSubmittedPartner;
        String submissionTypesFor;     // e.g., "kimura"
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
        String date;     // ISO string, e.g., 2025-11-11
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

    // -------- Main Program --------
    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);

        System.out.println("=== BJJ Workout Log ===");

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
            r.beltLevel = promptString(input, "1) Belt level of partner (e.g., white/blue/purple/brown/black): ");
            r.partnerSize = promptString(input, "2) Size of partner (e.g., light/med/heavy or lbs): ");
            r.partnerAge = promptInt(input, "3) Age of partner (integer): ");
            r.timesYouWereSubmitted = promptInt(input, "4) Number of times you were submitted: ");
            r.submissionTypesAgainst = promptString(input, "5) Submission type(s) they used (comma-separated): ");
            r.timesYouSubmittedPartner = promptInt(input, "6) Number of times you submitted partner: ");
            r.submissionTypesFor = promptString(input, "7) Submission type(s) you used (comma-separated): ");
            r.observations = promptString(input, "8) Observations of the round: ");
            rounds.add(r);
        }

        String notes = promptString(input, "\nAny overall notes? ");

        // Build workout object
        Workout w = new Workout();
        w.date = date;
        w.drills = drills;
        w.roundsCount = roundsCount;
        w.rounds = rounds;
        w.notes = notes;

        // Persist to file as one JSON line (scalable)
        Path out = Path.of("workouts.jsonl"); // NDJSON: 1 workout per line
        String jsonLine = w.toJson() + System.lineSeparator();
        try (BufferedWriter bw = Files.newBufferedWriter(
                out,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND)) {
            bw.write(jsonLine);
        } catch (IOException e) {
            System.err.println("Failed to write workout: " + e.getMessage());
            System.exit(1);
        }

        // Print a friendly summary
        System.out.println("\n--- Workout Saved ---");
        System.out.println("Date: " + w.date);
        System.out.println("Drills: " + w.drills);
        System.out.println("Rounds logged: " + w.roundsCount);
        System.out.println("Notes: " + w.notes);
        System.out.println("Appended to: " + out.toAbsolutePath());

        input.close();
    }

    // -------- Helpers --------
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

    // Minimal JSON string escape (enough for our simple inputs)
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
