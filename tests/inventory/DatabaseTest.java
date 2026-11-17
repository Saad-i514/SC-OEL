package inventory;

/** Live CRUD checks; removes only its own uniquely named record. @author OEL Team */
public final class DatabaseTest {
    private DatabaseTest() {}
    /** Runs against the configured database. @param args unused @throws Exception on failure */
    public static void main(String[] args) throws Exception {
        var repository = new InventoryRepository();
        long id = repository.add(new Item(0, "OEL-test-" + java.util.UUID.randomUUID(), 2, "In Stock"));
        try {
            assert repository.list().stream().anyMatch(item -> item.id() == id);
            assert repository.update(new Item(id, "OEL-updated-" + id, 0, "Out of Stock"));
            assert repository.list().stream().anyMatch(item -> item.id() == id && item.quantity() == 0 && item.status().equals("Out of Stock"));
            assert repository.delete(id);
            assert !repository.update(new Item(id, "Missing", 1, "Low Stock"));
            assert !repository.delete(id);
            assert repository.list().stream().noneMatch(item -> item.id() == id);
            System.out.println("PASS: live add/list/update/delete and missing-row handling");
        } finally { repository.delete(id); }
    }
}
