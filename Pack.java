import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/* Reads the file from a given path and checks if the information inside fits the rules.
If yes, return a the pack contains all the cards with their value
if not, return an error */

public final class Pack{
    public static final int cards_per_deck = 8;
    private final List<Card> cards;

    private Pack(List<Card> cards) {
        this.cards = List.copyOf(cards);
    }
    public static Pack Load_decks(int players_nums, Path file) throws IllegalArgumentException
    {   
        /* Giving detail checks so it is more easier to solve error. */
        if (players_nums < 1) throw new IllegalArgumentException("We need at least 1 player to start the game");
        if (Files.isDirectory(file)) {
            throw new IllegalArgumentException( file + " is not a file.");
        }
        if (!Files.isRegularFile(file)) {
            throw new IllegalArgumentException("There is nothing at '" + file + "'.");
        }
        List<String> rows;
        try {
            rows = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalArgumentException("'" + file + "' couldn't read file.");
        }
        
        //finding the index of last row with an actual value
        int lastRow = rows.size() - 1;
        if (lastRow >= 0){
            while (rows.get(lastRow).trim().isEmpty()) {
                lastRow--;
        }
        }
        List<Card> cards = new ArrayList<>(lastRow);
        for (int i = 0; i <= lastRow; i++) {
            cards.add(new Card(Rowcheck(rows.get(i), i + 1))); // make a new list without the spaces
        }
        int required_cards =  cards_per_deck * players_nums;
        if (cards.size() != required_cards) {
            throw new IllegalArgumentException("A pack for " + players_nums + " player"
            + " must have exactly " + required_cards + " rows of card values, but this one only have " 
            + cards.size() + " amount of cards.");
        }
        return new Pack(cards);
    }
    private static int Rowcheck(String row, int rowNumber) throws IllegalArgumentException 
    {
        String text = row.trim();
        // Check is the cardd value valid
        if (text.isEmpty()) {
            throw new IllegalArgumentException("row " + rowNumber + ": have nothing.");
        }
        if (text.matches("-[0-9]+") || !text.matches("[0-9]+")) {
            throw new IllegalArgumentException("row " + rowNumber + ": Invalid number," + 
            " card values must be positive whole number.");
        }

        // Incase the number in the file is too large for the Int to handle
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("row " + rowNumber + ": Number is too large.");
        }
    }

    public List<Card> getCards() {
        return cards;
    }
    public int size() {
        return cards.size();
    }
}