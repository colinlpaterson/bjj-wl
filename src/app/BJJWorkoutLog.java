package app;

import io.JsonlRepository;
import io.WorkoutRepository;
import model.Round;
import model.Workout;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

public class BJJWorkoutLog {

    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        WorkoutRepository repo = new JsonlRepository();

        while (true) {
            System.out.println("\n=== BJJ Workout Log ===");
            System.out.println("1) Add workout");
            System.out.println("2) View recent workouts");
            System.out.println("3) Update existing workout (by date)");
            System.out.println("4) Export CSV");
            System.out.println("5) Stats (basic)");
            System.out.println("6) Quit");
            int choice = promptIntRange(in, "Choose an option (1-6): ", 1, 6);

            try {
                if (choice == 1) {
                    addWorkout(in, repo);
                } else if (choice == 2) {
                    viewRecent(in, repo);
                } else if (choice == 3) {
                    updateByDate(in, repo);
                } else if (choice == 4) {
                    exportCsv(repo);
                } else if (choice == 5) {
                    showStats(repo);
                } else {
                    System.out.println("Good training. See you next time!");
                    break;
                }
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        }
        in.close();
    }

    // ---------- Actions ----------

    private static void addWorkout(Scanner input, WorkoutRepository repo) throws Exception {
        System.out.println("\n-- Add Workout --");
        Workout w = promptWorkout(input);
        repo.append(w);
        System.out.println("\n--- Workout Saved ---");
        System.out.println("ID:    " + w.id);
        System.out.println("Date:  " + w.date);
        System.out.println("Drills:" + w.drills);
        System.out.println("Rounds logged: " + w.roundsCount);
        System.out.println("Notes: " + w.notes);
        System.out.println("Appended to: data/workouts.jsonl");
    }

    private static void viewRecent(Scanner input, WorkoutRepository repo) throws Exception {
        System.out.println("\n-- View Recent Workouts --");
        List<String> lines = repo.readAllJsonLines();
        if (lines.isEmpty()) {
            System.out.println("No workouts found yet. Log one first!");
            return;
        }

        int n = promptIntRange(input, "How many most recent workouts to show? ", 1, 1000);
        int start = Math.max(0, lines.size() - n);
        List<String> slice = lines.subList(start, lines.size());

        System.out.println("\n--- Showing " + slice.size() + " workout(s) ---");
        for (int i = 0; i < slice.size(); i++) {
            String json = slice.get(i);

            String id   = JsonlRepository.extract(json, "\"id\":\"", "\"");   // may be null for older entries
            String date = JsonlRepository.extract(json, "\"date\":\"", "\"");
            String drills = JsonlRepository.extract(json, "\"drills\":\"", "\"");
            String roundsCountStr = JsonlRepository.extract(json, "\"roundsCount\":", ",");
            String notes = JsonlRepository.extract(json, "\"notes\":\"", "\"");

            if (roundsCountStr == null) roundsCountStr = "0";
            int roundsCount = JsonlRepository.safeInt(roundsCountStr);
            if (notes == null) notes = "";

            System.out.println("\n#" + (start + i + 1));
            if (id != null && !id.isEmpty()) {
                System.out.println("ID:     " + id);
            }
            System.out.println("Date:   " + (date != null ? date : "(unknown)"));
            System.out.println("Drills: " + (drills != null ? drills : "(unknown)"));
            System.out.println("Rounds: " + roundsCount);
            if (!notes.isEmpty()) {
                System.out.println("Notes:  " + notes);
            }

            // Round details
            String roundsBlock = JsonlRepository.extract(json, "\"rounds\":[", "],\"notes\"");
            if (roundsBlock != null && !roundsBlock.trim().isEmpty()) {
                List<String> roundObjects = JsonlRepository.splitRoundObjects(roundsBlock);
                System.out.println("Round details:");
                for (int rIndex = 0; rIndex < roundObjects.size(); rIndex++) {
                    String rj = roundObjects.get(rIndex);

                    String belt = JsonlRepository.extract(rj, "\"beltLevel\":\"", "\"");
                    String size = JsonlRepository.extract(rj, "\"partnerSize\":\"", "\"");
                    String ageStr = JsonlRepository.extract(rj, "\"partnerAge\":", ",");
                    String durStr = JsonlRepository.extract(rj, "\"roundDurationMinutes\":", ",");
                    String subForStr = JsonlRepository.extract(rj, "\"timesYouSubmittedPartner\":", ",");
                    String subAgainstStr = JsonlRepository.extract(rj, "\"timesYouWereSubmitted\":", ",");
                    String obs = JsonlRepository.extract(rj, "\"observations\":\"", "\"");

                    int age = JsonlRepository.safeInt(ageStr);
                    int dur = JsonlRepository.safeInt(durStr);
                    int subFor = JsonlRepository.safeInt(subForStr);
                    int subAgainst = JsonlRepository.safeInt(subAgainstStr);

                    System.out.println("  Round " + (rIndex + 1) + ": "
                            + "belt=" + (belt == null ? "?" : belt)
                            + ", size=" + (size == null ? "?" : size)
                            + ", age=" + age
                            + ", dur=" + dur + "m"
                            + ", subs for/against=" + subFor + "/" + subAgainst);
                    if (obs != null && !obs.isEmpty()) {
                        System.out.println("    Notes: " + obs);
                    }
                }
            }
        }
        System.out.println("\n(End of list)");
    }

    private static void updateByDate(Scanner input, WorkoutRepository repo) throws Exception {
        System.out.println("\n-- Update Existing Workout (by date) --");
        List<String> lines = repo.readAllJsonLines();
        if (lines.isEmpty()) {
            System.out.println("No workouts found.");
            return;
        }

        String dateQuery = promptString(input, "Enter workout date to update (YYYY-MM-DD): ");
        if (dateQuery.isBlank()) {
            System.out.println("No date entered. Aborting.");
            return;
        }

        List<Integer> matches = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String json = lines.get(i);
            String date = JsonlRepository.extract(json, "\"date\":\"", "\"");
            if (dateQuery.equals(date)) matches.add(i);
        }

        if (matches.isEmpty()) {
            System.out.println("No workouts found for date " + dateQuery + ".");
            return;
        }

        int targetIndex;
        if (matches.size() == 1) {
            targetIndex = matches.get(0);
            System.out.println("Found 1 workout for " + dateQuery + ".");
        } else {
            System.out.println("Found " + matches.size() + " workouts for " + dateQuery + ":");
            for (int k = 0; k < matches.size(); k++) {
                int idx = matches.get(k);
                String json = lines.get(idx);
                String drills = JsonlRepository.extract(json, "\"drills\":\"", "\"");
                String roundsCount = JsonlRepository.extract(json, "\"roundsCount\":", ",");
                System.out.println((k + 1) + ") line " + (idx + 1) + " | drills=" + nz(drills) + " | rounds=" + nz(roundsCount));
            }
            targetIndex = promptIntRange(input, "Select which to update (1-" + matches.size() + "): ", 1, matches.size()) - 1;
            targetIndex = matches.get(targetIndex);
        }

        System.out.println("\nCurrent entry:");
        System.out.println(lines.get(targetIndex));
        String ok = promptString(input, "Replace this entry? (y/n): ").toLowerCase();
        if (!ok.startsWith("y")) {
            System.out.println("Update cancelled.");
            return;
        }

        System.out.println("\n-- Enter replacement workout --");
        Workout replacement = promptWorkout(input);
        repo.replaceLine(targetIndex, replacement);
        System.out.println("Updated workout at line " + (targetIndex + 1) + " for date " + dateQuery + ".");
    }

    private static void exportCsv(WorkoutRepository repo) throws Exception {
        System.out.println("\n-- Export CSV --");
        List<String> lines = repo.readAllJsonLines();
        if (lines.isEmpty()) {
            System.out.println("No workouts found.");
            return;
        }

        java.nio.file.Path workoutsCsv = java.nio.file.Path.of("data", "workouts.csv");
        java.nio.file.Path roundsCsv   = java.nio.file.Path.of("data", "rounds.csv");

        try (java.io.BufferedWriter wcsv = java.nio.file.Files.newBufferedWriter(
                    workoutsCsv,
                    java.nio.charset.StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);
             java.io.BufferedWriter rcsv = java.nio.file.Files.newBufferedWriter(
                    roundsCsv,
                    java.nio.charset.StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.TRUNCATE_EXISTING)) {

            wcsv.write("date,drills,roundsCount,notes\n");
            rcsv.write("date,roundIndex,beltLevel,partnerSize,partnerAge,roundDurationMinutes,"
                    + "timesYouWereSubmitted,submissionTypesAgainst,timesYouSubmittedPartner,"
                    + "submissionTypesFor,observations\n");

            for (String json : lines) {
                String date   = JsonlRepository.extract(json, "\"date\":\"", "\"");
                String drills = JsonlRepository.extract(json, "\"drills\":\"", "\"");
                String roundsCountStr = JsonlRepository.extract(json, "\"roundsCount\":", ",");
                String notes = JsonlRepository.extract(json, "\"notes\":\"", "\"");
                if (date == null) date = "";
                if (drills == null) drills = "";
                if (roundsCountStr == null) roundsCountStr = "0";
                if (notes == null) notes = "";
                int roundsCount = JsonlRepository.safeInt(roundsCountStr);

                wcsv.write(csv(date) + "," + csv(drills) + "," + roundsCount + "," + csv(notes) + "\n");

                String roundsBlock = JsonlRepository.extract(json, "\"rounds\":[", "],\"notes\"");
                if (roundsBlock == null || roundsBlock.trim().isEmpty()) continue;

                List<String> roundObjects = JsonlRepository.splitRoundObjects(roundsBlock);
                for (int idx = 0; idx < roundObjects.size(); idx++) {
                    String rj = roundObjects.get(idx);
                    String beltLevel = JsonlRepository.extract(rj, "\"beltLevel\":\"", "\"");
                    String partnerSize = JsonlRepository.extract(rj, "\"partnerSize\":\"", "\"");
                    String partnerAgeStr = JsonlRepository.extract(rj, "\"partnerAge\":", ",");
                    String durStr = JsonlRepository.extract(rj, "\"roundDurationMinutes\":", ",");
                    String timesYouWereSubmittedStr = JsonlRepository.extract(rj, "\"timesYouWereSubmitted\":", ",");
                    String submissionTypesAgainst = JsonlRepository.extract(rj, "\"submissionTypesAgainst\":\"", "\"");
                    String timesYouSubmittedPartnerStr = JsonlRepository.extract(rj, "\"timesYouSubmittedPartner\":", ",");
                    String submissionTypesFor = JsonlRepository.extract(rj, "\"submissionTypesFor\":\"", "\"");
                    String observations = JsonlRepository.extract(rj, "\"observations\":\"", "\"");

                    int partnerAge = JsonlRepository.safeInt(partnerAgeStr);
                    int dur = JsonlRepository.safeInt(durStr);
                    int timesYouWereSubmitted = JsonlRepository.safeInt(timesYouWereSubmittedStr);
                    int timesYouSubmittedPartner = JsonlRepository.safeInt(timesYouSubmittedPartnerStr);

                    rcsv.write(
                        csv(date) + "," +
                        (idx + 1) + "," +
                        csv(nz(beltLevel)) + "," +
                        csv(nz(partnerSize)) + "," +
                        partnerAge + "," +
                        dur + "," +
                        timesYouWereSubmitted + "," +
                        csv(nz(submissionTypesAgainst)) + "," +
                        timesYouSubmittedPartner + "," +
                        csv(nz(submissionTypesFor)) + "," +
                        csv(nz(observations)) + "\n"
                    );
                }
            }
        }

        System.out.println("Exported:");
        System.out.println(" - " + workoutsCsv.toAbsolutePath());
        System.out.println(" - " + roundsCsv.toAbsolutePath());
    }

    private static void showStats(WorkoutRepository repo) throws Exception {
        System.out.println("\n-- Stats (basic) --");

        List<String> lines = repo.readAllJsonLines();
        if (lines.isEmpty()) {
            System.out.println("No workouts found yet. Log one first!");
            return;
        }

        int totalWorkouts = lines.size();
        int totalRounds = 0;
        int totalSubFor = 0;
        int totalSubAgainst = 0;
        int totalDurationMinutes = 0;
        int roundsWithDuration = 0;

        for (String json : lines) {
            String roundsCountStr = JsonlRepository.extract(json, "\"roundsCount\":", ",");
            int roundsCount = JsonlRepository.safeInt(roundsCountStr);
            totalRounds += roundsCount;

            String roundsBlock = JsonlRepository.extract(json, "\"rounds\":[", "],\"notes\"");
            if (roundsBlock == null || roundsBlock.trim().isEmpty()) {
                continue;
            }

            List<String> roundObjects = JsonlRepository.splitRoundObjects(roundsBlock);
            for (String rj : roundObjects) {
                String subForStr = JsonlRepository.extract(rj, "\"timesYouSubmittedPartner\":", ",");
                String subAgainstStr = JsonlRepository.extract(rj, "\"timesYouWereSubmitted\":", ",");
                String durStr = JsonlRepository.extract(rj, "\"roundDurationMinutes\":", ",");

                int subFor = JsonlRepository.safeInt(subForStr);
                int subAgainst = JsonlRepository.safeInt(subAgainstStr);
                int dur = JsonlRepository.safeInt(durStr);

                totalSubFor += subFor;
                totalSubAgainst += subAgainst;

                if (dur > 0) {
                    totalDurationMinutes += dur;
                    roundsWithDuration++;
                }
            }
        }

        double avgDuration = 0.0;
        if (roundsWithDuration > 0) {
            avgDuration = (double) totalDurationMinutes / roundsWithDuration;
        }

        System.out.println("Total workouts:          " + totalWorkouts);
        System.out.println("Total rounds:            " + totalRounds);
        System.out.println("Total submissions (for): " + totalSubFor);
        System.out.println("Total submissions (vs):  " + totalSubAgainst);
        if (roundsWithDuration > 0) {
            System.out.printf("Avg round duration:      %.1f min (%d rounds with duration)%n",
                    avgDuration, roundsWithDuration);
        } else {
            System.out.println("Avg round duration:      (no duration data yet)");
        }
        System.out.println("-- End of stats --");
    }

    // ---------- Prompts & helpers ----------

    private static Workout promptWorkout(Scanner input) {
        String date = promptDate(input);

        String drills = promptString(input, "Enter drills practiced: ");

        int roundsCount = promptIntRange(input,
                "Enter number of sparring rounds (1–20): ", 1, 20);

        List<Round> rounds = new ArrayList<>();
        for (int i = 1; i <= roundsCount; i++) {
            System.out.println("\n-- Round " + i + " details --");
            Round r = new Round();

            r.beltLevel = promptBeltLevel(input);

            r.partnerSize = promptString(input,
                    "2) Size of partner (light/med/heavy or lbs): ");

            r.partnerAge = promptIntRange(input,
                    "3) Age of partner (10–80): ", 10, 80);

            r.roundDurationMinutes = promptIntRange(input,
                    "4) Round duration in minutes (1–15): ", 1, 15);

            r.timesYouWereSubmitted = promptIntRange(input,
                    "5) Times you were submitted (0–20): ", 0, 20);

            r.submissionTypesAgainst = promptString(input,
                    "6) Submission type(s) they used (comma-separated): ");

            r.timesYouSubmittedPartner = promptIntRange(input,
                    "7) Times you submitted partner (0–20): ", 0, 20);

            r.submissionTypesFor = promptString(input,
                    "8) Submission type(s) you used (comma-separated): ");

            r.observations = promptString(input,
                    "9) Observations of the round: ");

            rounds.add(r);
        }

        String notes = promptString(input, "\nAny overall notes? ");

        Workout w = new Workout();
        w.id = UUID.randomUUID().toString();  // NEW: assign unique ID
        w.date = date;
        w.drills = drills;
        w.roundsCount = roundsCount;
        w.rounds = rounds;
        w.notes = notes;
        return w;
    }

    private static String promptDate(Scanner input) {
        while (true) {
            String raw = promptString(input,
                    "Enter date (YYYY-MM-DD) [Enter for today]: ");
            if (raw.isBlank()) {
                String today = LocalDate.now().toString();
                System.out.println("Using today's date: " + today);
                return today;
            }
            try {
                LocalDate parsed = LocalDate.parse(raw);
                return parsed.toString();
            } catch (Exception e) {
                System.out.println("Please enter a valid date in the format YYYY-MM-DD.");
            }
        }
    }

    private static String promptBeltLevel(Scanner input) {
        while (true) {
            String raw = promptString(input,
                    "1) Belt level of partner (white/blue/purple/brown/black): ");
            String belt = raw.trim().toLowerCase();
            switch (belt) {
                case "white":
                case "blue":
                case "purple":
                case "brown":
                case "black":
                    return belt.substring(0, 1).toUpperCase() + belt.substring(1);
                default:
                    System.out.println("Please enter one of: white, blue, purple, brown, black.");
            }
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

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static String csv(String s) {
        String x = (s == null) ? "" : s;
        boolean needsQuotes = x.contains(",") || x.contains("\"") || x.contains("\n") || x.contains("\r");
        if (needsQuotes) x = "\"" + x.replace("\"", "\"\"") + "\"";
        return x;
    }
}

