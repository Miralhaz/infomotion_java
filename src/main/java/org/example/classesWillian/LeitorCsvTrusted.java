package org.example.classesWillian;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class LeitorCsvTrusted {

    private static final DateTimeFormatter CSV_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public List<LogsWillianDisco> lerCsvDisco(String caminhoCsv) {

        List<LogsWillianDisco> lista = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                new FileInputStream(caminhoCsv), StandardCharsets.UTF_8))) {

            String linha;
            br.readLine(); // ignorar cabeçalho

            while ((linha = br.readLine()) != null) {
                String[] c = linha.split(";");

                Integer fk = Integer.parseInt(c[0]);
                String nome = c[1];
                LocalDateTime ts = LocalDateTime.parse(c[2], CSV_FORMAT);

                Double usoDisco = Double.parseDouble(c[5].replace(",", "."));
                Double tempDisco = Double.parseDouble(c[7].replace(",", "."));

                LogsWillianDisco log = new LogsWillianDisco(
                        fk, nome, ts, usoDisco, tempDisco
                );

                lista.add(log);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return lista;
    }
}
