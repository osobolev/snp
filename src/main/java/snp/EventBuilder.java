package snp;

import org.owasp.encoder.Encode;

import java.util.ArrayList;
import java.util.List;

final class EventBuilder {

    private final List<String> details = new ArrayList<>();

    static String escape(String text) {
        return Encode.forHtmlContent(text);
    }

    void addLine(String line) {
        if (line.isEmpty())
            return;
        details.add(line);
    }

    Event build(String title, String link, String price) {
        // todo: bold date
        List<String> lines = new ArrayList<>();
        lines.add(String.format("<b><a href=\"%s\">%s</a></b>", link, escape(title)));
        for (String detail : details) {
            lines.add(escape(detail));
        }
        if (price != null) {
            lines.add(escape("Цена: " + price));
        }
        String html = String.join("\n", lines);
        return new Event(title, link, html);
    }
}
