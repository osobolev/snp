package snp;

import java.util.List;

final class Event {

    private final String title;
    final String link;
    final boolean canBuy;
    private final String html;

    Event(String title, String link, boolean canBuy, String html) {
        this.title = title;
        this.link = link;
        this.canBuy = canBuy;
        this.html = html;
    }

    @Override
    public String toString() {
        return "\"" + title + "\" (" + link + ")";
    }

    List<String> sendTo() {
        return List.of("@SNP_sve");
    }

    String toHTML() {
        return html;
    }
}
