package model;

import java.util.ArrayList;
import java.util.List;

public class Workout {
    // Unique identifier
    public String id;

    public String date;           // ISO yyyy-MM-dd
    public String workoutType;    // "Gi" or "No-gi"
    public String drills;
    public Integer roundsCount;
    public List<Round> rounds = new ArrayList<>();
    public String notes;

    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");

        // id is optional for older records, but we include it when present
        if (id != null && !id.isEmpty()) {
            sb.append("\"id\":\"").append(escape(id)).append("\",");
        }

        sb.append("\"date\":\"").append(escape(date)).append("\",");

        // workoutType is optional for older records too
        if (workoutType != null && !workoutType.isEmpty()) {
            sb.append("\"workoutType\":\"").append(escape(workoutType)).append("\",");
        }

        sb.append("\"drills\":\"").append(escape(drills)).append("\",")
          .append("\"roundsCount\":").append(roundsCount == null ? 0 : roundsCount).append(",")
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

    // Minimal JSON string escape
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

