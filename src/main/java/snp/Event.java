package snp;

import java.util.List;

final class Event {

    final String title;
    final String link;
    final List<String> details;
    final String stage;
    final String price;

    Event(String title, String link, List<String> details, String stage, String price) {
        this.title = title;
        this.link = link;
        this.details = details;
        this.stage = stage;
        this.price = price;
    }
}
