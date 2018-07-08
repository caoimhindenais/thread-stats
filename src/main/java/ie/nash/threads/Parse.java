package ie.nash.threads;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class Parse implements CommandLineRunner {

    @Value("${thread.depth}")
    private int THREAD_DEPTH;

    @Value("${package}")
    private String PACKAGE;

    @Value("${thread.dump.file}")
    private String threadDumpFile;


    private final Logger LOGGER = LoggerFactory.getLogger(Parse.class);

    @Override
    public void run(String... args) throws Exception {
        LOGGER.info("");



        URI uri = ClassLoader.getSystemResource(threadDumpFile)
                .toURI();

        List<String> fileLines = Files.readAllLines(Paths.get(uri))
                .stream()
                .map(s -> s.replaceAll("<0x.*>", "<0xObjectID>"))
                .collect(Collectors.toList());

        List<Integer> threadStartIndex = IntStream.range(0, fileLines.size())
                .filter(i -> fileLines.get(i).contains("Thread.State") && !fileLines.get(i + 1).isEmpty())
                .boxed()
                .collect(Collectors.toList());


        Map<String, Integer> threadStatusCount = new HashMap<>();

        List<String> threads = new ArrayList<>();

        for (Integer threadIndex : threadStartIndex) {

            String threadStatus =fileLines.get(threadIndex);

            if(threadStatusCount.containsKey(threadStatus)) {
                Integer count =  threadStatusCount.get(threadStatus);
                threadStatusCount.put(threadStatus, ++count);
            } else {
                threadStatusCount.put(threadStatus, 1);
            }


            StringBuilder thread = new StringBuilder();
            for (int i = 0; i < THREAD_DEPTH; i++) {
                String line = fileLines.get(threadIndex + i);
                if (!line.isEmpty()) {
                    thread.append(line).append("\n");
                } else {
                    break;
                }
            }
            threads.add(thread.toString());
        }

        Map<String, Long> groupByThread = threads.stream()
                //.filter(s-> (!(PACKAGE.trim().isEmpty())) && !(s.contains(PACKAGE)))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        Path  path = Paths.get(threadDumpFile+".stats");

        Files.write(path,  () -> threadStatusCount.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .<CharSequence>map(e -> e.getValue() + ":" + e.getKey())
                .iterator(),StandardOpenOption.CREATE);

        Files.write(path, "------------------------------------------------\n".getBytes(), StandardOpenOption.APPEND);

        Files.write(path, () -> groupByThread.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .<CharSequence>map(e -> "Count " + e.getValue() + ":\n" + e.getKey())
                .iterator(),StandardOpenOption.APPEND);
    }
}
