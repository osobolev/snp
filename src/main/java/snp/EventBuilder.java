package snp;

import org.owasp.encoder.Encode;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class EventBuilder {

    // Уторак, 03. март 2026. у 19:00 - Сцена: Joвaн Ђoрђeвић
    private static final Pattern MAIN_ROW = Pattern.compile(
        "\\p{Alpha}+,?\\s*(\\d+)\\.?\\s*(\\p{Alpha}+)\\s*(\\d{4})\\.?\\s*\\p{Alpha}+\\s*(\\d{1,2}:\\d{2})\\s*\\S+?\\s*Сцена:?\\s*(.*)",
        Pattern.UNICODE_CHARACTER_CLASS |  Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private final List<String> htmlLines = new ArrayList<>();

    static String escape(String text) {
        return Encode.forHtmlContent(text);
    }

    void addLine(String line) {
        if (line.isEmpty())
            return;
        Matcher matcher = MAIN_ROW.matcher(line);
        String html;
        if (matcher.matches()) {
            int from = matcher.start(1);
            int to = matcher.end(2);
            html = escape(line.substring(0, from)) + "<b>" + escape(line.substring(from, to)) + "</b>" + escape(line.substring(to));
        } else {
            html = escape(line);
        }
        htmlLines.add(html);
    }

    Event build(String title, String link, String price) {
        List<String> lines = new ArrayList<>();
        lines.add(String.format("<b><a href=\"%s\">%s</a></b>", link, escape(title)));
        lines.addAll(htmlLines);
        if (price != null) {
            lines.add(escape("Цена: " + price));
        }
        String html = String.join("\n", lines);
        return new Event(title, link, html);
    }
}
