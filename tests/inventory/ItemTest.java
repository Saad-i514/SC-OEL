package inventory;

/** Dependency-free validation checks.
 * @author OEL Team */
public final class ItemTest {
    private ItemTest() {}
    /** Executes checks.
     * @param args unused */
    public static void main(String[] args) {
        assert Item.parse(0, " Tea ", " 12 ", "In Stock").name().equals("Tea");
        assert Item.parse(0, "Tea", "0", "Out of Stock").quantity() == 0;
        for (String quantity : new String[]{"-1", "abc", "1.5", "", "2147483648"}) {
            reject(() -> Item.parse(0, "Tea", quantity, "In Stock"));
        }
        reject(() -> Item.parse(0, " ", "2", "In Stock"));
        reject(() -> new Item(0, "Tea", 1, "Invalid"));
        reject(() -> new Item(0, "x".repeat(201), 1, "Low Stock"));
        System.out.println("PASS: valid input, trimming, zero, blank name, negative/non-numeric/fractional/overflow quantity, status and name limit");
    }
    private static void reject(Runnable action) {
        try { action.run(); throw new AssertionError("Expected validation failure"); }
        catch (IllegalArgumentException expected) { }
    }
}
