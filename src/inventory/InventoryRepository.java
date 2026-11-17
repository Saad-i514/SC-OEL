package inventory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/** Prepared-statement persistence for Supabase PostgreSQL. @author OEL Team */
public final class InventoryRepository {
    private final Properties config = new Properties();
    /** Loads untracked local connection settings.
     * @throws IOException if settings cannot be loaded
     */
    public InventoryRepository() throws IOException {
        try (var input = Files.newInputStream(Path.of("db.properties"))) { config.load(input); }
        for (String key : List.of("url", "user", "password")) {
            if (config.getProperty(key, "").isBlank()) throw new IOException("Missing database setting: " + key);
        }
    }
    private Connection connect() throws SQLException {
        return DriverManager.getConnection(config.getProperty("url"), config.getProperty("user"), config.getProperty("password"));
    }
    /** Fetches a stable name-ordered snapshot.
     * @return inventory rows, possibly empty
     * @throws SQLException on database failure
     */
    public List<Item> list() throws SQLException {
        var items = new ArrayList<Item>();
        try (var connection = connect(); var statement = connection.prepareStatement("select id,name,quantity,status from public.inventory_items order by lower(name),id"); var rows = statement.executeQuery()) {
            while (rows.next()) items.add(new Item(rows.getLong(1), rows.getString(2), rows.getInt(3), rows.getString(4)));
        }
        return items;
    }
    /** Adds an item.
     * @param item validated input
     * @return generated identity
     * @throws SQLException on database failure
     */
    public long add(Item item) throws SQLException {
        try (var connection = connect(); var statement = connection.prepareStatement("insert into public.inventory_items(name,quantity,status) values (?,?,?) returning id")) {
            bind(statement, item);
            try (var row = statement.executeQuery()) { row.next(); return row.getLong(1); }
        }
    }
    /** Updates an existing row.
     * @param item validated input with identity
     * @return whether the row still existed
     * @throws SQLException on database failure
     */
    public boolean update(Item item) throws SQLException {
        try (var connection = connect(); var statement = connection.prepareStatement("update public.inventory_items set name=?,quantity=?,status=? where id=?")) {
            bind(statement, item); statement.setLong(4, item.id());
            return statement.executeUpdate() == 1;
        }
    }
    /** Deletes a selected row.
     * @param id database identity
     * @return whether a row was removed
     * @throws SQLException on database failure
     */
    public boolean delete(long id) throws SQLException {
        try (var connection = connect(); var statement = connection.prepareStatement("delete from public.inventory_items where id=?")) {
            statement.setLong(1, id); return statement.executeUpdate() == 1;
        }
    }
    private static void bind(PreparedStatement statement, Item item) throws SQLException {
        statement.setString(1, item.name()); statement.setInt(2, item.quantity()); statement.setString(3, item.status());
    }
}
