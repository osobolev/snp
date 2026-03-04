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

    private void alert(String message) {
        try {
            client.sendMessage("@SNP_alerts", Event.escape(message), this::log);
        } catch (Exception ex) {
            log.error(ex);
        }
    }

    private static Set<String> loadLinks(Path file) throws IOException {
        if (Files.exists(file)) {
            try (Stream<String> lines = Files.lines(file)) {
                return lines.collect(Collectors.toSet());
            }
        } else {
            return Collections.emptySet();
        }
    }

    private void postNewEvents() throws IOException, InterruptedException {
        List<Event> allEvents = SNP.loadAllEvents();
        if (allEvents.isEmpty()) {
            alert("No events found!");
            return;
        }

        Set<String> postedLinks = loadLinks(postedFile);
        boolean first = true;
        for (Event event : allEvents) {
            if (postedLinks.contains(event.link))
                continue;
            String html = event.toHTML();
            for (String chatId : event.sendTo()) {
                log("Sending to " + chatId + ": " + event);
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
            alert("Error: " + ex);
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
