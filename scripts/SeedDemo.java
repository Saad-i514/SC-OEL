import inventory.InventoryRepository;
import inventory.Item;
import java.util.HashSet;
import java.util.List;

/** Adds named demonstration products without replacing existing records. */
class SeedDemo {
    public static void main(String[] args) throws Exception {
        var samples = List.of(
            new Item(0, "Espresso Coffee 250g (Demo)", 40, "In Stock"),
            new Item(0, "Green Tea 25 Bags (Demo)", 32, "In Stock"),
            new Item(0, "Basmati Rice 1kg (Demo)", 25, "In Stock"),
            new Item(0, "Whole Wheat Flour 1kg (Demo)", 18, "In Stock"),
            new Item(0, "White Sugar 1kg (Demo)", 22, "In Stock"),
            new Item(0, "Sunflower Oil 1L (Demo)", 16, "In Stock"),
            new Item(0, "Hand Soap 250ml (Demo)", 30, "In Stock"),
            new Item(0, "Fresh Milk 1L (Demo)", 4, "Low Stock"),
            new Item(0, "Chocolate Biscuits (Demo)", 3, "Low Stock"),
            new Item(0, "Laundry Detergent 500g (Demo)", 2, "Low Stock"),
            new Item(0, "Paper Cups 50 Pack (Demo)", 0, "Out of Stock"),
            new Item(0, "Kitchen Towels 2 Pack (Demo)", 0, "Out of Stock")
        );
        var repository = new InventoryRepository();
        var existing = new HashSet<String>();
        for (Item item : repository.list()) existing.add(item.name());
        var inserted = new java.util.HashMap<Long, Item>();
        for (Item item : samples) {
            // shortcut: reruns identify demos by name; use stable seed IDs if concurrent seeding is needed.
            if (existing.add(item.name())) inserted.put(repository.add(item), item);
        }
        var saved = repository.list();
        for (var entry : inserted.entrySet()) {
            assert saved.stream().anyMatch(item -> item.id() == entry.getKey()
                    && item.name().equals(entry.getValue().name())
                    && item.quantity() == entry.getValue().quantity()
                    && item.status().equals(entry.getValue().status())) : "Inserted sample was not persisted";
        }
        for (Item sample : samples) assert saved.stream().anyMatch(item -> item.name().equals(sample.name())) : "Missing sample";
        System.out.printf("Verified %d demo products; inserted %d, skipped %d existing names.%n", samples.size(), inserted.size(), samples.size()-inserted.size());
        System.out.printf("Inventory now contains %d products and %d units.%n", saved.size(), saved.stream().mapToLong(Item::quantity).sum());
    }
}
