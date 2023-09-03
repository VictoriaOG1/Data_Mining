package proyecto1;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class ReduceClass implements Runnable {
    private int id;
    private int outputNumber;
    private int counter;
    private String filePath;
    private Map<String, Integer> map;
    private Map<String, Integer> sorted;
    private CountDownLatch latch;

    public ReduceClass(int id, int outputNumber, CountDownLatch latch) {
        this.latch = latch;
        this.id = id;
        this.counter = 0;
        this.outputNumber = outputNumber;
        this.map = new HashMap<>();
    }

    public void setPathToRead(String filePath) {
        this.filePath = filePath;
    }

    public String getPathToRead() {
        return this.filePath;
    }

    public void setFilePath(String path) {
        synchronized (this) {
            this.filePath = path;
        }
    }

    public Map<String, Integer> getSortedMap() {
        return this.sorted;
    }

    public void shuffle() {
        sorted = map.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue, LinkedHashMap::new));

    }

    public synchronized void readReduce() {
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(this.filePath));) {
            String line;
            String key = "0";
            String value = "0";

            while ((line = bufferedReader.readLine()) != null) {
                String[] pair = line.split(":");
                if (pair.length == 2) {
                    key = pair[0].trim();
                    value = pair[1].trim();
                }

                if (!key.equals("0") && !value.equals("0")) {
                    if (map.containsKey(key)) {
                        int mapValue = map.get(key);
                        int newValue = mapValue + Integer.parseInt(value);
                        map.put(key, newValue);
                    } else {
                        map.put(key, Integer.parseInt(value));
                    }
                }
            }
            bufferedReader.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void write() {
        String path;

        if (id == 0) {
            path = "proyecto1/src/main/java/proyecto1/finalReducer.txt";
        } else {
            path = "proyecto1/src/main/java/proyecto1/outputReducer" + id + ".txt";
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path, true))) {
            if (id != 0) {
                Set<Entry<String, Integer>> entries = map.entrySet();

                for (Entry<String, Integer> entry : entries) {
                    String linea = entry.getKey() + ": " + entry.getValue();
                    writer.write(linea);
                    writer.newLine();
                }
            } else {
                shuffle();
                Set<Entry<String, Integer>> entries = sorted.entrySet();

                for (Entry<String, Integer> entry : entries) {
                    String linea = entry.getKey() + ": " + entry.getValue();
                    writer.write(linea);
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            System.err.println("An error occurred while writing to the file: " + e.getMessage());
        }
    }

    public synchronized void run() {
        try {
            System.out.println("Reducer con id " + id + " empezo");
            readReduce();
            System.out.println("Reducer con id " + id + " termino");
            counter++;
            if (counter == outputNumber) {
                write();
                latch.countDown();
            }

        } catch (Exception e) {
            System.out.println(
                    "Error ejecutando el recucer en archivo " + filePath + "finalizando reducer de esta seccion.");
            latch.countDown();
        }
    }
}
