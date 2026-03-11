package snp;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

final class SNP {

    private static final class Title {

        final String title;
        final String link;

        Title(String title, String link) {
            this.title = title;
            this.link = link;
        }
    }

    private static boolean canBuy(Elements buttons) {
        if (buttons.isEmpty())
            return true;
        for (Element button : buttons) {
            String name = button.attr("name");
            if ("reserveEvent".equals(name))
                return true;
        }
        return false;
    }

    private static Title parseTitle(Elements h4) {
        Element link = h4.selectFirst("a");
        if (link == null)
            return null;
        String href = link.attr("href");
        Elements remark = h4.select("span.remark_text");
        String remarkText = remark.text().strip();
        remark.remove();
        String title = h4.text().strip();
        String fullTitle;
        if (remarkText.isEmpty()) {
            fullTitle = title;
        } else {
            fullTitle = title + " " + remarkText;
        }
        return new Title(fullTitle, href);
    }

    private static Event parseEvent(Element rep) {
        Title title = null;
        EventBuilder eb = new EventBuilder();
        String price = "";
        for (Element row : rep.select("div.row")) {
            if (row.hasClass("buttons")) {
                Elements buttons = row.select("input[type=\"button\"]");
                if (!canBuy(buttons))
                    return null;
                Elements priceRange = row.select("span.price_range");
                price = priceRange.text().strip();
            } else {
                Elements h4 = rep.select("h4");
                if (!h4.isEmpty()) {
                    title = parseTitle(h4);
                }
                for (Element item : row.select("div.data-item")) {
                    Iterator<String> lines = item.wholeText().lines().iterator();
                    while (lines.hasNext()) {
                        String line = lines.next().strip();
                        eb.addLine(line);
                    }
                }
            }
        }
        if (title == null)
            return null;
        return eb.build(title.title, title.link, price);
    }

    static List<Event> loadAllEvents() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest get = HttpRequest
            .newBuilder(URI.create("https://prodaja.snp.org.rs/sr/site/index"))
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/145.0.0.0 Safari/537.36")
            .build();
        HttpResponse<String> resp = client.send(get, HttpResponse.BodyHandlers.ofString());
        String body = resp.body();
        Document doc = Jsoup.parse(body);
        Elements reps = doc.select("div.repertoireCnt");
        List<Event> events = new ArrayList<>();
        for (Element rep : reps) {
            Event event = parseEvent(rep);
            if (event != null) {
                events.add(event);
            }
        }
        return events;
    }
}
