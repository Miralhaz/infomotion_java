//package org.example;
//
//import com.amazonaws.services.lambda.runtime.Context;
//import com.amazonaws.services.lambda.runtime.RequestHandler;
//
//public class ManipuladorLambda implements RequestHandler<String, String> {
//
//    private static final AwsConnection awsConnection = new AwsConnection();
//
//    @Override
//    public String handleRequest(String inputFileName, Context context) {
//        TrustedParaCliente clienteEtl = new TrustedParaCliente(awsConnection);
//        clienteEtl.rodarProcesso(inputFileName);
//        return "ETL concluída para: " + inputFileName;
//    }
//}