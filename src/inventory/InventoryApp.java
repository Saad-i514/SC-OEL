package inventory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

/** Inventory workspace with background persistence and accessible form controls.
 * @author OEL Team */
public final class InventoryApp {
    private static final Color NAVY = new Color(31, 23, 49), CANVAS = new Color(244, 240, 249),
            INK = new Color(43, 32, 69), MUTED = new Color(111, 100, 130), LINE = new Color(221, 212, 234),
            ACCENT = new Color(116, 68, 207), RED = new Color(173, 47, 57);
    private final JFrame frame = new JFrame("Stockroom | Inventory Workspace");
    private final DefaultTableModel model = new DefaultTableModel(new String[]{"ID", "Item name", "Quantity", "Stock status"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
        @Override public Class<?> getColumnClass(int column) { return column == 0 ? Long.class : column == 2 ? Integer.class : String.class; }
    };
    private final JTable table = new JTable(model);
    private final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
    private final JTextField name = WorkspaceVisuals.input(""), quantity = WorkspaceVisuals.input("0"), search = WorkspaceVisuals.input("");
    private final JComboBox<String> status = WorkspaceVisuals.select(Item.STATUSES.toArray(String[]::new));
    private final JComboBox<String> filter = WorkspaceVisuals.select(new String[]{"All statuses", "In Stock", "Low Stock", "Out of Stock"});
    private final JLabel message = text("Connecting to your inventory...", 12, MUTED),
            connection = text("CONNECTING", 11, MUTED), count = text("Loading records", 12, MUTED),
            editorTitle = text("New item", 21, INK), editorHint = text("Add a product to your inventory.", 12, MUTED),
            emptyTitle = text("Loading inventory", 20, INK), emptyHint = text("Please wait while records are retrieved.", 13, MUTED);
    private final JLabel[] metrics = {text("—", 30, INK), text("—", 30, INK), text("—", 30, INK), text("—", 30, INK)};
    private final JButton add = button("Add item", ACCENT, Color.WHITE), update = button("Save changes", ACCENT, Color.WHITE),
            delete = button("Delete item", new Color(255, 241, 242), RED), refresh = button("Refresh", Color.WHITE, INK),
            clear = button("New item", NAVY, Color.WHITE), cancel = button("Clear selection", Color.WHITE, MUTED);
    private final CardLayout contentCards = new CardLayout();
    private final JPanel content = new JPanel(contentCards);
    private final WorkspaceVisuals.Pages pages = new WorkspaceVisuals.Pages();
    private final java.util.Map<String, JButton> navigation = new java.util.LinkedHashMap<>();
    private final JLabel pageTitle = text("Overview", 28, INK), pageSubtitle = text("Your store, at a glance.", 13, MUTED);
    private final WorkspaceVisuals.StockChart stockChart = new WorkspaceVisuals.StockChart();
    private final JLabel[] stockCounts = {text("Not loaded", 13, ACCENT), text("Not loaded", 13, MUTED), text("Not loaded", 13, RED)};
    private final JLabel attentionSummary = text("Loading stock health...", 14, INK);
    private final DefaultListModel<Item> attention = new DefaultListModel<>();
    private final JLabel attentionEmpty = text("No stock alerts to show.", 13, MUTED);
    private final JPanel attentionContent = new JPanel(new CardLayout());
    private final DefaultTableModel activityModel = new DefaultTableModel(new String[]{"Time", "Event"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private String currentPage = "Overview";
    private final InventoryRepository repository;
    private boolean busy;

    private InventoryApp(InventoryRepository repository) {
        this.repository = repository;
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        var root = new JPanel(new BorderLayout()); root.add(sidebar(), BorderLayout.WEST);
        var workspace = new JPanel(new BorderLayout(0, 20)); workspace.setBackground(CANVAS);
        workspace.setBorder(new EmptyBorder(24, 26, 16, 26));
        var heading = new JPanel(new BorderLayout()); heading.setOpaque(false);
        var titles = vertical(); titles.add(text("STOCKROOM  /  WORKSPACE", 10, MUTED)); titles.add(Box.createVerticalStrut(7));
        titles.add(pageTitle); titles.add(Box.createVerticalStrut(5)); titles.add(pageSubtitle); heading.add(titles, BorderLayout.CENTER);
        var actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 15)); actions.setOpaque(false); actions.add(refresh); actions.add(clear); heading.add(actions, BorderLayout.EAST);
        workspace.add(heading, BorderLayout.NORTH);
        pages.add(overview(), "Overview");
        var inventory = new JPanel(new BorderLayout(18, 0)); inventory.setOpaque(false); inventory.add(inventoryPanel(), BorderLayout.CENTER); inventory.add(editor(), BorderLayout.EAST);
        pages.add(inventory, "Inventory"); pages.add(activityPage(), "Activity"); workspace.add(pages, BorderLayout.CENTER);
        var footer = new JPanel(new BorderLayout(12, 0)); footer.setOpaque(false); footer.add(message, BorderLayout.CENTER); footer.add(connection, BorderLayout.EAST);
        workspace.add(footer, BorderLayout.SOUTH); root.add(workspace, BorderLayout.CENTER);
        wireActions(); WorkspaceVisuals.styleScrollbars(root); frame.setContentPane(root);
        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        frame.setSize(Math.min(1360, screen.width - 32), Math.min(900, screen.height - 32));
        frame.setMinimumSize(new Dimension(Math.min(1080, screen.width), Math.min(640, screen.height)));
        frame.setLocationRelativeTo(null); frame.setVisible(true); showPage("Overview"); work(() -> "Inventory loaded.");
    }
    private JPanel sidebar() {
        var panel = new JPanel(new BorderLayout()); panel.setBackground(NAVY); panel.setPreferredSize(new Dimension(208, 0)); panel.setBorder(new EmptyBorder(32, 20, 24, 20));
        var top = vertical(); top.add(text("ST / Stockroom", 22, Color.WHITE)); top.add(Box.createVerticalStrut(7)); top.add(text("A PLACE FOR EVERY PRODUCT", 9, new Color(156, 174, 197)));
        top.add(Box.createVerticalStrut(46)); top.add(text("WORKSPACE", 10, new Color(156, 174, 197))); top.add(Box.createVerticalStrut(15));
        String[] labels = {"01   Overview", "02   Inventory", "03   Activity"}; String[] names = {"Overview", "Inventory", "Activity"};
        for (int i=0; i<names.length; i++) {
            String page = names[i]; var nav = button(labels[i], NAVY, new Color(191,177,209)); nav.setHorizontalAlignment(SwingConstants.LEFT);
            nav.setAlignmentX(Component.LEFT_ALIGNMENT); nav.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46)); nav.addActionListener(event -> showPage(page));
            navigation.put(page, nav); top.add(nav); top.add(Box.createVerticalStrut(9));
        }
        panel.add(top, BorderLayout.NORTH);
        var bottom = vertical();
        var motion = new JCheckBox("Animations", true); motion.setOpaque(false); motion.setForeground(new Color(191,177,209)); motion.setFont(new Font("Segoe UI",Font.PLAIN,11));
        motion.setAlignmentX(Component.LEFT_ALIGNMENT); motion.addActionListener(event -> { pages.setAnimated(motion.isSelected()); WorkspaceVisuals.setMotion(motion.isSelected(), frame.getContentPane()); });
        bottom.add(motion); bottom.add(Box.createVerticalStrut(20)); bottom.add(text("STORE OPERATIONS", 10, new Color(166,148,190))); bottom.add(Box.createVerticalStrut(7)); bottom.add(text("Your daily control center",12,Color.WHITE));
        panel.add(bottom,BorderLayout.SOUTH); return panel;
    }
    private void showPage(String page) {
        currentPage=page; pages.showPage(page); pageTitle.setText(page.equals("Inventory") ? "Product inventory" : page.equals("Activity") ? "Workspace activity" : "Overview");
        pageSubtitle.setText(page.equals("Inventory") ? "Organize your catalog. Keep every quantity accurate." : page.equals("Activity") ? "A clear record of actions in this session." : "Your store, at a glance. Make room for a better day.");
        navigation.forEach((key,nav) -> { nav.setBackground(key.equals(page) ? new Color(73,47,110) : NAVY); nav.setForeground(key.equals(page) ? new Color(230,204,255) : new Color(191,177,209)); });
        controls();
    }
    private JPanel overview() {
        var page = vertical();
        var hero = WorkspaceVisuals.rounded(new Color(236,224,250),22); hero.setLayout(new BorderLayout(10,0)); hero.setBorder(new EmptyBorder(22,26,22,16)); hero.setMaximumSize(new Dimension(Integer.MAX_VALUE,210));
        var copy = vertical(); copy.add(text("LESS GUESSWORK. MORE CLARITY.",10,ACCENT)); copy.add(Box.createVerticalStrut(10));
        copy.add(text("Good stock. Great days.",29,INK)); copy.add(Box.createVerticalStrut(8)); copy.add(text("Keep your shelves ready for what comes next.",13,MUTED)); copy.add(Box.createVerticalStrut(17));
        var manage=button("Explore inventory  →",ACCENT,Color.WHITE); manage.addActionListener(event->showPage("Inventory")); copy.add(manage); hero.add(copy,BorderLayout.CENTER); hero.add(new WorkspaceVisuals.Artwork("warehouse.png"),BorderLayout.EAST);
        page.add(hero); page.add(Box.createVerticalStrut(16));
        var summaries=new JPanel(new GridLayout(1,4,12,0)); summaries.setOpaque(false); summaries.setMaximumSize(new Dimension(Integer.MAX_VALUE,128));
        String[] labels={"TOTAL PRODUCTS","UNITS ON HAND","LOW STOCK","OUT OF STOCK"};
        Color[] tileColors={new Color(231,218,252),new Color(222,231,255),new Color(255,232,212),new Color(251,222,235)};
        String[] symbols={"CATALOG", "VOLUME", "ATTENTION", "AVAILABILITY"};
        for(int i=0;i<metrics.length;i++) {
            var tile=WorkspaceVisuals.rounded(tileColors[i],24); tile.setBorder(new EmptyBorder(16,18,16,18)); tile.setLayout(new BorderLayout(0,7));
            tile.add(text(labels[i],11,INK),BorderLayout.NORTH); metrics[i].setFont(metrics[i].getFont().deriveFont(Font.BOLD,32f)); tile.add(metrics[i],BorderLayout.CENTER);
            tile.add(text(symbols[i],9,MUTED),BorderLayout.SOUTH); summaries.add(tile);
        }
        page.add(summaries); page.add(Box.createVerticalStrut(16));
        var lower=new JPanel(new GridLayout(1,2,16,0)); lower.setOpaque(false); lower.setMaximumSize(new Dimension(Integer.MAX_VALUE,300)); lower.setPreferredSize(new Dimension(800,300));
        var health=card(); health.setLayout(new BorderLayout(12,8)); health.add(text("Stock balance",18,INK),BorderLayout.NORTH); health.add(stockChart,BorderLayout.WEST);
        var legend=vertical(); legend.add(Box.createVerticalStrut(12)); for(int i=0;i<3;i++) { legend.add(text(Item.STATUSES.get(i),12,MUTED)); legend.add(Box.createVerticalStrut(5)); legend.add(stockCounts[i]); legend.add(Box.createVerticalStrut(12)); } health.add(legend,BorderLayout.CENTER); lower.add(health);
        var needs=card(); needs.setLayout(new BorderLayout(0,10)); needs.add(attentionSummary,BorderLayout.NORTH);
        var list=new JList<>(attention); list.setCellRenderer(new WorkspaceVisuals.AttentionRow()); list.setFixedCellHeight(56); list.setVisibleRowCount(3); list.setFocusable(false);
        list.setBackground(Color.WHITE); list.getAccessibleContext().setAccessibleName("Products needing stock attention");
        var attentionScroll=new JScrollPane(list); attentionScroll.setBorder(null); attentionScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        attentionScroll.setPreferredSize(new Dimension(0,168)); attentionScroll.getVerticalScrollBar().setUnitIncrement(56);
        var rows=new JPanel(new BorderLayout()); rows.setOpaque(false); rows.add(attentionScroll,BorderLayout.NORTH);
        attentionContent.setOpaque(false); attentionContent.add(rows,"rows"); attentionContent.add(attentionEmpty,"empty"); needs.add(attentionContent,BorderLayout.CENTER);
        var review=button("Review low-stock items  →",new Color(245,237,220),new Color(128,85,17)); review.addActionListener(event->{ search.setText(""); filter.setSelectedItem("Low Stock"); showPage("Inventory"); }); needs.add(review,BorderLayout.SOUTH); lower.add(needs); page.add(lower);
        var scroll=new JScrollPane(page); scroll.setBorder(null); scroll.getViewport().setBackground(CANVAS); scroll.getVerticalScrollBar().setUnitIncrement(20);
        var wrapper=new JPanel(new BorderLayout()); wrapper.setOpaque(false); wrapper.add(scroll); return wrapper;
    }
    private JPanel activityPage() {
        var page=new JPanel(new BorderLayout(0,18)); page.setOpaque(false);
        var banner=WorkspaceVisuals.rounded(new Color(236,224,250),18); banner.setLayout(new BorderLayout()); banner.setBorder(new EmptyBorder(14,24,14,20));
        var copy=vertical(); copy.add(text("Every action, accounted for.",24,INK)); copy.add(Box.createVerticalStrut(10)); copy.add(text("Loads, saves and errors from this open workspace.",13,MUTED)); copy.add(Box.createVerticalStrut(6)); copy.add(text("Session-only history · resets when the application closes",11,MUTED));
        banner.add(copy,BorderLayout.CENTER); var art=new WorkspaceVisuals.Artwork("activity.png"); art.setPreferredSize(new Dimension(175,105)); banner.add(art,BorderLayout.EAST); page.add(banner,BorderLayout.NORTH);
        var panel=card(); panel.setLayout(new BorderLayout(0,12)); panel.add(text("Session timeline",18,INK),BorderLayout.NORTH);
        var events=new JTable(activityModel); events.setRowHeight(44); events.setShowGrid(false); events.setFillsViewportHeight(true); events.setBackground(Color.WHITE); events.setFont(new Font("Segoe UI",Font.PLAIN,13)); events.getColumnModel().getColumn(0).setMaxWidth(110);
        events.getColumnModel().getColumn(0).setMinWidth(90);
        events.setDefaultRenderer(Object.class,new WorkspaceVisuals.ActivityCell());
        Runnable sizeEvents=()->WorkspaceVisuals.sizeActivityRows(events);
        activityModel.addTableModelListener(event->SwingUtilities.invokeLater(sizeEvents));
        events.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentResized(java.awt.event.ComponentEvent event) { sizeEvents.run(); }
        });
        events.getTableHeader().setFont(new Font("Segoe UI",Font.BOLD,12)); var scroll=new JScrollPane(events); scroll.setBorder(BorderFactory.createLineBorder(LINE)); panel.add(scroll); page.add(panel,BorderLayout.CENTER); return page;
    }
    private JPanel inventoryPanel() {
        var panel = card(); panel.setLayout(new BorderLayout(0, 16));
        var header = new JPanel(new BorderLayout(0, 16)); header.setOpaque(false);
        var title = new JPanel(new BorderLayout()); title.setOpaque(false); title.add(text("All items", 19, INK), BorderLayout.WEST); title.add(count, BorderLayout.EAST); header.add(title, BorderLayout.NORTH);
        var tools = new JPanel(new BorderLayout(10, 0)); tools.setOpaque(false); search.setToolTipText("Search by item name"); search.getAccessibleContext().setAccessibleName("Search inventory by name");
        var searchGroup = new JPanel(new BorderLayout(0, 6)); searchGroup.setOpaque(false); var searchLabel = text("Search items", 12, MUTED); searchLabel.setLabelFor(search); searchGroup.add(searchLabel, BorderLayout.NORTH); searchGroup.add(search, BorderLayout.CENTER);
        tools.add(searchGroup, BorderLayout.CENTER);
        var filterGroup = new JPanel(new BorderLayout(0, 6)); filterGroup.setOpaque(false); var filterLabel = text("Stock status", 12, MUTED); filterLabel.setLabelFor(filter); filterGroup.add(filterLabel, BorderLayout.NORTH); filterGroup.add(filter, BorderLayout.CENTER); tools.add(filterGroup, BorderLayout.EAST);
        header.add(tools, BorderLayout.CENTER); panel.add(header, BorderLayout.NORTH);
        table.setRowSorter(sorter); table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); table.setRowHeight(48); table.setShowGrid(false); table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(new Color(237, 228, 252)); table.setSelectionForeground(INK); table.setBackground(Color.WHITE); table.setFillsViewportHeight(true);
        table.getTableHeader().setReorderingAllowed(false); table.getTableHeader().setPreferredSize(new Dimension(0, 40)); table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.setDefaultRenderer(Object.class, new ItemCell()); table.setDefaultRenderer(Long.class, new ItemCell()); table.setDefaultRenderer(Integer.class, new ItemCell());
        table.getColumnModel().getColumn(0).setMaxWidth(65); table.getColumnModel().getColumn(2).setPreferredWidth(80); table.getColumnModel().getColumn(3).setPreferredWidth(135);
        table.getColumnModel().getColumn(0).setMinWidth(48);
        table.getColumnModel().getColumn(1).setPreferredWidth(260);
        table.getColumnModel().getColumn(1).setMinWidth(120);
        table.getColumnModel().getColumn(2).setMinWidth(85);
        table.getColumnModel().getColumn(3).setMinWidth(140);
        search.setPreferredSize(new Dimension(180,44)); filter.setPreferredSize(new Dimension(155,44));
        var scroll = new JScrollPane(table); scroll.setBorder(BorderFactory.createLineBorder(LINE)); content.add(scroll, "table");
        var empty = new JPanel(new GridBagLayout()); empty.setBackground(Color.WHITE); var copy = vertical(); copy.add(emptyTitle); copy.add(Box.createVerticalStrut(10)); copy.add(emptyHint); empty.add(copy); content.add(empty, "empty");
        panel.add(content, BorderLayout.CENTER); panel.add(text("Select a row to edit its details. Click a column heading to sort.", 11, MUTED), BorderLayout.SOUTH); return panel;
    }
    private JPanel editor() {
        var panel = card(); panel.setPreferredSize(new Dimension(286, 0)); panel.setLayout(new BorderLayout(0, 14));
        var heading = vertical(); heading.add(text("ITEM DETAILS", 10, MUTED)); heading.add(Box.createVerticalStrut(8));
        heading.add(editorTitle); heading.add(Box.createVerticalStrut(6)); heading.add(editorHint); panel.add(heading, BorderLayout.NORTH);
        var fields = vertical(); field(fields, "Item name", name); field(fields, "Quantity", quantity); field(fields, "Stock status", status);
        fields.add(text("Status is selected manually.", 11, MUTED));
        var scroll = new JScrollPane(fields); scroll.setBorder(null); scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getViewport().setBackground(Color.WHITE); scroll.getVerticalScrollBar().setUnitIncrement(18); panel.add(scroll, BorderLayout.CENTER);
        var actions = vertical();
        for (var action : List.of(add, update)) { action.setAlignmentX(Component.LEFT_ALIGNMENT); action.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44)); actions.add(action); }
        actions.add(Box.createVerticalStrut(8));
        var secondary = new JPanel(new GridLayout(1, 2, 8, 0)); secondary.setOpaque(false); secondary.setAlignmentX(Component.LEFT_ALIGNMENT);
        cancel.setText("Clear"); secondary.add(cancel); secondary.add(delete); secondary.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40)); actions.add(secondary);
        panel.add(actions, BorderLayout.SOUTH); return panel;
    }
    private void wireActions() {
        table.getSelectionModel().addListSelectionListener(event -> {
            if (event.getValueIsAdjusting()) return;
            int row = selectedRow();
            if (row >= 0) { name.setText(model.getValueAt(row, 1).toString()); quantity.setText(model.getValueAt(row, 2).toString()); status.setSelectedItem(model.getValueAt(row, 3)); }
            else { clearFields(); }
            controls();
        });
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent event) { applyFilter(); }
            @Override public void removeUpdate(DocumentEvent event) { applyFilter(); }
            @Override public void changedUpdate(DocumentEvent event) { applyFilter(); }
        });
        filter.addActionListener(event -> applyFilter());
        add.addActionListener(event -> save(false)); update.addActionListener(event -> save(true));
        delete.addActionListener(event -> {
            int row = selectedRow(); if (row < 0) return;
            long id = (Long) model.getValueAt(row, 0);
            if (JOptionPane.showConfirmDialog(frame, "Delete the selected item? This cannot be undone.", "Delete item", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION)
                work(() -> repository.delete(id) ? "Item deleted." : "Item was already deleted.");
        });
        refresh.addActionListener(event -> work(() -> "Inventory refreshed."));
        clear.addActionListener(event -> { showPage("Inventory"); reset(); name.requestFocusInWindow(); }); cancel.addActionListener(event -> reset());
        add.setMnemonic('A'); update.setMnemonic('S'); refresh.setMnemonic('R'); clear.setMnemonic('N');
    }
    private static void field(JPanel panel, String title, JComponent field) {
        var label = text(title, 12, INK); label.setLabelFor(field); panel.add(label); panel.add(Box.createVerticalStrut(7));
        field.setAlignmentX(Component.LEFT_ALIGNMENT); field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44)); field.setPreferredSize(new Dimension(220, 44)); panel.add(field); panel.add(Box.createVerticalStrut(12));
    }
    private static JPanel vertical() { var panel = new JPanel(); panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS)); panel.setOpaque(false); return panel; }
    private static JPanel card() { var panel=WorkspaceVisuals.rounded(Color.WHITE,24); panel.setBorder(new EmptyBorder(18,18,18,18)); return panel; }
    private static JLabel text(String value, int size, Color color) { var label = new JLabel(value); label.setFont(new Font("Segoe UI", Font.PLAIN, size)); label.setForeground(color); label.setAlignmentX(Component.LEFT_ALIGNMENT); return label; }
    private static JButton button(String title, Color background, Color foreground) { return WorkspaceVisuals.button(title, background, foreground); }
    private static final class ItemCell extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;
        @Override public Component getTableCellRendererComponent(JTable grid, Object value, boolean selected, boolean focus, int row, int column) {
            super.getTableCellRendererComponent(grid, value, selected, focus, row, column);
            setToolTipText(String.valueOf(value));
            setBorder(new EmptyBorder(0, 12, 0, 12)); setFont(new Font("Segoe UI", column == 1 ? Font.BOLD : Font.PLAIN, 12));
            setBackground(selected ? grid.getSelectionBackground() : row % 2 == 0 ? Color.WHITE : new Color(251, 249, 254)); setForeground(INK);
            setHorizontalAlignment(column == 2 ? SwingConstants.RIGHT : SwingConstants.LEFT);
            if (column == 3) {
                setForeground("In Stock".equals(value) ? ACCENT : "Low Stock".equals(value) ? new Color(143, 91, 7) : RED);
                setText("●  " + value);
            }
            return this;
        }
    }
    static long[] summarize(List<Item> items) {
        long units = 0, low = 0, out = 0;
        for (Item item : items) { units += item.quantity(); if (item.status().equals("Low Stock")) low++; if (item.status().equals("Out of Stock")) out++; }
        return new long[]{items.size(), units, low, out};
    }
    static RowFilter<DefaultTableModel, Integer> inventoryFilter(String query, String stock) {
        var byName = RowFilter.<DefaultTableModel, Integer>regexFilter("(?iu)" + Pattern.quote(query.strip()), 1);
        if (stock.equals("All statuses")) return byName;
        return RowFilter.andFilter(List.of(byName, RowFilter.<DefaultTableModel, Integer>regexFilter("^" + Pattern.quote(stock) + "$", 3)));
    }
    private void applyFilter() {
        sorter.setRowFilter(inventoryFilter(search.getText(), (String) filter.getSelectedItem()));
        count.setText(table.getRowCount() + " of " + model.getRowCount() + " items");
        boolean empty = table.getRowCount() == 0;
        emptyTitle.setText(model.getRowCount() == 0 ? "Your inventory starts here" : "No matching items");
        emptyHint.setText(model.getRowCount() == 0 ? "Use the item editor to add your first product." : "Try a different name or stock status.");
        contentCards.show(content, empty ? "empty" : "table");
    }
    private int selectedRow() { return table.getSelectedRow() < 0 ? -1 : table.convertRowIndexToModel(table.getSelectedRow()); }
    private void clearFields() { name.setText(""); quantity.setText("0"); status.setSelectedIndex(0); }
    private void reset() { table.clearSelection(); clearFields(); controls(); }
    private void controls() {
        boolean editing = selectedRow() >= 0;
        add.setVisible(!editing); update.setVisible(editing); add.setEnabled(!busy); update.setEnabled(!busy && editing);
        refresh.setEnabled(!busy); clear.setEnabled(!busy); cancel.setEnabled(!busy); delete.setEnabled(!busy && editing);
        table.setEnabled(!busy); name.setEnabled(!busy); quantity.setEnabled(!busy); status.setEnabled(!busy); search.setEnabled(!busy); filter.setEnabled(!busy);
        editorTitle.setText(editing ? "Edit item" : "New item"); editorHint.setText(editing ? "Update the selected product." : "Add a product to your inventory.");
        frame.getRootPane().setDefaultButton(currentPage.equals("Inventory") ? (editing ? update : add) : null);
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
    private void recordActivity(String event) {
        activityModel.insertRow(0,new Object[]{LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")),event});
    }
    private void work(Callable<String> operation) {
        busy = true; controls(); message.setForeground(MUTED); message.setText("Syncing inventory..."); connection.setText("SYNCING"); connection.setForeground(MUTED);
        new SwingWorker<List<Item>, Void>() {
            private String result;
            @Override protected List<Item> doInBackground() throws Exception { result = operation.call(); return repository.list(); }
            @Override protected void done() {
                try {
                    var items = get(); model.setRowCount(0);
                    for (Item item : items) model.addRow(new Object[]{item.id(), item.name(), item.quantity(), item.status()});
                    long[] totals = summarize(items); for (int i = 0; i < totals.length; i++) metrics[i].setText(String.format("%,d", totals[i]));
                    stockChart.setValues(totals[0]-totals[2]-totals[3],totals[2],totals[3]);
                    long[] stock={totals[0]-totals[2]-totals[3],totals[2],totals[3]}; for(int i=0;i<3;i++) stockCounts[i].setText(stock[i]+" products");
                    attention.clear(); for(Item item:items) if(!item.status().equals("In Stock")) attention.addElement(item);
                    attentionSummary.setText(attention.isEmpty()?"No items marked for attention":"Items needing attention");
                    attentionEmpty.setText(items.isEmpty()?"Add products to start tracking stock.":"All products are marked In Stock.");
                    ((CardLayout)attentionContent.getLayout()).show(attentionContent,attention.isEmpty()?"empty":"rows");
                    recordActivity(result);
                    reset(); applyFilter(); message.setText(result); connection.setText("SYNCED  " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))); connection.setForeground(ACCENT);
                } catch (Exception e) {
                    message.setForeground(RED); message.setText(result == null ? "Request failed. Refresh before retrying a write." : result + " Refresh failed; click Refresh.");
                    recordActivity(result == null ? "Request failed. Outcome may be unknown; refresh before retrying." : result + " Refresh failed.");
                    connection.setText("CONNECTION ERROR"); connection.setForeground(RED);
                    if (model.getRowCount() == 0) { emptyTitle.setText("Unable to load inventory"); emptyHint.setText("Check your connection, then select Refresh."); contentCards.show(content, "empty"); count.setText("Not available"); }
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
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
                var font = new javax.swing.plaf.FontUIResource("Segoe UI", Font.PLAIN, 13);
                for (Object key : java.util.Collections.list(UIManager.getDefaults().keys())) if (UIManager.get(key) instanceof javax.swing.plaf.FontUIResource) UIManager.put(key, font);
                UIManager.put("Button.select", new Color(222, 203, 247)); UIManager.put("Button.disabledText", MUTED);
                UIManager.put("TextField.border", BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(LINE), new EmptyBorder(8, 10, 8, 10)));
                UIManager.put("ComboBox.background", new Color(249,247,253)); UIManager.put("TableHeader.background", CANVAS);
                new InventoryApp(new InventoryRepository());
            } catch (Exception e) { JOptionPane.showMessageDialog(null, "Cannot start. Copy db.properties.example to db.properties and configure the connection.", "Setup required", JOptionPane.ERROR_MESSAGE); }
        });
    }
}
