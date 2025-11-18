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

    public void uploadTemperaturaBucket(String nomeArq) {
        String key = String.format("%s", nomeArq);
        try {
            s3.putObject(
                    PutObjectRequest.builder()
                            .bucket("s3-trusted-infomotion")
                            .key(key)
                            .contentType("csv")
                            .build(),
                    RequestBody.fromFile(Path.of(nomeArq))
            );

            System.out.println("Upload concluído: " + nomeArq + "\n");
            deleteCsvLocal(nomeArq);
        }
        catch (Exception e) {
            System.err.println("Erro ao fazer upload de " + nomeArq + ": " + e.getMessage());
        }
    }

    public void deleteCsvLocal(String nomeArq){
        try {
            Path caminho = Path.of(nomeArq);
            java.nio.file.Files.deleteIfExists(caminho);
            System.out.println("Arquivo deletado com sucesso: " + nomeArq + "\n");
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
    // NOVO MÉTODO: Baixar do Trusted (trusted como origem)
    public void downloadBucketTrusted(String nomeArq) {
        String key = String.format("alertas/%s", nomeArq); // Assumindo que tem o prefixo 'alertas/'
        try {
            s3.getObject(
                    GetObjectRequest.builder()
                            .bucket("s3-trusted-infomotion") // MUDANÇA AQUI: de raw para trusted
                            .key(key)
                            .build(),
                    Paths.get(nomeArq)
            );
        } catch (Exception e) {
            System.err.println("Erro ao baixar do Trusted " + nomeArq + ": " + e.getMessage());
            throw new RuntimeException("Falha no download do Trusted", e);
        }
    }

    // NOVO MÉTODO: Upload para o Client (client como destino)
    public void uploadBucketClient(String nomeArq) {
        String key = String.format("produto_final/%s", nomeArq); // Novo prefixo para o client
        try {
            s3.putObject(
                    PutObjectRequest.builder()
                            .bucket("s3-client-infomotion") // MUDANÇA AQUI: de trusted para client
                            .key(key)
                            .contentType("text/csv")
                            .build(),
                    RequestBody.fromFile(Path.of(nomeArq))
            );

            System.out.println("Upload concluído para CLIENT: " + nomeArq);
            // Não chamamos deleteCsvLocal aqui, pois será feito no finally da TrustedToClient
        }
        catch (Exception e) {
            System.err.println("Erro ao fazer upload para CLIENT " + nomeArq + ": " + e.getMessage());
            throw new RuntimeException("Falha no upload para Client", e);
        }
    }

}
