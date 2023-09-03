package proyecto1;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileSplitter {
    public static void splitFile(String sourceFilePath, String outputDirectory, long chunkSize) {
        try {
            File inputFile = new File(sourceFilePath);
            FileInputStream inputStream = new FileInputStream(inputFile);
            byte[] buffer = new byte[(int) chunkSize];
            int bytesRead;
            int chunkNumber = 1;

            StringBuilder textBuilder = new StringBuilder();

            while ((bytesRead = inputStream.read(buffer)) > 0) {

                String chunkText = new String(buffer, 0, bytesRead);
                chunkText = chunkText.replaceAll("[^a-zA-Z\\s]", " ").replaceAll("\\d", " ")
                        .replaceAll("[\\r\\n]+", " ").toLowerCase();

                textBuilder.append(chunkText);

                String chunkFilePath = outputDirectory + "chunk" + chunkNumber + ".txt";
                FileOutputStream outputStream = new FileOutputStream(chunkFilePath);
                outputStream.write(textBuilder.toString().getBytes());
                outputStream.close();

                textBuilder.setLength(0);
                chunkNumber++;

            }

            inputStream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
