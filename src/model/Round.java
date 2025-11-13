package model;

public class Round {
    public String beltLevel;
    public String partnerSize;            // free text (e.g., light/med/heavy or lbs)
    public Integer partnerAge;
    public Integer roundDurationMinutes;  // NEW: duration of the round in minutes
    public Integer timesYouWereSubmitted;
    public String submissionTypesAgainst; // comma-separated
    public Integer timesYouSubmittedPartner;
    public String submissionTypesFor;     // comma-separated
    public String observations;

    public String toJson() {
        return new StringBuilder()
            .append("{")
            .append("\"beltLevel\":\"").append(escape(beltLevel)).append("\",")
            .append("\"partnerSize\":\"").append(escape(partnerSize)).append("\",")
            .append("\"partnerAge\":").append(partnerAge == null ? 0 : partnerAge).append(",")
            .append("\"roundDurationMinutes\":").append(roundDurationMinutes == null ? 0 : roundDurationMinutes).append(",")
            .append("\"timesYouWereSubmitted\":").append(timesYouWereSubmitted == null ? 0 : timesYouWereSubmitted).append(",")
            .append("\"submissionTypesAgainst\":\"").append(escape(submissionTypesAgainst)).append("\",")
            .append("\"timesYouSubmittedPartner\":").append(timesYouSubmittedPartner == null ? 0 : timesYouSubmittedPartner).append(",")
            .append("\"submissionTypesFor\":\"").append(escape(submissionTypesFor)).append("\",")
            .append("\"observations\":\"").append(escape(observations)).append("\"")
            .append("}")
            .toString();
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

