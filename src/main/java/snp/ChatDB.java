package snp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class ChatDB {

    private final Path dbFile;

    private Set<String> alreadyPosted = null;

    ChatDB(String chatId) {
        this.dbFile = Path.of(chatId + ".txt");
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

    boolean containsLink(String link) throws IOException {
        if (alreadyPosted == null) {
            alreadyPosted = loadLinks(dbFile);
        }
        return alreadyPosted.contains(link);
    }

    void addLink(String link) throws IOException {
        Files.write(dbFile, List.of(link), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
}
