package snp;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class SNPBot {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    private final Path postedFile = Path.of("posted_links.txt");
    private final PrintWriter log;

    private SNPBot(PrintWriter log) {
        this.log = log;
        log("=== SNP bot started");
    }

    private static String getTimestamp() {
        return TIMESTAMP_FORMAT.format(LocalDateTime.now());
    }

    private void log(String level, String message) {
        log.println("[" + level + "] " + getTimestamp() + " | " + message);
        log.flush();
    }

    private void log(String message) {
        log("INFO", message);
    }

    private List<Event> loadNewEvents() throws IOException, InterruptedException {
        List<Event> allEvents = SNP.loadAllEvents();

        Set<String> postedLinks;
        if (Files.exists(postedFile)) {
            try (Stream<String> lines = Files.lines(postedFile)) {
                postedLinks = lines.collect(Collectors.toSet());
            }
        } else {
            postedLinks = Collections.emptySet();
        }

        return allEvents
            .stream()
            .filter(e -> !postedLinks.contains(e.link))
            .collect(Collectors.toList());
    }

    private void postNewEvents() throws IOException, InterruptedException {
        List<Event> newEvents = loadNewEvents();
        if (newEvents.isEmpty())
            return;
        TelegramClient client = TelegramClient.create();
        boolean first = true;
        for (Event event : newEvents) {
            log("Sending " + event);
            String html = event.toHTML();
            for (String chatId : event.sendTo()) {
                if (!first) {
                    Thread.sleep(1000);
                }
                first = false;
                client.sendMessage(chatId, html, this::log);
            }
            Files.write(postedFile, List.of(event.link), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }
    }

    private void botAction() {
        log("Starting SNP scan...");
        try {
            postNewEvents();
            log("Successfully finished SNP scan!");
        } catch (Exception ex) {
            log("ERROR", ex.toString());
            ex.printStackTrace(log);
        }
    }

    private static PrintWriter openLog(String[] args) throws IOException {
        if (args.length > 0) {
            return new PrintWriter(new FileOutputStream(args[0], true), true, StandardCharsets.UTF_8);
        } else {
            return new PrintWriter(System.out, true);
        }
    }

    public static void main(String[] args) throws IOException {
        SNPBot bot = new SNPBot(openLog(args));
        Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(
            bot::botAction, 0, 30, TimeUnit.MINUTES
        );
    }
}
