package snp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class SNPBot {

    private final Path postedFile = Path.of("posted_links.txt");
    private final SNPLog log;
    private final TelegramClient client;

    private SNPBot(SNPLog log, TelegramClient client) {
        this.log = log;
        this.client = client;
        log("=== SNP bot started");
    }

    private void log(String message) {
        log.info(message);
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
            log.error(ex);
        }
    }

    public static void main(String[] args) throws IOException {
        SNPLog log = SNPLog.create(args.length > 0 ? args[0] : null);
        TelegramClient client = TelegramClient.create();
        SNPBot bot = new SNPBot(log, client);
        Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(
            bot::botAction, 0, 30, TimeUnit.MINUTES
        );
    }
}
