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
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SNPParse {

    // Уторак, 03. март 2026. у 19:00 - Сцена: Joвaн Ђoрђeвић
    private static final Pattern MAIN_ROW = Pattern.compile(
        "\\p{Alpha}+,?\\s*(\\d+)\\.?\\s*(\\p{Alpha}+)\\s*(\\d{4})\\.?\\s*\\p{Alpha}+\\s*(\\d{1,2}:\\d{2})\\s*\\S+?\\s*Сцена:?\\s*(.*)",
        Pattern.UNICODE_CHARACTER_CLASS |  Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );
    // 2.700,00 - 4.200,00 RSD
    private static final Pattern PRICE2_ROW = Pattern.compile(
        "([0-9.,]+)\\s*[^0-9.,]+\\s*([0-9.,]+)\\s*RSD",
        Pattern.UNICODE_CHARACTER_CLASS |  Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );
    private static final Pattern PRICE1_ROW = Pattern.compile(
        "([0-9.,]+)\\s*RSD",
        Pattern.UNICODE_CHARACTER_CLASS |  Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private static final Locale SR = Locale.of("sr");
    private static final DateTimeFormatter DATE_PATTERN = DateTimeFormatter.ofPattern("dd MMMM yyyy", SR);
    private static final DateTimeFormatter TIME_PATTERN = DateTimeFormatter.ofPattern("HH:mm", SR);
    private static final NumberFormat PRICE_FORMAT = NumberFormat.getNumberInstance(SR);

    private record PreTitle(
        String title,
        String link
    )
    {}

    private record PreEvent(
        LocalDateTime time,
        String stage
    )
    {}

    private record Price(
        int min, int max
    )
    {}

    private record Event(
        String title,
        String link,
        LocalDateTime time,
        String stage,
        Price price
    )
    {}

    private static int parsePriceValue(String price) {
        try {
            Number number = PRICE_FORMAT.parse(price);
            return number.intValue();
        } catch (ParseException ex) {
            return 0;
        }
    }

    private static Price parsePrice(String text) {
        {
            Matcher matcher = PRICE2_ROW.matcher(text);
            if (matcher.matches()) {
                int min = parsePriceValue(matcher.group(1));
                int max = parsePriceValue(matcher.group(2));
                return new Price(min, max);
            }
        }
        {
            Matcher matcher = PRICE1_ROW.matcher(text);
            if (matcher.matches()) {
                int price = parsePriceValue(matcher.group(1));
                return new Price(price, price);
            }
        }
        return null;
    }

    private static PreEvent parsePreEvent(String text) {
        Matcher matcher = MAIN_ROW.matcher(text);
        if (!matcher.matches())
            return null;
        String day = matcher.group(1);
        String month = matcher.group(2);
        String year = matcher.group(3);
        String timeStr = matcher.group(4);
        String stage = matcher.group(5);
        LocalDate date = LocalDate.parse(day + " " + month + " " + year, DATE_PATTERN);
        LocalTime time = LocalTime.parse(timeStr, TIME_PATTERN);
        return new PreEvent(date.atTime(time), stage);
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

    private static PreTitle parseTitle(Elements h4) {
        Element link = h4.selectFirst("a");
        String href;
        if (link != null) {
            href = link.attr("href");
        } else {
            href = null;
        }
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
        return new PreTitle(fullTitle, href);
    }

    private static Event parseEvent(Element rep) {
        PreTitle title = null;
        PreEvent pre = null;
        Price price = null;
        for (Element row : rep.select("div.row")) {
            if (row.hasClass("buttons")) {
                Elements buttons = row.select("input[type=\"button\"]");
                if (!canBuy(buttons))
                    return null;
                Elements priceRange = row.select("span.price_range");
                price = parsePrice(priceRange.text().strip());
            } else {
                Elements h4 = rep.select("h4");
                if (!h4.isEmpty()) {
                    title = parseTitle(h4);
                }
                for (Element item : row.select("div.data-item")) {
                    PreEvent maybePre = parsePreEvent(item.text().strip());
                    if (maybePre != null) {
                        pre = maybePre;
                    }
                }
            }
        }
        if (pre == null || title == null)
            return null;
        return new Event(title.title, title.link, pre.time, pre.stage, price);
    }

    public static void main(String[] args) throws IOException, InterruptedException {
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
                System.out.println(event);
            }
        }
    }
}
