package org.example;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

public class AwsConnection {

    private final Scanner scanner = new Scanner(System.in);
    private final S3Client s3 = S3Utils.createClient();

    public String dbUserName() {
        System.out.println("\nDigite o nome do usuario do banco de dados:");
        return scanner.next();
    }

    public String dbSenha() {
        System.out.println("\nDigite a senha do banco de dados:");
        return scanner.next();
    }

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

        s3.putObject(
                PutObjectRequest.builder()
                        .bucket("s3-trusted-infomotion")
                        .key(key)
                        .contentType("text/csv")
                        .build(),
                RequestBody.fromFile(Path.of(nomeArq))
        );
    }
}
