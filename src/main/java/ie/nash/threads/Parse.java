package ie.nash.threads;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

        List<String> threads = new ArrayList<>();

        URI uri = ClassLoader.getSystemResource(threadDumpFile)
                .toURI();

        List<String> fileLines = Files.readAllLines(Paths.get(uri))
                .stream()
                .map(s -> s.replaceAll("<0x.*>", "<0xObjectID>"))
                .collect(Collectors.toList());

        List<Integer> threadIndexes = IntStream.range(0, fileLines.size())
                .filter(i -> fileLines.get(i).contains("Thread.State") && !fileLines.get(i + 1).isEmpty())
                .boxed()
                .collect(Collectors.toList());


        for (Integer threadIndex : threadIndexes) {
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

        Map<String, Long> counted = threads.stream()
                .filter(s-> !s.contains(PACKAGE))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        Path  path = Paths.get(threadDumpFile+".stats");

        Files.write(path, () -> counted.entrySet().stream()
                .<CharSequence>map(e -> "Count " + e.getValue() + ":\n" + e.getKey())
                .iterator());
    }
}
