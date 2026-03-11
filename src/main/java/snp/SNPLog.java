package snp;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

final class SNPLog {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
    private static final ZoneId CET = ZoneId.of("Europe/Belgrade");

    private final PrintWriter log;

    private SNPLog(PrintWriter log) {
        this.log = log;
    }

    static SNPLog create(String fileName) throws IOException {
        PrintWriter pw;
        if (fileName != null) {
            pw = new PrintWriter(new FileOutputStream(fileName, true), true, StandardCharsets.UTF_8);
        } else {
            pw = new PrintWriter(System.out, true);
        }
        return new SNPLog(pw);
    }

    private static String getTimestamp() {
        return TIMESTAMP_FORMAT.format(LocalDateTime.now(CET));
    }

    void log(String level, String message) {
        log.println("[" + level + "] " + getTimestamp() + " | " + message);
        log.flush();
    }

    void info(String message) {
        log("INFO", message);
    }

    void error(Throwable ex) {
        log("ERROR", ex.toString());
        ex.printStackTrace(log);
        log.flush();
    }
}
