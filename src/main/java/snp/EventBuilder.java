package snp;

import org.owasp.encoder.Encode;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class EventBuilder {

    private static final Pattern STAGE_PATTERN = Pattern.compile(
        "Сцена:\\s*(.*)",
        Pattern.UNICODE_CHARACTER_CLASS |  Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private final List<String> details = new ArrayList<>();
    private String stage = null;

    static String escape(String text) {
        return Encode.forHtmlContent(text);
    }

    private static String getStage(String text) {
        Matcher matcher = STAGE_PATTERN.matcher(text);
        if (!matcher.find())
            return null;
        return matcher.group(1);
    }

    void addLine(String line) {
        if (line.isEmpty())
            return;
        details.add(line);
        String maybeStage = getStage(line);
        if (maybeStage != null) {
            stage = maybeStage;
        }
    }

    Event build(String title, String link, String price) {
        return new Event(title, link, details, stage, price);
    }
}
