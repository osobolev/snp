package snp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class SNPBot {

    private final SNPLog log;
    private final TelegramClient client;
    private final Map<String, LocalDate> lastAlert = new HashMap<>();

    private SNPBot(SNPLog log, TelegramClient client) {
        this.log = log;
        this.client = client;
        log("=== SNP bot started");
    }

    private void log(String message) {
        log.info(message);
    }

    private void alert(String type, String message) {
        LocalDate today = LocalDate.now();
        LocalDate last = lastAlert.get(type);
        if (last != null && !last.isBefore(today))
            return;
        try {
            client.sendMessage("@SNP_alerts", EventBuilder.escape(message), this::log);
            lastAlert.put(type, today);
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
            alert("empty", "No events found!");
            return;
        }

        Map<String, Set<String>> postedInChats = new HashMap<>();
        boolean first = true;
        for (Event event : allEvents) {
            for (String chatId : event.sendTo()) {
                Path chatDb = Path.of(chatId + ".txt");
                Set<String> alreadyPosted = postedInChats.get(chatId);
                if (alreadyPosted == null) {
                    alreadyPosted = loadLinks(chatDb);
                    postedInChats.put(chatId, alreadyPosted);
                }
                if (alreadyPosted.contains(event.link))
                    continue;
                log("Sending to " + chatId + ": " + event);
                if (!first) {
                    Thread.sleep(2000);
                }
                first = false;
                client.sendMessage(chatId, event.toHTML(), this::log);
                Files.write(chatDb, List.of(event.link), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            }
        }
    }

    private void botAction() {
        log("Starting SNP scan...");
        try {
            postNewEvents();
            log("Successfully finished SNP scan!");
        } catch (Exception ex) {
            log.error(ex);
            alert("error", "Error: " + ex);
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
