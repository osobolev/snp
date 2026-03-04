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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SNP {

    private static final Pattern STAGE_PATTERN = Pattern.compile(
        "Сцена:\\s*(.*)",
        Pattern.UNICODE_CHARACTER_CLASS |  Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private record Title(
        String title,
        String link
    )
    {}

    private record Event(
        String title,
        String link,
        List<String> details,
        String stage,
        String price
    )
    {}

    private static String getStage(String text) {
        Matcher matcher = STAGE_PATTERN.matcher(text);
        if (!matcher.find())
            return null;
        return matcher.group(1);
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
        List<String> details = new ArrayList<>();
        String stage = null;
        String price = null;
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
                        if (!line.isEmpty()) {
                            details.add(line);
                            String maybeStage = getStage(line);
                            if (maybeStage != null) {
                                stage = maybeStage;
                            }
                        }
                    }
                }
            }
        }
        if (title == null)
            return null;
        return new Event(title.title, title.link, details, stage, price);
    }

    private static List<Event> loadAllEvents() throws IOException, InterruptedException {
        List<Event> events = new ArrayList<>();
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest get = HttpRequest
                .newBuilder(URI.create("https://prodaja.snp.org.rs/sr/site/index"))
                .build();
            HttpResponse<String> resp = client.send(get, HttpResponse.BodyHandlers.ofString());
            String body = resp.body();
            Document doc = Jsoup.parse(body);
            Elements reps = doc.select("div.repertoireCnt");
            for (Element rep : reps) {
                Event event = parseEvent(rep);
                if (event != null) {
                    events.add(event);
                }
            }
        }
        return events;
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        List<Event> events = loadAllEvents();
        for (Event event : events) {
            System.out.println(event);
        }
    }
}
