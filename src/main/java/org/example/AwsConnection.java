package org.example;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

public class AwsConnection {
    private final S3Client s3 = S3Utils.createClient();

    public void downloadBucket(String nomeArq) {
        String key = String.format(nomeArq);

        s3.getObject(
                GetObjectRequest.builder()
                        .bucket("s3-raw-infomotion")
                        .key(key)
                        .build(),
                Paths.get(nomeArq)
        );
    }

    public void uploadBucket(String nomeArq) {
        String key = String.format("alertas/%s", nomeArq);
        try {
            s3.putObject(
                    PutObjectRequest.builder()
                            .bucket("s3-trusted-infomotion")
                            .key(key)
                            .contentType("text/csv")
                            .build(),
                    RequestBody.fromFile(Path.of(nomeArq))
            );

        System.out.println("Upload concluído: " + nomeArq);
        deleteCsvLocal(nomeArq);
        }
     catch (Exception e) {
        System.err.println("Erro ao fazer upload de " + nomeArq + ": " + e.getMessage());
        }
    }

    public void deleteCsvLocal(String nomeArq){
        String key = String.format("alertas/%s", nomeArq);
        try {
            Path caminho = Path.of(nomeArq);
            java.nio.file.Files.deleteIfExists(caminho);
            System.out.println("Arquivo deletado com sucesso: " + nomeArq);
        } catch (Exception e) {
            System.err.println("Erro ao deletar o arquivo " + nomeArq + ": " + e.getMessage());
        }
    }

    public void limparTemporarios() {
        try {
            var arquivos = java.nio.file.Files.list(Path.of("."))
                    .filter(p -> p.toString().endsWith(".csv") || p.toString().endsWith(".json"))
                    .toList();

            for (Path p : arquivos) {
                try {
                    java.nio.file.Files.deleteIfExists(p);
                    System.out.println("Arquivo temporário removido: " + p.getFileName());
                } catch (Exception e) {
                    System.err.println("Erro ao deletar " + p.getFileName() + ": " + e.getMessage());
                }
            }

        } catch (Exception e) {
            System.err.println("Erro ao limpar arquivos temporários: " + e.getMessage());
        }
    }
}
