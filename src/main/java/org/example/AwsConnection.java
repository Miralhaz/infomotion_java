package org.example;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class AwsConnection {

    private final S3Client s3 = S3Utils.createClient();

    public S3Client getS3Client() {
        return s3;
    }

    public void downloadBucketRaw(String nomeArq) {
        String key = String.format(nomeArq);

        s3.getObject(
                GetObjectRequest.builder()
                        .bucket("s3-raw-infomotion-1")
                        .key(key)
                        .build(),
                Paths.get(nomeArq)
        );
    }

    public List<String> listarArquivosRaw() {
        List<String> chaves = new ArrayList<>();

        String regexFiltro = "^data\\d+\\.csv$";

        ListObjectsV2Request listReq = ListObjectsV2Request.builder()
                .bucket("s3-raw-infomotion-1")
                .build();

        s3.listObjectsV2Paginator(listReq)
                .stream()
                .forEach(response ->
                        response.contents().forEach(s3Object -> {
                            String key = s3Object.key();

                            if (key.matches(regexFiltro)) {
                                chaves.add(key);
                            }
                        })
                );

        return chaves;
    }

    public List<String> listarEspecificacoesRaw() {
        List<String> chaves = new ArrayList<>();

        String regexFiltro = "^EspecificacoesHardware\\d+\\.csv$";

        ListObjectsV2Request listReq = ListObjectsV2Request.builder()
                .bucket("s3-raw-infomotion-1")
                .build();

        s3.listObjectsV2Paginator(listReq)
                .stream()
                .forEach(response ->
                        response.contents().forEach(s3Object -> {
                            String key = s3Object.key();

                            if (key.matches(regexFiltro)) {
                                chaves.add(key);
                            }
                        })
                );

        return chaves;
    }

    public List<String> listarArquivosRawProcessos() {
        List<String> chaves = new ArrayList<>();

        String regexFiltro = "^processos\\d+\\.csv$";

        ListObjectsV2Request listReq = ListObjectsV2Request.builder()
                .bucket("s3-raw-infomotion-1")
                .build();

        s3.listObjectsV2Paginator(listReq)
                .stream()
                .forEach(response ->
                        response.contents().forEach(s3Object -> {
                            String key = s3Object.key();

                            if (key.matches(regexFiltro)) {
                                chaves.add(key);
                            }
                        })
                );

        return chaves;
    }

    public void uploadTemperaturaComPasta(Integer idServidor, String nomeArq) {
        String key = String.format("tratamentoMiralha/temperatura/cpu/servidor_%d/%s", idServidor, nomeArq);
        try {
            s3.putObject(
                    PutObjectRequest.builder()
                            .bucket("s3-client-infomotion-1")
                            .key(key)
                            .contentType("application/json")
                            .build(),
                    RequestBody.fromFile(Path.of(nomeArq))
            );

            System.out.println("  → Upload concluído: " + key);
            deleteCsvLocal(nomeArq);
        }
        catch (Exception e) {
            System.err.println("Erro ao fazer upload de " + nomeArq + ": " + e.getMessage());
        }
    }

    public void uploadDiscoComPasta(Integer idServidor, String nomeArq) {
        String key = String.format("tratamentoMiralha/temperatura/disco/servidor_%d/%s", idServidor, nomeArq);
        try {
            s3.putObject(
                    PutObjectRequest.builder()
                            .bucket("s3-client-infomotion-1")
                            .key(key)
                            .contentType("application/json")
                            .build(),
                    RequestBody.fromFile(Path.of(nomeArq))
            );

            System.out.println("  → Upload concluído: " + key);
            deleteCsvLocal(nomeArq);
        }
        catch (Exception e) {
            System.err.println("Erro ao fazer upload de " + nomeArq + ": " + e.getMessage());
        }
    }

    public void downloadBucketTrusted(String nomeArq) {
        String key = nomeArq;
        Path destino = Paths.get(nomeArq);

        try {
            if (destino.getParent() != null) {
                Files.createDirectories(destino.getParent());
            }

            s3.getObject(
                    GetObjectRequest.builder()
                            .bucket("s3-trusted-infomotion-1")
                            .key(key)
                            .build(),
                    destino
            );
            System.out.println("Download concluído: " + nomeArq);
        } catch (software.amazon.awssdk.services.s3.model.NoSuchKeyException e) {
            System.out.println("Aviso: Arquivo '" + nomeArq + "' não encontrado no S3 Trusted.");
        } catch (Exception e) {
            System.err.println("Erro ao baixar do Trusted " + nomeArq + ": " + e.getMessage());
            throw new RuntimeException("Falha no download do Trusted", e);
        }
    }

    public void uploadBucketTrusted(String nomeArq) {
        String key = String.format("%s", nomeArq);
        try {
            s3.putObject(
                    PutObjectRequest.builder()
                            .bucket("s3-trusted-infomotion-1")
                            .key(key)
                            .contentType("text/csv")
                            .build(),
                    RequestBody.fromFile(Path.of(nomeArq))
            );

            System.out.println("Upload concluído: " + nomeArq);
        }
        catch (Exception e) {
            System.err.println("Erro ao fazer upload de " + nomeArq + ": " + e.getMessage());
        }
    }

    public void downloadBucketProcessosTrusted(String nomeArq) {
        String key = nomeArq;
        Path destino = Paths.get(nomeArq);

        try {
            if (destino.getParent() != null) {
                Files.createDirectories(destino.getParent());
            }

            s3.getObject(
                    GetObjectRequest.builder()
                            .bucket("s3-trusted-infomotion-1")
                            .key(key)
                            .build(),
                    destino
            );
        } catch (software.amazon.awssdk.services.s3.model.NoSuchKeyException e) {
            System.out.println("Aviso: Arquivo '" + nomeArq + "' não encontrado no S3 Trusted. Prosseguindo.");
        } catch (Exception e) {
            System.err.println("Erro ao baixar do Trusted " + nomeArq + ": " + e.getMessage());
            throw new RuntimeException("Falha no download do Trusted", e);
        }
    }

    public void uploadBucketClient(String nomeArq) {
        String key = String.format("produto_final/%s", nomeArq);
        try {
            s3.putObject(
                    PutObjectRequest.builder()
                            .bucket("s3-client-infomotion-1")
                            .key(key)
                            .contentType("text/csv")
                            .build(),
                    RequestBody.fromFile(Path.of(nomeArq))
            );

            System.out.println("Upload concluído para CLIENT: " + nomeArq);
        }
        catch (Exception e) {
            System.err.println("Erro ao fazer upload para CLIENT " + nomeArq + ": " + e.getMessage());
            throw new RuntimeException("Falha no upload para Client", e);
        }
    }

    public void uploadBucketClient(String nomePasta, String nomeArq) {
        String key = String.format("%s/%s", nomePasta, nomeArq);
        try {
            s3.putObject(
                    PutObjectRequest.builder()
                            .bucket("s3-client-infomotion-1")
                            .key(key)
                            .contentType("text/csv")
                            .build(),
                    RequestBody.fromFile(Path.of(nomeArq))
            );

            System.out.println("Upload concluído para CLIENT: " + nomeArq);
        }
        catch (Exception e) {
            System.err.println("Erro ao fazer upload para CLIENT " + nomeArq + ": " + e.getMessage());
            throw new RuntimeException("Falha no upload para Client", e);
        }
    }

    public void uploadDiscoBucket(String nomeArq) {
        String key = String.format("tratamentoMiralha/disco/%s", nomeArq);
        try {
            s3.putObject(
                    PutObjectRequest.builder()
                            .bucket("s3-client-infomotion-1")
                            .key(key)
                            .contentType("text/csv")
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

    public void uploadTemperaturaBucket(String nomeArq) {
        String key = String.format("tratamentoMiralha/temperatura/%s", nomeArq);
        try {
            s3.putObject(
                    PutObjectRequest.builder()
                            .bucket("s3-client-infomotion-1")
                            .key(key)
                            .contentType("text/csv")
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

    public void downloadTemperaturaBucket(String nomeArq) {
        String key = nomeArq;

        try {
            s3.getObject(
                    GetObjectRequest.builder()
                            .bucket("s3-client-infomotion-1")
                            .key(key)
                            .build(),
                    Paths.get(nomeArq)
            );
        } catch (software.amazon.awssdk.services.s3.model.NoSuchKeyException e) {
            System.out.println("Aviso: Arquivo '" + nomeArq + "' não encontrado no S3 Trusted. Será criado um novo.");
        } catch (Exception e) {
            System.err.println("Erro inesperado ao baixar '" + nomeArq + "' do Trusted: " + e.getMessage());
            throw new RuntimeException("Falha no download do Trusted", e);
        }
    }

    public void uploadProcessosBucket(String nomeArq) {
        String key = String.format("tratamentoMiralha/processos/%s", nomeArq);
        try {
            s3.putObject(
                    PutObjectRequest.builder()
                            .bucket("s3-client-infomotion-1")
                            .key(key)
                            .contentType("text/csv")
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

    public void downloadCardsServidoresBucket(String nomeArq) {
        String key = String.format("status_servidores/%s", nomeArq);

        try {
            s3.getObject(
                    GetObjectRequest.builder()
                            .bucket("s3-client-infomotion-1")
                            .key(key)
                            .build(),
                    Paths.get(nomeArq)
            );
        } catch (software.amazon.awssdk.services.s3.model.NoSuchKeyException e) {
            System.out.println("Arquivo de status não encontrado. Será criado um novo.");
        } catch (Exception e) {
            System.err.println("Erro ao baixar status: " + e.getMessage());
        }
    }

    public void uploadCardsServidoresBucket(String nomeArq) {
        String key = String.format("status_servidores/%s", nomeArq);

        try {
            s3.putObject(
                    PutObjectRequest.builder()
                            .bucket("s3-client-infomotion-1")
                            .key(key)
                            .contentType("application/json")
                            .build(),
                    RequestBody.fromFile(Path.of(nomeArq))
            );

            System.out.println("Upload concluído: " + key);
            deleteCsvLocal(nomeArq);

        } catch (Exception e) {
            System.err.println("Erro ao fazer upload de status: " + e.getMessage());
        }
    }

    public void deleteCsvLocal(String nomeArq){
        try {
            Path caminho = Path.of(nomeArq);
            java.nio.file.Files.deleteIfExists(caminho);
            System.out.println("  → Arquivo local deletado: " + nomeArq);
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