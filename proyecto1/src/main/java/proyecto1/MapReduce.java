package proyecto1;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MapReduce {
    public static void main(String[] args) {

        int coordinatorfail = -1;
        int nodetoFail = -1;
        int reducerFail = -1;

        // Creacion de chunks del archivo de 1GB
        String inputText = "proyecto1/src/main/java/proyecto1/bigbible.txt";
        String outputDirectory = "proyecto1/src/main/java/proyecto1/";
        int chunkSize = 17 * 1024 * 1024;
        FileSplitter.splitFile(inputText, outputDirectory, chunkSize);

        /*** MAPPER ***/

        /* */
        // Crear nodos mapper
        ArrayList<MapClass> mapperArray = new ArrayList<>();

        // Crear pools de hilos para nodos mapper
        int numberThreadsMapper = 6;
        ExecutorService mapExecutor = Executors.newFixedThreadPool(numberThreadsMapper);

        // Utilizar CountDownLatch para esperar a que todos los hilos terminen
        CountDownLatch latchMapper = new CountDownLatch(numberThreadsMapper);

        // Arreglo de tareas hechas submit de mapper
        ArrayList<Future<?>> futuresMapperList = new ArrayList<>();

        for (int nChunk = 0; nChunk < 60; nChunk++) {

            if (nChunk < numberThreadsMapper) {
                mapperArray.add(new MapClass(nChunk + 1, (int) 60 / numberThreadsMapper, latchMapper));
            }
            String chunkFilePath = outputDirectory + "chunk" + (nChunk + 1) + ".txt";
            int nMapper = nChunk % numberThreadsMapper;
            mapperArray.get(nMapper).setPathToRead(chunkFilePath);
            // mapExecutor.execute(mapperArray.get(nMapper));

            futuresMapperList.add(mapExecutor.submit(mapperArray.get(nMapper)));
        }

        if (nodetoFail != -1) {
            System.out.println("Fallo en el nodo numero: " + (nodetoFail + 1));
            // Se elimina de la lista
            mapperArray.get(nodetoFail).end();
            mapperArray.remove(nodetoFail);
            // Cancelacion de todos los hilos pertenecientes a ese nodo y eliminacion de
            // tareas
            int newNMapper = 0;
            for (int i = nodetoFail; i < 60; i += 6) {
                System.out.println("Fallo, cancelando mapeo de chunk " + (i + 1) +
                        " correspondiente al nodo " + (nodetoFail + 1));
                futuresMapperList.get(i).cancel(true);
                newNMapper++;
                if (newNMapper > 4) {
                    newNMapper = newNMapper % 5;
                }
                String chunkNewFilePath = outputDirectory + "chunk" + (i + 1) + ".txt";
                mapperArray.get(newNMapper).setPathToRead(chunkNewFilePath);
                mapperArray.get(newNMapper).setChunkNumber(12);
                futuresMapperList.set(i, mapExecutor.submit(mapperArray.get(newNMapper)));
            }

            String filename = outputDirectory + "outputMapper" + (nodetoFail + 1) + ".txt";
            try {
                Files.deleteIfExists(Paths.get(filename));
                System.out.println("Se ha eliminado el archivo " + filename);
            } catch (IOException e) {
                System.err.println("Error al eliminar el archivo, no existe." + filename +
                        ": " + e.getMessage());
            }
        }

        if (coordinatorfail != -1) {

            futuresMapperList.stream().forEach(x -> x.cancel(true));

            mapExecutor.shutdownNow();

            System.out.println("Falla en el nodo coordinador, se termina de ejecutar todo");

        } else {

            try {
                // Esperar hasta que todos los hilos mapper hayan terminado
                latchMapper.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Apagar el ExecutorService después de que todos los trabajos mapper estén
            // completos
            mapExecutor.shutdownNow();

            System.out.println("Todos los hilos mapper han terminado");

            /*** REDUCER ***/

            // Crear nodos reducer
            ArrayList<ReduceClass> reducerArray = new ArrayList<>();

            // Crear pools de hilos para nodos reduce
            int numberThreadsReducer = 3;
            ExecutorService reducerExecutor = Executors.newFixedThreadPool(numberThreadsReducer);

            // Utilizar CountDownLatch para esperar a que todos los hilos terminen
            CountDownLatch latchReducer = new CountDownLatch(numberThreadsReducer);

            // Arreglo de tareas hechas submit de reducer
            ArrayList<Future<?>> futuresReducerList = new ArrayList<>();

            for (int nOutputM = 0; nOutputM < 6; nOutputM++) {

                if (nOutputM < numberThreadsReducer) {
                    if (nOutputM == nodetoFail) {
                        reducerArray.add(new ReduceClass(nOutputM + 1, 1, latchReducer));
                    } else {
                        reducerArray.add(new ReduceClass(nOutputM + 1, (int) 6 / numberThreadsReducer, latchReducer));
                    }
                }
                if (nOutputM == nodetoFail) {
                    System.out.println("");
                } else {
                    String outputMFilePath = outputDirectory + "outputMapper" + (nOutputM + 1) + ".txt";
                    int nReducer = nOutputM % numberThreadsReducer;
                    reducerArray.get(nReducer).setPathToRead(outputMFilePath);
                    futuresReducerList.add(reducerExecutor.submit(reducerArray.get(nReducer)));
                }

            }

            if (reducerFail != -1) {
                System.out.println("Fallo en el nodo numero: " + (reducerFail + 1) + "se reinicia el nodo");
                // Se resetea nodo Reducer
                reducerArray.get(reducerFail).reset();

                // Cancelacion de todos los tasks pertenecientes a ese nodo
                int newNReducer = 0;
                for (int i = reducerFail; i < 6; i += 3) {
                    futuresReducerList.get(i).cancel(true);
                    String chunkNewFilePath = outputDirectory + "ouputMapper" + (i + 1) + ".txt";
                    reducerArray.get(newNReducer).setPathToRead(chunkNewFilePath);
                    futuresReducerList.set(i, reducerExecutor.submit(reducerArray.get(newNReducer)));
                }
            }

            try {
                // Esperar hasta que todos los hilos mapper hayan terminado
                latchReducer.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Apagar el ExecutorService después de que todos los trabajos mapper estén
            // completos
            reducerExecutor.shutdownNow();

            System.out.println("Todos los hilos reducer han terminado");

            /*** FINAL REDUCER ***/
            // Crear pools de hilos para nodos reduce
            // Utilizar CountDownLatch para esperar a que todos los hilos terminen
            CountDownLatch finalReducerLatch = new CountDownLatch(1);

            ExecutorService finalReducerExecutor = Executors.newFixedThreadPool(1);
            ReduceClass finalReducer = new ReduceClass(0, 3, finalReducerLatch);

            for (int nOutputR = 0; nOutputR < 3; nOutputR++) {
                String outputRFilePath = outputDirectory + "outputReducer" + (nOutputR + 1) +
                        ".txt";
                finalReducer.setPathToRead(outputRFilePath);
                finalReducerExecutor.execute(finalReducer);

                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            try {
                // Esperar hasta que el hilo reduce haya terminado
                finalReducerLatch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            finalReducerExecutor.shutdownNow();
        }
    }
}
