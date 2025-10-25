package org.example;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class SlackNotifier {

    //private static final String SLACK_TOKEN = "xoxb-9411844966034-9766757433092-BGDm1FpBxA5dOxavjtJgZazn";
    private static final String CHANNEL = "#alertas";

    public static void sendSlackMessage(String message) throws Exception {
        String jsonPayload = String.format(
                "{\"channel\":\"%s\",\"text\":\"%s\"}",
                CHANNEL,
                message.replace("\"", "\\\"")
                        .replace("\n", "\\n")  // Adicione isso
                        .replace("\r", "\\r")
                        .replace("\t", "\\t")
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://slack.com/api/chat.postMessage"))
                .header("Authorization", "Bearer " + SLACK_TOKEN)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            System.err.println("Erro ao enviar mensagem: " + response.body());
        } else {
            System.out.println("Mensagem enviada com sucesso!");
        }
    }
}