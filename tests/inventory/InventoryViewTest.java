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
        var magnetic = new java.util.concurrent.atomic.AtomicReference<WorkspaceVisuals.MagneticButton>();
        var clicks = new java.util.concurrent.atomic.AtomicInteger();
        SwingUtilities.invokeAndWait(() -> {
            var button = new WorkspaceVisuals.MagneticButton("Save", java.awt.Color.BLUE, java.awt.Color.WHITE);
            button.setBounds(10, 20, 180, 48); button.addActionListener(event -> clicks.incrementAndGet()); magnetic.set(button);
            button.dispatchEvent(new java.awt.event.MouseEvent(button, java.awt.event.MouseEvent.MOUSE_MOVED, 0, 0, 175, 44, 0, false));
        });
        Thread.sleep(350);
        SwingUtilities.invokeAndWait(() -> {
            var button = magnetic.get();
            assert button.offsetX > 0 && button.offsetX <= 3 && button.offsetY <= 2;
            assert button.getBounds().equals(new java.awt.Rectangle(10, 20, 180, 48)) : "Magnetism must not move the hit area";
            button.doClick(0); assert clicks.get() == 1;
            button.setEnabled(false); button.doClick(0); assert clicks.get() == 1;
            javax.swing.UIManager.put("Stockroom.motion", false);
        });
        Thread.sleep(350);
        SwingUtilities.invokeAndWait(() -> {
            var button = magnetic.get(); assert Math.abs(button.offsetX) < .03 && Math.abs(button.offsetY) < .03;
            button.setEnabled(true);
            button.dispatchEvent(new java.awt.event.MouseEvent(button, java.awt.event.MouseEvent.MOUSE_MOVED, 0, 0, 175, 44, 0, false));
        });
        Thread.sleep(100);
        SwingUtilities.invokeAndWait(() -> {
            assert magnetic.get().offsetX == 0 && magnetic.get().offsetY == 0 : "Reduced motion must stop attraction";
            magnetic.get().removeNotify(); javax.swing.UIManager.put("Stockroom.motion", true);
            var input = WorkspaceVisuals.input("12"); input.setSize(200, 44);
            var select = WorkspaceVisuals.select(new String[]{"In Stock", "Low Stock"}); select.setSize(200, 44); select.setSelectedIndex(1);
            var canvas = new java.awt.image.BufferedImage(200, 44, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            var graphics = canvas.createGraphics(); input.paint(graphics); select.paint(graphics); graphics.dispose();
            assert input.getText().equals("12") && select.getSelectedItem().equals("Low Stock");
        });
        for (String asset : List.of("warehouse.png", "activity.png")) {
            var image = javax.imageio.ImageIO.read(java.nio.file.Path.of("assets", asset).toFile());
            assert image != null && image.getWidth() > 0 : "Missing artwork: " + asset;
        }
        System.out.println("PASS: summaries, filters, page navigation, charts, artwork, magnetic hit areas, disabled/reduced motion, styled fields");
    }
}
