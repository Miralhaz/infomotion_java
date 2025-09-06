package org.example;

import java.util.*;

public class Main {
    public static void main(String[] args) {
        List<RegistroLogs> logs = new ArrayList<>();
        logs.add(new RegistroLogs("Verônica", "111.222.333-54", 77.3, 56.7, 81.2, new Date()));
        logs.add(new RegistroLogs("Gabriel", "888.999.000-21", 95.0, 75.0, 110.0, new Date()));
        logs.add(new RegistroLogs("Verônica", "111.222.333-54", 84.5, 58.7, 89.2, new Date()));
        logs.add(new RegistroLogs("Verônica", "222.333.444-65", 77.3, 56.7, 99.2, new Date()));
        logs.add(new RegistroLogs("Verônica", "333.444.555-76", 90.1, 70.2, 80.5, new Date()));
        logs.add(new RegistroLogs("Gabriel", "666.777.888-09", 81.9, 60.2, 102.1, new Date()));
        logs.add(new RegistroLogs("Gabriel", "999.000.111-32", 88.2, 64.8, 105.4, new Date()));
        logs.add(new RegistroLogs("Verônica", "444.555.666-87", 65.8, 45.3, 95.7, new Date()));
        logs.add(new RegistroLogs("Verônica", "555.666.777-98", 72.4, 30.9, 88.6, new Date()));
        logs.add(new RegistroLogs("Gabriel", "777.888.999-10", 69.5, 20.7, 79.3, new Date()));
        logs.add(new RegistroLogs("Gabriel", "000.111.222-43", 79.7, 40.6, 91.8, new Date()));

        System.out.println(logs);
    }
}