package org.example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class TrustedParaCliente {

    private final AwsConnection awsConnection;

    // Construtor: Inicializa a classe com a conexão S3
    public TrustedParaCliente(AwsConnection awsConnection) {
        this.awsConnection = awsConnection;
    }

    /**
     * Ponto de entrada da ETL Trusted -> Cliente (o método que será chamado pelo Lambda Handler).
     * @param nomeDoCsvNoBucket Nome do arquivo a ser baixado do bucket Trusted.
     */
    public void rodarProcesso(String nomeDoCsvNoBucket) {
        System.out.println("--- Iniciando Pipeline: Trusted -> Cliente ---");

        try {
            // 1. BAIXAR O ARQUIVO DO TRUSTED PARA LOCAL
            awsConnection.downloadBucketTrusted(nomeDoCsvNoBucket);
            System.out.println("Arquivo baixado do Trusted para local: " + nomeDoCsvNoBucket);

            // 2. TRATAR O ARQUIVO LOCAL
            Path localFilePath = Path.of(nomeDoCsvNoBucket);
            aplicarTransformacoesFinais(localFilePath);
            System.out.println("Transformação final (nulos) aplicada em: " + nomeDoCsvNoBucket);

            // 3. FAZER UPLOAD PARA O BUCKET CLIENTE
            awsConnection.uploadBucketClient(nomeDoCsvNoBucket);
            System.out.println("Upload concluído para o Cliente: " + nomeDoCsvNoBucket);

        } catch (Exception e) {
            System.err.println("Erro FATAL na ETL TrustedParaCliente para " + nomeDoCsvNoBucket + ": " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Garante que o arquivo local temporário seja removido, independente de sucesso ou falha
            awsConnection.deleteCsvLocal(nomeDoCsvNoBucket);
        }
    }

    /**
     * Módulo de Transformação Final: Substitui valores vazios/nulos por "N/A".
     * (O código que você enviou, usando 'for' loop)
     */
    private void aplicarTransformacoesFinais(Path caminhoArquivoLocal) throws IOException {
        System.out.println("Aplicando tratamento de NULOS...");

        // 1. LER TODAS AS LINHAS DO ARQUIVO LOCAL
        List<String> todasAsLinhas = Files.readAllLines(caminhoArquivoLocal);

        if (todasAsLinhas.isEmpty()) {
            System.out.println("Arquivo vazio. Nenhuma transformação aplicada.");
            return;
        }

        List<String> linhasProcessadas = new ArrayList<>();

        for (String linhaOriginal : todasAsLinhas) {
            String linhaTratada = linhaOriginal;

            // Tratamento de ,,
            linhaTratada = linhaTratada.replace(",,", ",N/A,");

            // Tratamento no FINAL
            if (linhaTratada.endsWith(",")) {
                linhaTratada += "N/A";
            }

            // Tratamento no INÍCIO
            if (linhaTratada.startsWith(",")) {
                linhaTratada = "N/A" + linhaTratada;
            }

            // Tratamento de ""
            linhaTratada = linhaTratada.replace("\"\"", "\"N/A\"");

            linhasProcessadas.add(linhaTratada);
        }

        // 3. ESCREVER AS LINHAS PROCESSADAS SOBRE O ARQUIVO LOCAL
        Files.write(caminhoArquivoLocal, linhasProcessadas);

        System.out.println("Tratamento de nulos concluído. Arquivo local sobrescrito.");
    }
}