package proyecto1;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

public class MapClass implements Runnable {
    private int id;
    private int chunkNumber;
    private int counter;
    private String filePath;
    // private ArrayList<String> textInMemory;
    private Multimap<String, Integer> multimap;
    private CountDownLatch latch;

    public MapClass(int id, int chunkNumber, CountDownLatch latch) {
        this.latch = latch;
        this.id = id;
        this.counter = 0;
        this.chunkNumber = chunkNumber;
    }

    public void setPathToRead(String filePath) {
        this.filePath = filePath;
    }

    public String getPathToRead() {
        return this.filePath;
    }

    public Multimap<String, Integer> getMultimap() {
        return this.multimap;
    }

    public synchronized void write() {
        String path = "proyecto1/src/main/java/proyecto1/outputMapper" + id + ".txt";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path, true))) {
            // Iterate through the Multimap entries and write them to the file
            for (String key : multimap.keySet()) {
                for (Integer value : multimap.get(key)) {
                    writer.write(key + ": " + value);
                    writer.newLine(); // Add a newline between entries
                }
            }
        } catch (IOException e) {
            System.err.println("An error occurred while writing to the file: " + e.getMessage());
        }
    }

    public synchronized void run() {
        try {
            System.out.println("Mapper con id " + id + " empezo");
            multimap = ArrayListMultimap.create();
            // textInMemory = new ArrayList<>();
            File file = new File(filePath);
            Scanner input = new Scanner(file);

            while (input.hasNext()) {
                String word = input.next();
                // textInMemory.add(word);
                multimap.put(word, 1);
            }

            input.close();
            write();
            System.out.println("Mapper con id " + id + " termino");
            counter++;
            if (counter == chunkNumber) {
                latch.countDown();
            }

        } catch (Exception e) {
            System.out.println(
                    "Error ejecutando el mapping en archivo " + filePath + "finalizando mapping de esta seccion.");
            latch.countDown();
        }
    }
}
