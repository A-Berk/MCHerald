package MCHerald.gui;

import MCHerald.MCHerald;
import MCHerald.util.Constants;
import MCHerald.util.GUI;
import MCHerald.util.Language;
import MCHerald.util.ServerInfo;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class ServerTable extends JFrame implements GUI {

    private MCHerald herald;
    private ServerListTable table;
    private JButton deleteButton;

    public ServerTable(MCHerald herald){
        super();

        // UI Settings
        try {
            UIManager.setLookAndFeel(Language.UI.LOOK_FEEL);
        } catch (ReflectiveOperationException | UnsupportedLookAndFeelException ignore) {
            System.out.println(Language.UI.LOOK_FEEL_ERROR);
        }
        UIManager.put("swing.boldMetal", Boolean.FALSE);
        UIManager.put("Button.focus", new Color(0, 0, 0, 0));

        this.herald = herald;
        this.setIconImage(herald.getAppIcon());

        // Button Components
        JPanel buttons = new JPanel(new BorderLayout());
        JPanel westButtons = new JPanel();
        JPanel eastButtons = new JPanel();

        JButton refreshButton = new JButton(Language.TABLE.REFRESH);
        JButton addButton = new JButton(Language.TABLE.ADD);
        deleteButton = new JButton(Language.TABLE.DELETE);
        JButton aboutButton = new JButton(Language.TABLE.ABOUT);

        westButtons.add(refreshButton);
        westButtons.add(addButton);
        westButtons.add(deleteButton);
        eastButtons.add(aboutButton);

        deleteButton.setEnabled(false);

        buttons.add(westButtons, BorderLayout.WEST);
        buttons.add(eastButtons, BorderLayout.EAST);

        // Table
        JPanel serverTable = new JPanel();
        table = new ServerListTable();
        serverTable.add(table);

        // Button Listeners
        refreshButton.addActionListener(e -> {
            herald.refreshServerTable();
            deleteButton.setEnabled(false);
        });

        deleteButton.addActionListener(e -> {
            if(table.getSelectedRow()[Constants.COLUMNS.NAME] != null)
                herald.deleteServer(table.getSelectedRow()[Constants.COLUMNS.NAME].toString());
        });

        addButton.addActionListener(e ->herald.openAddServerMenu());
        aboutButton.addActionListener(e -> herald.openAbout());

        // Main Panel
        JPanel serverWindow = new JPanel(new BorderLayout());
        serverWindow.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        serverWindow.add(serverTable, BorderLayout.NORTH);
        serverWindow.add(buttons, BorderLayout.SOUTH);

        // Frame Settings
        this.setTitle(Language.TABLE.TITLE);
        this.setContentPane(serverWindow);
        this.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        this.setResizable(false);
        this.setPreferredSize(new Dimension(650, 350));
        this.pack();
        this.setLocationRelativeTo(null);
    }

    /* Public Methods */

    public void update(){
        SwingUtilities.invokeLater(() -> table.update());
    }

    /* Contract Methods */

    @Override
    public void open(){
        SwingUtilities.invokeLater(() -> {
            table.update();
            deleteButton.setEnabled(false);
            this.setVisible(true);
        });
    }

    @Override
    public void close(){
        SwingUtilities.invokeLater(() -> this.setVisible(false));
    }

    @Override
    public void shutdown(){
        SwingUtilities.invokeLater(() -> {
            this.setVisible(false);
            this.dispose();
        });
    }

    private class ServerListTable extends JScrollPane {
        private JTable table;
        private DefaultTableModel model;
        private Object[] selectedRow;

        ServerListTable(){
            this(new Object[0][0]);
        }

        ServerListTable(Object[][] data) {
            model = new DefaultTableModel(data, Language.TABLE.COLUMN_NAMES){
                @Override
                public boolean isCellEditable(int row, int col){
                    return col != Constants.COLUMNS.ONLINE_OUT_OF_MAX;
                }
            };

            table = new CustomTable(model);
            table.getTableHeader().setReorderingAllowed(false);
            table.getColumnModel().getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            table.addPropertyChangeListener(e -> {
                if (e.getPropertyName().equals("tableCellEditor")){
                    if (!table.isEditing()){
                        // Can't look up by name, it might change. Temp fix, TODO Get edited ServerInfo via what?
                        herald.editServer(
                                ((ServerInfo) herald.getServerList().values().toArray()[table.getSelectedRow()]).getName(),
                                table.getSelectedColumn(),
                                table.getModel().getValueAt(table.getSelectedRow(), table.getSelectedColumn()).toString()
                        );
                    }
                }
            });

            table.setDefaultRenderer(String.class, new DefaultTableCellRenderer(){
                @Override
                public Component getTableCellRendererComponent(JTable table,Object value,boolean isSelected,boolean hasFocus,int row,int column) {
                    Component c = super.getTableCellRendererComponent(table,value,isSelected,hasFocus,row,column);
                    c.setForeground(
                            column == Constants.COLUMNS.IP && table.getModel().getValueAt(row, Constants.COLUMNS.ONLINE_OUT_OF_MAX).toString().equals(Language.TABLE.PLAYER_COUNT_ERROR) ?
                                    Color.RED :
                                    Color.BLACK
                    );
                    return c;
                }
            });

            table.setShowGrid(false);
            table.setIntercellSpacing(new Dimension(0, 0));
            table.getTableHeader().setBorder(new MatteBorder(0, 0, 1, 0, Color.BLACK));

            selectedRow = null;

            table.getSelectionModel().addListSelectionListener(e -> {
                selectedRow = new Object[Language.TABLE.COLUMN_NAMES.length];
                for (int i = 0; i < selectedRow.length; i++) {
                    if(table.getSelectedRow() >= 0)
                        selectedRow[i] = table.getValueAt(table.getSelectedRow(), i);
                }
                if(table.getSelectedRow() > -1 && !table.isEditing()){
                    deleteButton.setEnabled(true);
                } else {
                    deleteButton.setEnabled(false);
                }
            });

            this.setPreferredSize(new Dimension(600, 250));
            resizeTable(table);
            table.setPreferredScrollableViewportSize(table.getPreferredSize());
            this.setViewportView(table);
            this.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        }

        Object[] getSelectedRow() {
            return selectedRow;
        }

        void update() {
            Object[][] tableData = new Object[herald.getServerList().size()][Language.TABLE.COLUMN_NAMES.length];
            int i = 0;
            for(ServerInfo server : herald.getServerList().values())
                tableData[i++] = server.getServerData();

            model.setDataVector(tableData, Language.TABLE.COLUMN_NAMES);
            resizeTable(table);
        }

        private void resizeTable(JTable table) {
            SwingUtilities.invokeLater(()->{
                table.getColumnModel().getColumn(0).setPreferredWidth(15);
                table.getColumnModel().getColumn(1).setPreferredWidth(100);
                table.getColumnModel().getColumn(2).setPreferredWidth(100);
                table.getColumnModel().getColumn(3).setPreferredWidth(50);
                table.getColumnModel().getColumn(4).setPreferredWidth(15);
            });
        }
    }

    private static class CustomTable extends JTable {
        private Border cellBorder;

        CustomTable(DefaultTableModel model){
            super(model);
            cellBorder = BorderFactory.createCompoundBorder();
            cellBorder = BorderFactory.createCompoundBorder(cellBorder, BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY));
            cellBorder = BorderFactory.createCompoundBorder(cellBorder, BorderFactory.createEmptyBorder(0, 10, 0, 0));
        }

        @Override
        public Class getColumnClass(int column) {
            return (column == 0) ? Boolean.class : String.class;
        }

        @Override
        public Component prepareRenderer(TableCellRenderer renderer, int row, int column){
            Component cell = super.prepareRenderer(renderer, row, column);
            ((JComponent) cell).setBorder((row == this.getRowCount() - 1) ? BorderFactory.createEmptyBorder(0, 10, 0, 0) : cellBorder);
            return cell;
        }

        @Override
        public Component prepareEditor(TableCellEditor editor, int row, int column) {
            Component c = super.prepareEditor(editor, row, column);
            c.setBackground(javax.swing.UIManager.getColor("Table.selectionBackground"));
            return c;
        }
    }
}
