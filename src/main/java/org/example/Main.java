package org.example;
import java.time.LocalDateTime;
import java.util.*;
import java.time.format.DateTimeFormatter;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        LocalDateTime agora = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String dataFormatada = agora.format(formatter);



        List<RegistroLogs> logs = new ArrayList<>();
        logs.add(new RegistroLogs("Verônica", "111.222.333-54", 77.3, 56.7, 81.2, dataFormatada));
        logs.add(new RegistroLogs("Gabriel", "888.999.000-21", 95.0, 75.0, 90.0, dataFormatada));
        logs.add(new RegistroLogs("Verônica", "111.222.333-54", 84.5, 58.7, 89.2, dataFormatada));
        logs.add(new RegistroLogs("Verônica", "222.333.444-65", 77.3, 56.7, 99.2, dataFormatada));
        logs.add(new RegistroLogs("Verônica", "333.444.555-76", 90.1, 70.2, 80.5, dataFormatada));
        logs.add(new RegistroLogs("Gabriel", "666.777.888-09", 81.9, 60.2, 92.1, dataFormatada));
        logs.add(new RegistroLogs("Gabriel", "999.000.111-32", 88.2, 64.8, 95.4, dataFormatada));
        logs.add(new RegistroLogs("Verônica", "444.555.666-87", 65.8, 45.3, 95.7, dataFormatada));
        logs.add(new RegistroLogs("Verônica", "555.666.777-98", 72.4, 30.9, 88.6, dataFormatada));
        logs.add(new RegistroLogs("Gabriel", "777.888.999-10", 69.5, 20.7, 79.3, dataFormatada));
        logs.add(new RegistroLogs("Gabriel", "000.111.222-43", 79.7, 40.6, 91.8, dataFormatada));


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
                        RegistroLogs aux = logs.get(i);
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
                        RegistroLogs aux = logs.get(i);
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
                        RegistroLogs aux = logs.get(i);
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