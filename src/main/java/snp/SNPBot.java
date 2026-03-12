package snp;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class SNPBot {

    private final boolean debug;
    private final SNPLog log;
    private final TelegramClient client;

    private final Map<String, LocalDate> lastAlert = new HashMap<>();
    private boolean wasEmpty = false;
    private boolean wasError = false;

    private SNPBot(boolean debug, SNPLog log, TelegramClient client) {
        this.debug = debug;
        this.log = log;
        this.client = client;
        log("=== SNP bot started");
    }

    private void log(String message) {
        log.info(message);
    }

    private boolean alert(String message) {
        try {
            client.sendMessage("@SNP_alerts", EventBuilder.escape(message), this::log);
            return true;
        } catch (Exception ex) {
            log.error(ex);
            return false;
        }
    }

    private void alert(String type, String message) {
        LocalDate today = LocalDate.now();
        LocalDate last = lastAlert.get(type);
        if (last != null && !last.isBefore(today))
            return;
        if (alert(message)) {
            lastAlert.put(type, today);
        }
    }

    private void postNewEvents() throws IOException, InterruptedException {
        List<Event> allEvents = SNP.loadAllEvents();
        if (allEvents.isEmpty()) {
            wasEmpty = true;
            log.log("WARNING", "No events found!");
            alert("empty", "No events found!");
            return;
        } else {
            if (wasEmpty) {
                alert("Found events again!");
            }
            wasEmpty = false;
        }

        Map<String, ChatDB> postedInChats = new HashMap<>();
        for (Event event : allEvents) {
            for (String chatId : event.sendTo()) {
                ChatDB alreadyPosted = postedInChats.computeIfAbsent(chatId, ChatDB::new);
                if (alreadyPosted.containsLink(event.link))
                    continue;
                log("Sending to " + chatId + ": " + event);
                if (!debug) {
                    client.sendMessage(chatId, event.toHTML(), this::log);
                }
                alreadyPosted.addLink(event.link);
            }
        }
    }

    private void botAction() {
        log("Starting SNP scan...");
        try {
            postNewEvents();
            if (wasError) {
                alert("No more errors!");
            }
            wasError = false;
            log("Successfully finished SNP scan!");
        } catch (Exception ex) {
            wasError = true;
            log.error(ex);
            alert("error", "Error: " + ex);
        }
    }

    public static void main(String[] args) throws IOException {
        SNPLog log = SNPLog.create(args.length > 0 ? args[0] : null);
        TelegramClient client = TelegramClient.create();
        SNPBot bot = new SNPBot(args.length <= 0, log, client);
        Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(
            bot::botAction, 0, 30, TimeUnit.MINUTES
        );
    }
}
