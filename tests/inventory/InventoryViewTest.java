package inventory;

import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.util.Arrays;
import java.util.List;

/** Checks summary arithmetic, literal search, filtering and sorted identities.
 * @author OEL Team */
public final class InventoryViewTest {
    private InventoryViewTest() { }
    /** Runs view-logic checks on the Swing event thread.
     * @param args unused
     * @throws Exception when a check fails
     */
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            assert Arrays.equals(InventoryApp.summarize(List.of()), new long[]{0, 0, 0, 0});
            var items = List.of(new Item(1, "Tea [green]", Integer.MAX_VALUE, "In Stock"),
                    new Item(2, "Coffee", Integer.MAX_VALUE, "Low Stock"), new Item(3, "Tea black", 0, "Out of Stock"));
            assert Arrays.equals(InventoryApp.summarize(items), new long[]{3, 4294967294L, 1, 1});
            var model = new DefaultTableModel(new String[]{"ID", "Name", "Quantity", "Status"}, 0);
            for (Item item : items) model.addRow(new Object[]{item.id(), item.name(), item.quantity(), item.status()});
            var sorter = new TableRowSorter<>(model);
            sorter.setRowFilter(InventoryApp.inventoryFilter(" [GREEN] ", "All statuses"));
            assert sorter.getViewRowCount() == 1;
            assert model.getValueAt(sorter.convertRowIndexToModel(0), 0).equals(1L);
            sorter.setRowFilter(InventoryApp.inventoryFilter("tea", "Out of Stock"));
            assert sorter.getViewRowCount() == 1;
            assert model.getValueAt(sorter.convertRowIndexToModel(0), 0).equals(3L);
            sorter.setRowFilter(InventoryApp.inventoryFilter(".*", "All statuses"));
            assert sorter.getViewRowCount() == 0 : "Search must treat regex punctuation literally";
            sorter.setRowFilter(InventoryApp.inventoryFilter("", "All statuses"));
            sorter.toggleSortOrder(1);
            assert sorter.getViewRowCount() == 3;
            assert model.getValueAt(sorter.convertRowIndexToModel(0), 0).equals(2L);
            var pages = new WorkspaceVisuals.Pages();
            var overview = new javax.swing.JPanel();
            var inventory = new javax.swing.JPanel();
            pages.add(overview, "Overview"); pages.add(inventory, "Inventory");
            pages.setAnimated(false); pages.showPage("Inventory");
            assert inventory.isVisible() && !overview.isVisible();
            pages.showPage("Overview");
            assert overview.isVisible() && !inventory.isVisible();
            var chart = new WorkspaceVisuals.StockChart(); chart.setSize(180, 160);
            var canvas = new java.awt.image.BufferedImage(180, 160, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            var graphics = canvas.createGraphics();
            chart.paint(graphics); chart.setValues(1, 2, 3); chart.paint(graphics); graphics.dispose();
        });
        for (String asset : List.of("warehouse.png", "activity.png")) {
            var image = javax.imageio.ImageIO.read(java.nio.file.Path.of("assets", asset).toFile());
            assert image != null && image.getWidth() > 0 : "Missing artwork: " + asset;
        }
        System.out.println("PASS: summaries, literal search, filters, sorted identities, page navigation, empty/populated charts, bundled artwork");
    }
}
