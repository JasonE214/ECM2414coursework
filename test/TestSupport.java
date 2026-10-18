import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

//helper class, some general functions to make test easier to make
final class TestSupport {

    private TestSupport() {
    }

//make cards
    static List<Card> cards(int... values) {
        List<Card> cards = new ArrayList<>();
        for (int value : values) {
            cards.add(new Card(value));
        }
        return cards;
    }

//turn card back to number, used for checking if both card have same value
//as compare 2 object won't pass the test
    static List<Integer> values(List<Card> cards) {
        List<Integer> values = new ArrayList<>();
        for (Card card : cards) {
            values.add(card.getValue());
        }
        return values;
    }   

//make deck with deck number and cards
    static CardDeck deck(int number, int... values) {
        CardDeck deck = new CardDeck(number);
        for (Card card : cards(values)) {
            deck.addToBottom(card);
        }
        return deck;
    }

//makeing a pack and give it to the player
    static Pack pack(int players, int... values) throws IllegalArgumentException {
        List<Integer> list = new ArrayList<>();
        for (int value : values) {
            list.add(value);
        }
        return pack(players, list);
    }

//overload method for list inputs
    static Pack pack(int players, List<Integer> values) {
        try {
            Path file = Files.createTempFile("pack", ".txt");
            file.toFile().deleteOnExit();
            Files.write(file, rows(values), StandardCharsets.UTF_8);
            return Pack.Load_decks(players, file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

//Turn number to string so it use to write into files
    static List<String> rows(List<Integer> values) {
        List<String> rows = new ArrayList<>();
        for (int value : values) {
            rows.add(Integer.toString(value));
        }
        return rows;
    }

//write a file and put it into a folder
    static Path writePackFile(Path folder, String name, List<Integer> values) throws IOException {
        return Files.write(folder.resolve(name), rows(values), StandardCharsets.UTF_8);
    }


//don't need to realise something is broken and press control + C everytime
//java will still close normally and don't wait for a stuck thread to keep running 
    static ExecutorService daemonPool() {
        return Executors.newCachedThreadPool(task -> {
            Thread thread = new Thread(task);
            thread.setDaemon(true);
            return thread;
        });
    }

//Make sure the pack is winable, uses seed to make the filler cards to make sure 
//problem can be trace down everytime a test fails 

    static List<Integer> winnableValues(int players, long seed) { 

        Random random = new Random(seed); 

        List<Integer> values = new ArrayList<>(); 

        for (int value = 1; value <= players; value++) { 

            values.addAll(Collections.nCopies(4, value)); 

        } 

        while (values.size() < Pack.cards_per_deck * players) { 

            values.add(random.nextInt(players + 4)); // 0 .. n+3 

        } 

        Collections.shuffle(values, random); 

        return values; 

    }

//fix deck for 1 player game 

    static List<Integer> onePlayerPackValues() { 

        return List.of(1, 2, 3, 4, 1, 1, 1, 5); 

    } 

//make a place for the testing to write stuff in 

    static PrintWriter quietLog() { 

        return new PrintWriter(new StringWriter()); 

    } 

//mute the console from printing messages like player win, just need to know if the test is working or not 

    static PrintStream quietConsole() { 

        return new PrintStream(new ByteArrayOutputStream(), true); 

    } 

//the testting player input 

    static BufferedReader input(String... lines) { 

        return new BufferedReader(new StringReader(String.join("\n", lines) + "\n")); 

    } 

//split test into list of lines \\R so it can handle both linux and window 

    static List<String> lines(String text) { 

        if (text.isEmpty()) { 

            return List.of(); 

        } 

        return Arrays.asList(text.split("\\R")); 

    } 

//count how many lines have the keyword, e.g. check if there is only 1 win to make sure only 1 player won 

    static long count(List<String> lines, String fragment) { 

        return lines.stream().filter(line -> line.contains(fragment)).count(); 

    }

//even thou I mute the console, but still need someway to remember the input 

    static final class RecordingConsole { 

        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream(); 

        final PrintStream stream = new PrintStream(bytes, true, StandardCharsets.UTF_8); 

 

        List<String> lines() { 

            return TestSupport.lines(new String(bytes.toByteArray(), StandardCharsets.UTF_8)); 

        } 

    }
}