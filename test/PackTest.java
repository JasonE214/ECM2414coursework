import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class PackTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private Path file(String name, String content) throws IOException {
        return Files.write(temp.getRoot().toPath().resolve(name), content.getBytes(StandardCharsets.UTF_8));
    }

    private static String rowsOf(List<String> rows) {
        return String.join("\n", rows) + "\n";
    }

//check if the test read the file in row order
    @Test
    public void read_in_order() throws Exception {
        List<Integer> values = new ArrayList<>();
        for (int i = 1; i <= 16; i++) {
            values.add(i);
        }
        Path file = TestSupport.writePackFile(temp.getRoot().toPath(), "two.txt", values);

        Pack pack = Pack.Load_decks(2, file);

        assertEquals(16, pack.size());
        assertEquals(values, TestSupport.values(pack.getCards()));
    }

//Check if it takes all non negative numbers
    @Test
    public void accept_non_negatives() throws Exception {
        Path file = file("big.txt", rowsOf(List.of("0", "67", "5000", "2147483647", "0", "1", "7", "9")));
        assertEquals(List.of(0, 67, 5000, Integer.MAX_VALUE, 0, 1, 7, 9),
                TestSupport.values(Pack.Load_decks(1, file).getCards()));
    }

//Check if files with tabs and create new line or empty rows in end will be accepted
    @Test
    public void accept_diff_fomate() throws Exception {
        Path file = file("windows.txt", " 1\r\n2 \r\n\t3\r\n4\r\n5\r\n6\r\n7\r\n8\r\n\r\n\r\n");
        assertEquals(List.of(1, 2, 3, 4, 5, 6, 7, 8), TestSupport.values(Pack.Load_decks(1, file).getCards()));
    }

//any packs with content that is not equal to 8n row will be rejected
    @Test
    public void reject_incorrect_pack() throws Exception {
        for (int rows : new int[] {0, 1, 15, 17, 24, 32}) {
            Path file = file("rows" + rows + ".txt", rowsOf(Collections.nCopies(rows, "1")));
            IllegalArgumentException e = assertThrows("" + rows + " rows",
                    IllegalArgumentException.class, () -> Pack.Load_decks(2, file));
            assertTrue(e.getMessage(), e.getMessage().contains("exactly 16 rows"));
        }
    }

//See if test will rejec any not ok row and correct message
    @Test
    public void reject_exceptions() throws Exception {
        String[] badRows = {"-1", "abc", "3.5", "1 2", "1,2", "0x10", "", "   ", "99999999999"};
        for (int i = 0; i < badRows.length; i++) {
            List<String> rows = new ArrayList<>(List.of("1", "2", "3", "4", "5", "6", "7", "8"));
            rows.set(3, badRows[i]); // row 4, so blank rows are not at the end
            Path file = file("bad" + i + ".txt", rowsOf(rows));
            IllegalArgumentException e = assertThrows("row '" + badRows[i] + "'",
                    IllegalArgumentException.class, () -> Pack.Load_decks(1, file));
            assertTrue("the message names the row: " + e.getMessage(), e.getMessage().startsWith("row 4:"));
        }
    }

//see if it rejects negatives and the correct message
    @Test
    public void reject_negative() throws Exception {
        Path file = file("negative.txt", rowsOf(List.of("1", "2", "-3", "4", "5", "6", "7", "8")));
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Pack.Load_decks(1, file));
        assertEquals("row 3: Invalid number, card values must be larger than -1.", e.getMessage());
    }

//check last_row actually works to on compelety blank files or empty files 
//and reject as invalid pack
//instead of crashing normally
    @Test
    public void reject_blank() throws Exception {
        for (String content : new String[] {"", "\n\n", "  \n\t\n \n"}) {
            Path file = file("blank" + content.length() + ".txt", content);
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Pack.Load_decks(1, file));
            assertTrue(e.getMessage(), e.getMessage().contains("must have exactly 8 rows"));
        }
    }

//reject non exist file
    @Test
    public void rejects_empty() {
        Path missing = temp.getRoot().toPath().resolve("missing.txt");
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Pack.Load_decks(1, missing));
        assertTrue(e.getMessage(), e.getMessage().contains("missing.txt"));
    }

//reject file 
    @Test
    public void rejects_folder() {
        assertThrows(IllegalArgumentException.class, () -> Pack.Load_decks(1, temp.getRoot().toPath()));
    }

// reject any file with no text content
    @Test
    public void rejects_Ntxt_file() throws Exception {
        Path binary = Files.write(temp.getRoot().toPath().resolve("picture.png"),
                new byte[] {(byte) 0x89, 'P', 'N', 'G', (byte) 0xFF, (byte) 0xFE, 0, 1});
        assertThrows(IllegalArgumentException.class, () -> Pack.Load_decks(1, binary));
    }

}
