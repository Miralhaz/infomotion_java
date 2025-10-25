package org.example;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

public class AwsConnection {
    Scanner scanner = new Scanner(System.in);

    public String dbUserName(){
        System.out.println("\nDigite o nome do usuario do banco de dados");
        return scanner.next();
    }

    public String dbSenha(){
        System.out.println("\nDigite a senha do banco de dados");
        return scanner.next();
    }

    S3Client s3 = S3Client.builder()
            .region(Region.US_EAST_1).build();

    public void downloadBuckat(String nomeArq) {
        String key = String.format("pasta/%s",nomeArq);
        s3.getObject(
                GetObjectRequest.builder()
                        .bucket("s3-raw-infomotion")
                        .key(key)
                        .build(),
                Paths.get(nomeArq)
        );
    }

    public void uploadBucket(String nomeArq) {
        String key = String.format("pasta/%s",nomeArq);
        s3.putObject(
                PutObjectRequest.builder()
                        .bucket("s3-trusted-infomotion")
                        .key("/pasta")
                        .contentType("text/csv")
                        .contentDisposition("inline")
                        .build(),
                RequestBody.fromFile(Path.of("relatorio.csv"))
        );
    }
}
