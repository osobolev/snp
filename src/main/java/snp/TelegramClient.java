package snp;

import smalljson.JSONObject;
import smalljson.JSONWriter;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.function.Consumer;

import static smalljson.JSONFactory.JSON;

final class TelegramClient {

    private final String botToken;

    TelegramClient(String botToken) {
        this.botToken = botToken;
    }

    static TelegramClient create() throws IOException {
        Properties properties = new Properties();
        try (BufferedReader rdr = Files.newBufferedReader(Path.of("telegram.properties"))) {
            properties.load(rdr);
        }
        String botToken = properties.getProperty("bot.token");
        return new TelegramClient(botToken);
    }

    private Integer maybeSendMessage(String chatId, String html) throws IOException, InterruptedException {
        String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";

        JSONObject obj = new JSONObject();
        obj.put("chat_id", chatId);
        obj.put("text", html);
        obj.put("parse_mode", "HTML");
        String json = JSONWriter.toString(obj);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest
            .newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        int code = response.statusCode();
        if (code != 200) {
            JSONObject error;
            try {
                error = JSON.parseObject(response.body());
            } catch (Exception ex) {
                error = JSON.newObject();
            }
            if (code == 429) {
                Integer retryAfter;
                JSONObject parameters = error.opt("parameters", JSONObject.class);
                if (parameters != null) {
                    retryAfter = parameters.opt("retry_after", Integer.class);
                } else {
                    retryAfter = null;
                }
                return retryAfter == null ? 1 : retryAfter.intValue();
            }
            String description = error.opt("description", String.class);
            String message = description == null ? "HTTP error code: " + code : description;
            throw new IOException(message);
        }
        return null;
    }

    void sendMessage(String chatId, String html, Consumer<String> logger) throws IOException, InterruptedException {
        int tries = 0;
        while (true) {
            tries++;
            Integer retryAfter = maybeSendMessage(chatId, html);
            if (retryAfter == null)
                return;
            if (tries >= 3)
                throw new IOException("Too many retries");
            logger.accept("Retry " + tries + " - waiting for " + retryAfter + " sec...");
            Thread.sleep(retryAfter.intValue() * 1000L);
        }
    }
}
