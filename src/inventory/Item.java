package inventory;

/** Validated inventory value.
 * @author OEL Team
 * @param id database identity (zero for a new item)
 * @param name nonblank item name
 * @param quantity nonnegative whole quantity
 * @param status selected stock status
 */
public record Item(long id, String name, int quantity, String status) {
    /** Allowed labels shared by the form and validation. */
    public static final java.util.List<String> STATUSES = java.util.List.of("In Stock", "Low Stock", "Out of Stock");
    /** Validates and normalizes an item.
     * @param id database identity
     * @param name item name
     * @param quantity whole quantity
     * @param status stock label
     */
    public Item {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Enter an item name.");
        name = name.strip();
        if (name.length() > 200) throw new IllegalArgumentException("Name must be at most 200 characters.");
        if (quantity < 0) throw new IllegalArgumentException("Quantity cannot be negative.");
        if (!STATUSES.contains(status)) throw new IllegalArgumentException("Select a valid stock status.");
    }
    /** Parses form input.
     * @param id database identity
     * @param name item name
     * @param quantity quantity text
     * @param status stock label
     * @return validated item
     */
    public static Item parse(long id, String name, String quantity, String status) {
        try { return new Item(id, name, Integer.parseInt(quantity.strip()), status); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Quantity must be a whole number from 0 to 2147483647."); }
    }
}
