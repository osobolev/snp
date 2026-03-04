package snp;

import java.util.ArrayList;
import java.util.List;

final class Event {

    private final String title;
    final String link;
    private final List<String> details;
    private final String stage;
    private final String price;

    Event(String title, String link, List<String> details, String stage, String price) {
        this.title = title;
        this.link = link;
        this.details = details;
        this.stage = stage;
        this.price = price;
    }

    @Override
    public String toString() {
        return title + " (" + link + ")";
    }

    List<String> sendTo() {
        // todo: possibly use filter by stage
        return List.of("@SNP_sve");
    }

    String toHTML() {
        List<String> lines = new ArrayList<>();
        lines.add(String.format("<b><a href=\"%s\">%s</a></b>", link, EventBuilder.escape(title)));
        for (String detail : details) {
            lines.add(EventBuilder.escape(detail));
        }
        if (price != null) {
            lines.add(EventBuilder.escape("Цена: " + price));
        }
        return String.join("\n", lines);
    }
}
