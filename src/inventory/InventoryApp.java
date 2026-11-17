package inventory;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.concurrent.Callable;

/** Desktop inventory editor with background database operations. @author OEL Team */
public final class InventoryApp {
    private final JFrame frame = new JFrame("Stockroom | Inventory Management");
    private final DefaultTableModel model = new DefaultTableModel(new String[]{"ID", "Item name", "Quantity", "Stock status"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JTextField name = new JTextField(24);
    private final JTextField quantity = new JTextField("0", 10);
    private final JComboBox<String> status = new JComboBox<>(Item.STATUSES.toArray(String[]::new));
    private final JLabel message = new JLabel("Connecting to Supabase...");
    private final JButton add = new JButton("Add item"), update = new JButton("Save changes"), delete = new JButton("Delete item"), refresh = new JButton("Refresh"), clear = new JButton("Clear selection");
    private final InventoryRepository repository;
    private boolean busy;

    private InventoryApp(InventoryRepository repository) {
        this.repository = repository;
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        var root = new JPanel(new BorderLayout(16, 16));
        root.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        var heading = new JPanel(new GridLayout(2, 1, 0, 6));
        var title = new JLabel("Stockroom"); title.setFont(title.getFont().deriveFont(Font.BOLD, 28f));
        heading.add(title); heading.add(new JLabel("Store inventory  /  Supabase PostgreSQL")); root.add(heading, BorderLayout.NORTH);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); table.setRowHeight(30); table.setAutoCreateRowSorter(true);
        table.getTableHeader().setReorderingAllowed(false);
        root.add(new JScrollPane(table), BorderLayout.CENTER);
        var editor = new JPanel(new GridLayout(0, 1, 0, 8));
        label(editor, "Item name", name); label(editor, "Quantity (whole number)", quantity); label(editor, "Stock status", status);
        for (var button : List.of(add, update, delete, clear, refresh)) editor.add(button);
        root.add(editor, BorderLayout.EAST); root.add(message, BorderLayout.SOUTH);
        table.getSelectionModel().addListSelectionListener(event -> {
            if (event.getValueIsAdjusting()) return;
            int row = selectedRow();
            if (row >= 0) { name.setText(model.getValueAt(row, 1).toString()); quantity.setText(model.getValueAt(row, 2).toString()); status.setSelectedItem(model.getValueAt(row, 3)); }
            controls();
        });
        add.addActionListener(event -> save(false)); update.addActionListener(event -> save(true));
        delete.addActionListener(event -> {
            int row = selectedRow(); if (row < 0) return;
            long id = (Long) model.getValueAt(row, 0);
            if (JOptionPane.showConfirmDialog(frame, "Delete the selected item?", "Confirm deletion", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
                work(() -> repository.delete(id) ? "Item deleted." : "Item was already deleted.");
        });
        refresh.addActionListener(event -> work(() -> "Inventory refreshed."));
        clear.addActionListener(event -> reset());
        frame.setContentPane(root); frame.setSize(1000, 600); frame.setMinimumSize(new Dimension(800, 480)); frame.setLocationRelativeTo(null); frame.setVisible(true);
        work(() -> "Inventory loaded.");
    }
    private static void label(JPanel panel, String text, JComponent field) {
        var label = new JLabel(text); label.setLabelFor(field); panel.add(label); panel.add(field);
    }
    private int selectedRow() { return table.getSelectedRow() < 0 ? -1 : table.convertRowIndexToModel(table.getSelectedRow()); }
    private void reset() { table.clearSelection(); name.setText(""); quantity.setText("0"); status.setSelectedIndex(0); controls(); }
    private void controls() {
        add.setEnabled(!busy); refresh.setEnabled(!busy); clear.setEnabled(!busy);
        update.setEnabled(!busy && selectedRow() >= 0); delete.setEnabled(!busy && selectedRow() >= 0);
        table.setEnabled(!busy); name.setEnabled(!busy); quantity.setEnabled(!busy); status.setEnabled(!busy);
    }
    private void save(boolean editing) {
        int row = selectedRow(); if (editing && row < 0) return;
        try {
            Item item = Item.parse(editing ? (Long) model.getValueAt(row, 0) : 0, name.getText(), quantity.getText(), (String) status.getSelectedItem());
            work(() -> {
                if (editing) return repository.update(item) ? "Changes saved." : "Item no longer exists. Refresh and select another item.";
                repository.add(item); return "Item added.";
            });
        } catch (IllegalArgumentException e) { JOptionPane.showMessageDialog(frame, e.getMessage(), "Check input", JOptionPane.WARNING_MESSAGE); }
    }
    private void work(Callable<String> operation) {
        busy = true; controls(); message.setText("Working...");
        new SwingWorker<List<Item>, Void>() {
            private String result;
            @Override protected List<Item> doInBackground() throws Exception { result = operation.call(); return repository.list(); }
            @Override protected void done() {
                try {
                    var items = get(); model.setRowCount(0);
                    for (Item item : items) model.addRow(new Object[]{item.id(), item.name(), item.quantity(), item.status()});
                    reset(); message.setText(result + (items.isEmpty() ? " Inventory is empty. Add your first item." : " " + items.size() + " item(s)."));
                } catch (Exception e) {
                    message.setText(result == null ? "Database request failed. Refresh before retrying a write." : result + " Refresh failed; click Refresh.");
                    JOptionPane.showMessageDialog(frame, "Could not load the database. Check your connection and db.properties.\nA write may have completed; refresh before adding it again.", "Database unavailable", JOptionPane.ERROR_MESSAGE);
                } finally { busy = false; controls(); }
            }
        }.execute();
    }
    /** Starts the application on the Swing event thread.
     * @param args unused
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); new InventoryApp(new InventoryRepository()); }
            catch (Exception e) { JOptionPane.showMessageDialog(null, "Cannot start. Copy db.properties.example to db.properties and configure the connection.", "Setup required", JOptionPane.ERROR_MESSAGE); }
        });
    }
}
