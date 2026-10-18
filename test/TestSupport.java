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

}