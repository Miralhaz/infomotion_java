package org.example;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

public class Main {
    public static List<Logs> lerJason() {
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream("logs.json");
        } catch (FileNotFoundException erro) {
            System.out.println("Arquivo nao encontrado");
            System.exit(1);
        }

        LogMapper logMapper = new LogMapper();
        List<Logs> listaLog = new ArrayList<>();
        try {
            listaLog = logMapper.mapearLogs(inputStream);

        } catch (IOException erro) {
            System.out.println("Erro ao mapear o json");
            erro.printStackTrace();
        } finally {
            try {

                inputStream.close();
            } catch (IOException erro) {
                System.out.println("Erro ao fechar o arquivo json");

            }

        }return listaLog;
    }


    public static void main(String[] args) {
        List<Logs> logs =  lerJason();
        Scanner sc = new Scanner(System.in);
// CPU MAIS ALTA
        int indiceMenor;
        boolean continuar = true;
        while (continuar) {

            System.out.println("""
                    Você gostária de ordenar a lista através \
                    
                     1 - CPU\
                    
                     2 - RAM\
                    
                     3 - DISCO\
                    
                     4 - Sair\
                    
                    Digite um número para selecionar:\s""");
            int escolhaUsar = sc.nextInt();

            if (escolhaUsar == 1) {
                for (int i = 0; i < logs.size() - 1; i++) {
                    indiceMenor = i;
                    for (int j = i + 1; j < logs.size(); j++) {
                        if (logs.get(j).getCpu() > logs.get(indiceMenor).getCpu()) {
                            indiceMenor = j;
                        }
                    }
                    if (i != indiceMenor) {
                        Logs aux = logs.get(i);
                        logs.set(i, logs.get(indiceMenor));
                        logs.set(indiceMenor, aux);
                    }

                }
                System.out.println(logs);
            } else if (escolhaUsar == 2) {
                // RAM MAIS ALTA
                for (int i = 0; i < logs.size() - 1; i++) {
                    indiceMenor = i;
                    for (int j = i + 1; j < logs.size(); j++) {
                        if (logs.get(j).getRam() > logs.get(indiceMenor).getRam()) {
                            indiceMenor = j;
                        }
                    }
                    if (i != indiceMenor) {
                        Logs aux = logs.get(i);
                        logs.set(i, logs.get(indiceMenor));
                        logs.set(indiceMenor, aux);
                    }

                }
                System.out.println(logs);
            } else if (escolhaUsar == 3) {
                //DISCO MAIS ALTO
                for (int i = 0; i < logs.size() - 1; i++) {
                    indiceMenor = i;
                    for (int j = i + 1; j < logs.size(); j++) {
                        if (logs.get(j).getDisco() > logs.get(indiceMenor).getDisco()) {
                            indiceMenor = j;
                        }
                    }
                    if (i != indiceMenor) {
                        Logs aux = logs.get(i);
                        logs.set(i, logs.get(indiceMenor));
                        logs.set(indiceMenor, aux);
                    }

                }
                System.out.println(logs);

            } else if(escolhaUsar == 4){
                continuar = false;
                System.out.println("Saindo...");
            }else {
                System.out.println("Escolha inválida");
            }
        }
    }
}