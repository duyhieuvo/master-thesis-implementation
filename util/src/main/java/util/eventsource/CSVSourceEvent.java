package util.eventsource;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;


public class CSVSourceEvent {
    public static String[] readHeaders(String pathToFile) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(pathToFile))) {
            return br.readLine().split(",");
        }
    }


    public static void generateEventFromCSV(String pathToFile, EventsPublisher eventsPublisher, int startPosition){
        try {
            String[] headers = readHeaders((pathToFile));
            Stream<String> stream = Files.lines(Paths.get(pathToFile));
            stream  .sequential()
                    .skip(startPosition) // skip headers and the previously read events
                    .map(line -> line.split(","))
                    .map(data -> IntStream.range(0, data.length)
                            .boxed()
                            .collect(Collectors.toMap(i -> headers[i], i -> data[i])))
                    .forEachOrdered(event -> eventsPublisher.publishEvents(event));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
