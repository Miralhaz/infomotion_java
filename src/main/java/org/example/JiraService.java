package org.example;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class JiraService {

    private static final String JIRA_BASE_URL = "https://sentinela-grupo-3.atlassian.net";
    private static final String API_EMAIL = "lucas.silva051@sptech.school";
    private static final String API_TOKEN = "";
    private static final String PROJECT_KEY = "INF";
    private static final String ISSUE_TYPE_ID = "10057";

    private static String getBasicAuthHeader() {

        String auth = API_EMAIL + ":" + API_TOKEN;

        return "Basic " + Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

    }


    /**
     * Cria um ticket no Jira Service Management.
     *
     * @param summary     O título do ticket.
     * @param description O corpo do ticket.
     * @throws Exception Se a chamada à API falhar.
     */

    public static void createAlertTicket(String summary, String description) throws Exception {

        String formattedSummary = summary.replace("\"", "\\\"")
                .replace("\n", " ") // Substitui por espaço
                .replace("\r", " ");

        String formattedDescription = description.replace("\"", "\\\"")
                .replace("\n", " ") // Apenas substitui por espaço
                .replace("\r", " ");

        String jsonPayload = String.format("""
            {
              "fields": {
                "project": {
                  "key": "%s"
                },
                "summary": "%s",
                "issuetype": {
                  "id": "%s"
                },
                "description": "%s", 
                "reporter": {
                  "emailAddress": "%s" 
                }
              }
            }
            """, PROJECT_KEY, formattedSummary, ISSUE_TYPE_ID, formattedDescription, API_EMAIL);


        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(JIRA_BASE_URL + "/rest/api/2/issue"))
                .header("Authorization", getBasicAuthHeader())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 201) {
                System.out.println("Ticket de Alerta criado com sucesso!");
            } else {
                System.err.println("Erro ao criar ticket JSM. Status: " + response.statusCode());
                System.err.println("Resposta da API: " + response.body());
                throw new IOException("Falha na API do Jira: " + response.statusCode());
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Exceção ao chamar a API do Jira: " + e.getMessage());
            throw e;
        }

    }
}